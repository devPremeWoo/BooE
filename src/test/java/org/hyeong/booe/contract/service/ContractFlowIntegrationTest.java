package org.hyeong.booe.contract.service;

import org.hyeong.booe.contract.ContractFixture;
import org.hyeong.booe.contract.domain.Contract;
import org.hyeong.booe.contract.domain.ContractFormData;
import org.hyeong.booe.contract.domain.ContractParty;
import org.hyeong.booe.contract.domain.type.ContractStatus;
import org.hyeong.booe.contract.domain.type.PartyRole;
import org.hyeong.booe.contract.dto.req.ContractBaseReqDto;
import org.hyeong.booe.contract.dto.req.DownPaymentConfirmReqDto;
import org.hyeong.booe.contract.dto.req.ReviewRequestDto;
import org.hyeong.booe.contract.dto.res.ContractResDto;
import org.hyeong.booe.contract.repository.ContractFormDataRepository;
import org.hyeong.booe.contract.repository.ContractPartyRepository;
import org.hyeong.booe.contract.repository.ContractRepository;
import org.hyeong.booe.exception.ContractAccessDeniedException;
import org.hyeong.booe.global.fcm.FcmService;
import org.hyeong.booe.member.domain.Member;
import org.hyeong.booe.member.domain.MemberDevice;
import org.hyeong.booe.member.domain.MemberProfile;
import org.hyeong.booe.member.domain.type.Platform;
import org.hyeong.booe.member.repository.MemberDeviceRepository;
import org.hyeong.booe.member.repository.MemberProfileRepository;
import org.hyeong.booe.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
@DisplayName("계약 흐름 통합 테스트")
class ContractFlowIntegrationTest {

    @Autowired ContractService contractService;
    @Autowired ContractRepository contractRepository;
    @Autowired ContractFormDataRepository contractFormDataRepository;
    @Autowired ContractPartyRepository contractPartyRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired MemberProfileRepository memberProfileRepository;
    @Autowired MemberDeviceRepository memberDeviceRepository;

    @MockBean FcmService fcmService;

    Member lessor;
    Member lessee;

