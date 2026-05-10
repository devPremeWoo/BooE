package org.hyeong.booe.contract.dto.res;

import org.hyeong.booe.contract.domain.Contract;
import org.hyeong.booe.contract.domain.ContractPaymentSchedule;
import org.hyeong.booe.contract.domain.type.ContractRole;
import org.hyeong.booe.contract.domain.type.PaymentType;
import org.hyeong.booe.contract.dto.req.ContractBaseReqDto;
import org.hyeong.booe.member.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContractListResDtoTest {

    private final Member lessor = member(1L, "booe_lessor");
    private final Member lessee = member(2L, "booe_lessee");

    @Nested
    @DisplayName("myRole 결정")
    class RoleResolution {

        @Test
        @DisplayName("memberId가 임대인이면 LESSOR")
        void lessor_role() {
            Contract contract = contractWithSchedules(List.of());

            ContractListResDto dto = ContractListResDto.of(contract, 1L);

            assertThat(dto.getMyRole()).isEqualTo(ContractRole.LESSOR);
        }

        @Test
        @DisplayName("memberId가 임대인이 아니면 LESSEE로 간주")
        void lessee_role() {
            Contract contract = contractWithSchedules(List.of());
            contract.requestReview(lessee);

            ContractListResDto dto = ContractListResDto.of(contract, 2L);

            assertThat(dto.getMyRole()).isEqualTo(ContractRole.LESSEE);
        }
    }

    @Nested
    @DisplayName("deletedByCounterpart")
    class DeletedByCounterpart {

        @Test
        @DisplayName("내가 LESSOR인데 LESSEE가 삭제했으면 true")
        void lessor_sees_lessee_deleted() {
            Contract contract = contractWithSchedules(List.of());
            contract.requestReview(lessee);
            contract.deleteByLessee();

            ContractListResDto dto = ContractListResDto.of(contract, 1L);

            assertThat(dto.isDeletedByCounterpart()).isTrue();
        }

        @Test
        @DisplayName("내가 LESSEE인데 LESSOR가 삭제했으면 true")
        void lessee_sees_lessor_deleted() {
            Contract contract = contractWithSchedules(List.of());
            contract.requestReview(lessee);
            contract.deleteByLessor();

            ContractListResDto dto = ContractListResDto.of(contract, 2L);

            assertThat(dto.isDeletedByCounterpart()).isTrue();
        }

        @Test
        @DisplayName("내가 삭제한 건 deletedByCounterpart=false")
        void self_delete_not_counted() {
            Contract contract = contractWithSchedules(List.of());
            contract.deleteByLessor();

            ContractListResDto dto = ContractListResDto.of(contract, 1L);

            assertThat(dto.isDeletedByCounterpart()).isFalse();
        }
    }

    @Nested
    @DisplayName("findNextPayment — due_date >= 오늘 중 가장 빠른 것")
    class FindNextPayment {

        @Test
        @DisplayName("과거 + 미래 스케줄이 섞여있으면 가장 빠른 미래 스케줄을 반환")
        void picks_earliest_future() {
            LocalDate today = LocalDate.now();
            ContractPaymentSchedule past1   = schedule(today.minusMonths(2), 500_000L, PaymentType.MONTHLY_RENT, 1);
            ContractPaymentSchedule past2   = schedule(today.minusMonths(1), 500_000L, PaymentType.MONTHLY_RENT, 2);
            ContractPaymentSchedule future1 = schedule(today.plusMonths(1),  500_000L, PaymentType.MONTHLY_RENT, 3);
            ContractPaymentSchedule future2 = schedule(today.plusMonths(2),  500_000L, PaymentType.MONTHLY_RENT, 4);

            Contract contract = contractWithSchedules(List.of(past1, future2, past2, future1));

            ContractListResDto dto = ContractListResDto.of(contract, 1L);

            assertThat(dto.getNextPaymentDueDate()).isEqualTo(today.plusMonths(1));
            assertThat(dto.getNextPaymentAmount()).isEqualTo(500_000L);
            assertThat(dto.getNextPaymentType()).isEqualTo(PaymentType.MONTHLY_RENT);
        }

        @Test
        @DisplayName("오늘 만기 스케줄도 미래로 포함된다")
        void today_is_included() {
            LocalDate today = LocalDate.now();
            ContractPaymentSchedule todaySchedule = schedule(today, 500_000L, PaymentType.MONTHLY_RENT, 1);

            Contract contract = contractWithSchedules(List.of(todaySchedule));

            ContractListResDto dto = ContractListResDto.of(contract, 1L);

            assertThat(dto.getNextPaymentDueDate()).isEqualTo(today);
        }

        @Test
        @DisplayName("모든 스케줄이 과거면 next 정보가 모두 null")
        void all_past_returns_null() {
            LocalDate today = LocalDate.now();
            ContractPaymentSchedule past = schedule(today.minusDays(1), 500_000L, PaymentType.MONTHLY_RENT, 1);

            Contract contract = contractWithSchedules(List.of(past));

            ContractListResDto dto = ContractListResDto.of(contract, 1L);

            assertThat(dto.getNextPaymentDueDate()).isNull();
            assertThat(dto.getNextPaymentAmount()).isNull();
            assertThat(dto.getNextPaymentType()).isNull();
        }

        @Test
        @DisplayName("스케줄이 비어있으면 next 정보가 모두 null")
        void no_schedules_returns_null() {
            Contract contract = contractWithSchedules(List.of());

            ContractListResDto dto = ContractListResDto.of(contract, 1L);

            assertThat(dto.getNextPaymentDueDate()).isNull();
            assertThat(dto.getNextPaymentAmount()).isNull();
            assertThat(dto.getNextPaymentType()).isNull();
        }

        @Test
        @DisplayName("BALANCE가 가장 가까운 미래라면 BALANCE 타입을 반환")
        void balance_can_be_next() {
            LocalDate today = LocalDate.now();
            ContractPaymentSchedule balance = schedule(today.plusDays(3), 10_000_000L, PaymentType.BALANCE, 1);
            ContractPaymentSchedule rent    = schedule(today.plusMonths(1), 500_000L, PaymentType.MONTHLY_RENT, 2);

            Contract contract = contractWithSchedules(List.of(rent, balance));

            ContractListResDto dto = ContractListResDto.of(contract, 1L);

            assertThat(dto.getNextPaymentType()).isEqualTo(PaymentType.BALANCE);
            assertThat(dto.getNextPaymentAmount()).isEqualTo(10_000_000L);
            assertThat(dto.getNextPaymentDueDate()).isEqualTo(today.plusDays(3));
        }
    }

    @Nested
    @DisplayName("기본 필드 매핑")
    class BasicFields {

        @Test
        @DisplayName("contractId, address, status, contractType이 그대로 매핑된다")
        void basic_fields() {
            Contract contract = contractWithSchedules(List.of());

            ContractListResDto dto = ContractListResDto.of(contract, 1L);

            assertThat(dto.getContractId()).isEqualTo(100L);
            assertThat(dto.getAddress()).isEqualTo("서울시 강남구 테헤란로 1");
            assertThat(dto.getStatus()).isNotNull();
            assertThat(dto.getContractType()).isNotNull();
        }
    }

    private Member member(Long id, String code) {
        Member m = Member.builder().memberCode(code).build();
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }

    private Contract contractWithSchedules(List<ContractPaymentSchedule> schedules) {
        Contract contract = Contract.createContract(lessor, baseDto());
        ReflectionTestUtils.setField(contract, "id", 100L);
        ReflectionTestUtils.setField(contract, "paymentSchedules", schedules);
        return contract;
    }

    private ContractPaymentSchedule schedule(LocalDate due, Long amount, PaymentType type, int order) {
        return ContractPaymentSchedule.createPaymentSchedule(new Contract(), type, amount, due, order);
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
}
