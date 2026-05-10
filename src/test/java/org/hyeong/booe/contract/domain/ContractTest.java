package org.hyeong.booe.contract.domain;

import org.hyeong.booe.contract.domain.type.ContractStatus;
import org.hyeong.booe.contract.domain.type.ContractType;
import org.hyeong.booe.contract.dto.req.ContractBaseReqDto;
import org.hyeong.booe.member.domain.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ContractTest {

    private Member lessor;
    private Member lessee;
    private Contract contract;

    @BeforeEach
    void setUp() {
        lessor = member(1L, "booe_lessor");
        lessee = member(2L, "booe_lessee");
        contract = Contract.createContract(lessor, baseDto());
    }

    private Member member(Long id, String code) {
        Member m = Member.builder().memberCode(code).build();
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }

    private ContractBaseReqDto baseDto() {
        ContractBaseReqDto dto = new ContractBaseReqDto();
        ContractBaseReqDto.AddressInfo addr = new ContractBaseReqDto.AddressInfo();
        ReflectionTestUtils.setField(addr, "address", "서울시 강남구 테헤란로 1");
        ReflectionTestUtils.setField(dto, "addressInfo", addr);

        ContractBaseReqDto.PaymentInfo pay = new ContractBaseReqDto.PaymentInfo();
        ReflectionTestUtils.setField(pay, "deposit", 10_000_000L);
        ReflectionTestUtils.setField(pay, "monthlyRent", 500_000L);
        ReflectionTestUtils.setField(dto, "paymentInfo", pay);

        ContractBaseReqDto.LeaseTerm term = new ContractBaseReqDto.LeaseTerm();
        ReflectionTestUtils.setField(term, "moveInDate", LocalDate.of(2026, 6, 1));
        ReflectionTestUtils.setField(term, "leaseEndDate", LocalDate.of(2027, 5, 31));
        ReflectionTestUtils.setField(term, "leaseMonths", 12);
        ReflectionTestUtils.setField(dto, "leaseTerm", term);
        return dto;
    }

    @Nested
    @DisplayName("createContract")
    class Creation {

        @Test
        @DisplayName("초기 상태는 DRAFT, type은 MONTHLY")
        void initial_status() {
            assertThat(contract.getStatus()).isEqualTo(ContractStatus.DRAFT);
            assertThat(contract.getType()).isEqualTo(ContractType.MONTHLY);
            assertThat(contract.getMember()).isEqualTo(lessor);
            assertThat(contract.getAddress()).isEqualTo("서울시 강남구 테헤란로 1");
        }

        @Test
        @DisplayName("PaymentInfo의 deposit/monthlyRent를 받는다")
        void payment_info() {
            assertThat(contract.getTotalDeposit()).isEqualTo(10_000_000L);
            assertThat(contract.getMonthlyRent()).isEqualTo(500_000L);
        }
    }

    @Nested
    @DisplayName("상태 전이 메서드")
    class StatusTransition {

        @Test
        @DisplayName("requestReview() → REVIEW_REQUESTED + lesseeMember 설정")
        void requestReview() {
            contract.requestReview(lessee);

            assertThat(contract.getStatus()).isEqualTo(ContractStatus.REVIEW_REQUESTED);
            assertThat(contract.getLesseeMember()).isEqualTo(lessee);
        }

        @Test
        @DisplayName("submitByLessee() → LESSEE_SUBMITTED")
        void submitByLessee() {
            contract.submitByLessee();
            assertThat(contract.getStatus()).isEqualTo(ContractStatus.LESSEE_SUBMITTED);
        }

        @Test
        @DisplayName("confirmByLessor(name, phone) → PAYMENT_PENDING + 영수자 정보")
        void confirmByLessor() {
            contract.confirmByLessor("홍임대", "01011112222");

            assertThat(contract.getStatus()).isEqualTo(ContractStatus.PAYMENT_PENDING);
            assertThat(contract.getReceiverName()).isEqualTo("홍임대");
            assertThat(contract.getReceiverPhone()).isEqualTo("01011112222");
        }

        @Test
        @DisplayName("completePayment() → PAYMENT_COMPLETED")
        void completePayment() {
            contract.completePayment();
            assertThat(contract.getStatus()).isEqualTo(ContractStatus.PAYMENT_COMPLETED);
        }

        @Test
        @DisplayName("cancelPayment() → PAYMENT_PENDING")
        void cancelPayment() {
            contract.completePayment();
            contract.cancelPayment();
            assertThat(contract.getStatus()).isEqualTo(ContractStatus.PAYMENT_PENDING);
        }

        @Test
        @DisplayName("requestSign(documentId) → SIGN_REQUESTED + modusignDocumentId")
        void requestSign() {
            contract.requestSign("doc-123");

            assertThat(contract.getStatus()).isEqualTo(ContractStatus.SIGN_REQUESTED);
            assertThat(contract.getModusignDocumentId()).isEqualTo("doc-123");
        }

        @Test
        @DisplayName("completeSigning() → SIGNED")
        void completeSigning() {
            contract.completeSigning();
            assertThat(contract.getStatus()).isEqualTo(ContractStatus.SIGNED);
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("주소/금액/기간이 갱신된다")
        void update_fields() {
            ContractBaseReqDto newDto = baseDto();
            ContractBaseReqDto.AddressInfo addr = new ContractBaseReqDto.AddressInfo();
            ReflectionTestUtils.setField(addr, "address", "변경된 주소");
            ReflectionTestUtils.setField(newDto, "addressInfo", addr);
            ContractBaseReqDto.PaymentInfo pay = new ContractBaseReqDto.PaymentInfo();
            ReflectionTestUtils.setField(pay, "deposit", 20_000_000L);
            ReflectionTestUtils.setField(pay, "monthlyRent", 700_000L);
            ReflectionTestUtils.setField(newDto, "paymentInfo", pay);

            contract.update(newDto);

            assertThat(contract.getAddress()).isEqualTo("변경된 주소");
            assertThat(contract.getTotalDeposit()).isEqualTo(20_000_000L);
            assertThat(contract.getMonthlyRent()).isEqualTo(700_000L);
        }
    }

    @Nested
    @DisplayName("삭제 플래그")
    class DeleteFlags {

        @Test
        @DisplayName("deleteByLessor() 후 isDeletedByLessor=true")
        void delete_by_lessor() {
            assertThat(contract.isDeletedByLessor()).isFalse();
            contract.deleteByLessor();
            assertThat(contract.isDeletedByLessor()).isTrue();
        }

        @Test
        @DisplayName("deleteByLessee() 후 isDeletedByLessee=true")
        void delete_by_lessee() {
            assertThat(contract.isDeletedByLessee()).isFalse();
            contract.deleteByLessee();
            assertThat(contract.isDeletedByLessee()).isTrue();
        }
    }

    @Nested
    @DisplayName("isLessor / isLessee")
    class RoleCheck {

        @Test
        @DisplayName("isLessor: 임대인 ID와 일치 시 true")
        void isLessor_true() {
            assertThat(contract.isLessor(1L)).isTrue();
            assertThat(contract.isLessor(99L)).isFalse();
        }

        @Test
        @DisplayName("isLessee: 임차인 미설정이면 false")
        void isLessee_when_no_lessee() {
            assertThat(contract.isLessee(2L)).isFalse();
        }

        @Test
        @DisplayName("isLessee: requestReview 후 임차인 ID와 일치 시 true")
        void isLessee_true_after_request() {
            contract.requestReview(lessee);

            assertThat(contract.isLessee(2L)).isTrue();
            assertThat(contract.isLessee(99L)).isFalse();
        }
    }

    @Nested
    @DisplayName("isPaidOrAfter")
    class PaidOrAfter {

        @Test
        @DisplayName("DRAFT, REVIEW_REQUESTED, PAYMENT_PENDING → false")
        void before_payment() {
            assertThat(contract.isPaidOrAfter()).isFalse();
            contract.requestReview(lessee);
            assertThat(contract.isPaidOrAfter()).isFalse();
            contract.confirmByLessor("a", "01011112222");
            assertThat(contract.isPaidOrAfter()).isFalse();
        }

        @Test
        @DisplayName("PAYMENT_COMPLETED, SIGN_REQUESTED, SIGNED → true")
        void after_payment() {
            contract.completePayment();
            assertThat(contract.isPaidOrAfter()).isTrue();

            contract.requestSign("doc");
            assertThat(contract.isPaidOrAfter()).isTrue();

            contract.completeSigning();
            assertThat(contract.isPaidOrAfter()).isTrue();
        }
    }
}