    @BeforeEach
    void setUp() {
        lessor = memberRepository.save(Member.builder().memberCode("lessor-integ-001").build());
        lessee = memberRepository.save(Member.builder().memberCode("lessee-integ-001").build());

        memberProfileRepository.save(MemberProfile.builder()
                .member(lessee)
                .name("김임차")
                .email("lessee@test.com")
                .phoneNumber("01033334444")
                .birth(LocalDate.of(1990, 1, 1))
                .build());

        memberDeviceRepository.save(MemberDevice.create(lessor, "device-lessor-1", "fcm-token-lessor", Platform.IOS));
        memberDeviceRepository.save(MemberDevice.create(lessee, "device-lessee-1", "fcm-token-lessee", Platform.IOS));
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────

    private Long saveNewDraft() {
        return contractService.save(ContractFixture.saveDto(null), lessor.getId());
    }

    private Long saveAndRequestReview() {
        Long contractId = saveNewDraft();

        ContractBaseReqDto saveDto = ContractFixture.saveDto(contractId);
        ReviewRequestDto reviewDto = new ReviewRequestDto();
        ReflectionTestUtils.setField(reviewDto, "contract", saveDto);
        ReflectionTestUtils.setField(reviewDto, "lesseePhoneNumber", "01033334444");

        contractService.requestReview(reviewDto, lessor.getId());
        return contractId;
    }

    private Long saveAndSubmitLessee() {
        Long contractId = saveAndRequestReview();
        contractService.submitLesseeInfo(contractId, ContractFixture.saveDto(contractId), lessee.getId());
        return contractId;
    }

    // ── 임시저장 ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("임시저장 (save)")
    class Save {

        @Test
        @DisplayName("신규 계약 생성 시 계약서·폼데이터·당사자가 저장된다")
        void 신규계약_저장_성공() {
            Long contractId = saveNewDraft();

            Contract saved = contractRepository.findById(contractId).orElseThrow();
            Optional<ContractFormData> formData = contractFormDataRepository.findById(contractId);
            List<ContractParty> parties = contractPartyRepository.findAll().stream()
                    .filter(p -> p.getContract().getId().equals(contractId))
                    .toList();

            assertThat(saved.getStatus()).isEqualTo(ContractStatus.DRAFT);
            assertThat(saved.getMember().getId()).isEqualTo(lessor.getId());
            assertThat(formData).isPresent();
            assertThat(parties).hasSize(2); // 임대인 1 + 임차인 1
        }

        @Test
        @DisplayName("기존 계약 수정 시 Contract는 재생성하지 않고 폼데이터·당사자만 갱신된다")
        void 기존계약_수정_성공() {
            Long contractId = saveNewDraft();
            long contractCountBefore = contractRepository.count();

            ContractBaseReqDto updateDto = ContractFixture.saveDto(contractId);
            contractService.save(updateDto, lessor.getId());

            assertThat(contractRepository.count()).isEqualTo(contractCountBefore);

            List<ContractParty> parties = contractPartyRepository.findAll().stream()
                    .filter(p -> p.getContract().getId().equals(contractId))
                    .toList();
            assertThat(parties).hasSize(2);
        }
    }

    // ── 확인 요청 ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("확인 요청 (requestReview)")
    class RequestReview {

        @Test
        @DisplayName("확인 요청 후 계약 상태가 REVIEW_REQUESTED로 바뀌고 임차인에게 FCM이 발송된다")
        void 확인요청_성공() {
            Long contractId = saveAndRequestReview();

            Contract contract = contractRepository.findById(contractId).orElseThrow();
            assertThat(contract.getStatus()).isEqualTo(ContractStatus.REVIEW_REQUESTED);
            assertThat(contract.getLesseeMember().getId()).isEqualTo(lessee.getId());

            verify(fcmService).sendToAll(
                    List.of("fcm-token-lessee"),
                    "계약서 확인 요청",
                    "임대인이 계약서 확인을 요청했습니다.",
                    String.valueOf(contractId)
            );
        }
    }

    // ── 계약서 조회 ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("계약서 조회 (getContract)")
    class GetContract {

        @Test
        @DisplayName("임대인이 계약서를 조회하면 폼 JSON을 포함한 응답을 받는다")
        void 임대인_조회_성공() {
            Long contractId = saveAndRequestReview();

            ContractResDto result = contractService.getContract(contractId, lessor.getId());

            assertThat(result.getContractId()).isEqualTo(contractId);
            assertThat(result.getStatus()).isEqualTo(ContractStatus.REVIEW_REQUESTED);
            assertThat(result.getFormJson()).isNotBlank();
        }

        @Test
        @DisplayName("임차인이 계약서를 조회할 수 있다")
        void 임차인_조회_성공() {
            Long contractId = saveAndRequestReview();

            ContractResDto result = contractService.getContract(contractId, lessee.getId());

            assertThat(result.getContractId()).isEqualTo(contractId);
        }

        @Test
        @DisplayName("관련 없는 사용자가 접근하면 ContractAccessDeniedException이 발생한다")
        void 관련없는_사용자_예외() {
            Long contractId = saveAndRequestReview();
            Member outsider = memberRepository.save(Member.builder().memberCode("outsider-001").build());

            assertThatThrownBy(() -> contractService.getContract(contractId, outsider.getId()))
                    .isInstanceOf(ContractAccessDeniedException.class);
        }
    }

    // ── 임차인 정보 입력 ───────────────────────────────────────────────────

    @Nested
    @DisplayName("임차인 정보 입력 (submitLesseeInfo)")
    class SubmitLesseeInfo {

        @Test
        @DisplayName("임차인이 정보를 입력하면 상태가 LESSEE_SUBMITTED로 바뀌고 임대인에게 FCM이 발송된다")
        void 임차인_정보입력_성공() {
            Long contractId = saveAndRequestReview();

            contractService.submitLesseeInfo(contractId, ContractFixture.saveDto(contractId), lessee.getId());

            Contract contract = contractRepository.findById(contractId).orElseThrow();
            assertThat(contract.getStatus()).isEqualTo(ContractStatus.LESSEE_SUBMITTED);

            verify(fcmService).sendToAll(
                    List.of("fcm-token-lessor"),
                    "임차인 정보 입력 완료",
                    "임차인이 정보를 입력했습니다. 확인 후 결제를 진행해주세요.",
                    String.valueOf(contractId)
            );
        }

        @Test
        @DisplayName("임차인이 아닌 사용자가 제출하면 ContractAccessDeniedException이 발생한다")
        void 권한없는_사용자_예외() {
            Long contractId = saveAndRequestReview();

            assertThatThrownBy(() ->
                    contractService.submitLesseeInfo(contractId, ContractFixture.saveDto(contractId), lessor.getId()))
                    .isInstanceOf(ContractAccessDeniedException.class);
        }
    }

    // ── 계약금 수령 확인 ───────────────────────────────────────────────────

    @Nested
    @DisplayName("계약금 수령 확인 (confirmDownPayment)")
    class ConfirmDownPayment {

        @Test
        @DisplayName("임대인이 계약금 수령 확인 후 상태가 PAYMENT_PENDING으로 바뀌고 영수자 정보가 저장된다")
        void 계약금_수령확인_성공() {
            Long contractId = saveAndSubmitLessee();

            DownPaymentConfirmReqDto dto = new DownPaymentConfirmReqDto();
            ReflectionTestUtils.setField(dto, "receiverName", "홍임대");
            ReflectionTestUtils.setField(dto, "receiverPhone", "01011112222");

            contractService.confirmDownPayment(contractId, dto, lessor.getId());

            Contract contract = contractRepository.findById(contractId).orElseThrow();
            assertThat(contract.getStatus()).isEqualTo(ContractStatus.PAYMENT_PENDING);
            assertThat(contract.getReceiverName()).isEqualTo("홍임대");
            assertThat(contract.getReceiverPhone()).isEqualTo("01011112222");
        }

        @Test
        @DisplayName("임대인이 아닌 사용자가 확인하면 ContractAccessDeniedException이 발생한다")
        void 권한없는_사용자_예외() {
            Long contractId = saveAndSubmitLessee();

            assertThatThrownBy(() ->
                    contractService.confirmDownPayment(contractId, new DownPaymentConfirmReqDto(), lessee.getId()))
                    .isInstanceOf(ContractAccessDeniedException.class);
        }
    }

    // ── 전체 흐름 ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("전체 계약 흐름")
    class FullFlow {

        @Test
        @DisplayName("DRAFT → REVIEW_REQUESTED → LESSEE_SUBMITTED → PAYMENT_PENDING 상태 전환 검증")
        void 전체_상태_전환_검증() {
            // 1. 임시저장 (DRAFT)
            Long contractId = saveNewDraft();
            assertThat(contractRepository.findById(contractId).orElseThrow().getStatus())
                    .isEqualTo(ContractStatus.DRAFT);

            // 2. 확인 요청 (REVIEW_REQUESTED)
            ContractBaseReqDto saveDto = ContractFixture.saveDto(contractId);
            ReviewRequestDto reviewDto = new ReviewRequestDto();
            ReflectionTestUtils.setField(reviewDto, "contract", saveDto);
            ReflectionTestUtils.setField(reviewDto, "lesseePhoneNumber", "01033334444");
            contractService.requestReview(reviewDto, lessor.getId());
            assertThat(contractRepository.findById(contractId).orElseThrow().getStatus())
                    .isEqualTo(ContractStatus.REVIEW_REQUESTED);

            // 3. 임차인 정보 입력 (LESSEE_SUBMITTED)
            contractService.submitLesseeInfo(contractId, ContractFixture.saveDto(contractId), lessee.getId());
            assertThat(contractRepository.findById(contractId).orElseThrow().getStatus())
                    .isEqualTo(ContractStatus.LESSEE_SUBMITTED);

            // 4. 계약금 수령 확인 (PAYMENT_PENDING)
            DownPaymentConfirmReqDto confirmDto = new DownPaymentConfirmReqDto();
            ReflectionTestUtils.setField(confirmDto, "receiverName", "홍임대");
            ReflectionTestUtils.setField(confirmDto, "receiverPhone", "01011112222");
            contractService.confirmDownPayment(contractId, confirmDto, lessor.getId());
            assertThat(contractRepository.findById(contractId).orElseThrow().getStatus())
                    .isEqualTo(ContractStatus.PAYMENT_PENDING);
        }

        @Test
        @DisplayName("임차인 정보 입력 시 임차인 당사자만 교체되고 임대인 당사자는 유지된다")
        void 임차인_당사자_교체_검증() {
            Long contractId = saveAndRequestReview();

            List<ContractParty> before = contractPartyRepository.findAll().stream()
                    .filter(p -> p.getContract().getId().equals(contractId))
                    .toList();
            long lessorPartyCount = before.stream()
                    .filter(p -> p.getRole() == PartyRole.LESSOR || p.getRole() == PartyRole.CO_LESSOR)
                    .count();

            contractService.submitLesseeInfo(contractId, ContractFixture.saveDto(contractId), lessee.getId());

            List<ContractParty> after = contractPartyRepository.findAll().stream()
                    .filter(p -> p.getContract().getId().equals(contractId))
                    .toList();
            long lessorPartyCountAfter = after.stream()
                    .filter(p -> p.getRole() == PartyRole.LESSOR || p.getRole() == PartyRole.CO_LESSOR)
                    .count();

            // 임대인 당사자 수는 변하지 않아야 한다
            assertThat(lessorPartyCountAfter).isEqualTo(lessorPartyCount);
            // 임차인 당사자가 존재해야 한다
            assertThat(after).anyMatch(p -> p.getRole() == PartyRole.LESSEE);
        }
    }
}
