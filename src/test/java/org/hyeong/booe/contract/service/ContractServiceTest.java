package org.hyeong.booe.contract.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hyeong.booe.contract.ContractFixture;
import org.hyeong.booe.contract.domain.Contract;
import org.hyeong.booe.contract.domain.ContractFormData;
import org.hyeong.booe.contract.domain.type.ContractStatus;
import org.hyeong.booe.contract.dto.req.ContractSaveReqDto;
import org.hyeong.booe.contract.dto.req.DownPaymentConfirmReqDto;
import org.hyeong.booe.contract.dto.req.ReviewRequestDto;
import org.hyeong.booe.contract.dto.res.ContractResDto;
import org.hyeong.booe.contract.repository.ContractFormDataRepository;
import org.hyeong.booe.contract.repository.ContractPartyRepository;
import org.hyeong.booe.contract.repository.ContractRepository;
import org.hyeong.booe.exception.ContractAccessDeniedException;
import org.hyeong.booe.exception.ContractNotFoundException;
import org.hyeong.booe.exception.MemberNotFoundException;
import org.hyeong.booe.exception.ProfileNotFoundException;
import org.hyeong.booe.global.fcm.FcmService;
import org.hyeong.booe.member.domain.Member;
import org.hyeong.booe.member.domain.MemberDevice;
import org.hyeong.booe.member.domain.MemberProfile;
import org.hyeong.booe.member.domain.type.MemberStatus;
import org.hyeong.booe.member.repository.MemberDeviceRepository;
import org.hyeong.booe.member.repository.MemberProfileRepository;
import org.hyeong.booe.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContractService 단위 테스트")
class ContractServiceTest {

    @Mock ContractRepository contractRepository;
    @Mock ContractPartyRepository contractPartyRepository;
    @Mock ContractFormDataRepository contractFormDataRepository;
    @Mock MemberRepository memberRepository;
    @Mock MemberProfileRepository memberProfileRepository;
    @Mock MemberDeviceRepository memberDeviceRepository;
    @Mock FcmService fcmService;

    @Spy
    ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @InjectMocks
    ContractService contractService;

    Member lessor;
    Member lessee;
    Contract contract;

    @BeforeEach
    void setUp() {
        lessor = ContractFixture.member(1L, "lessor-code");
        lessee = ContractFixture.member(2L, "lessee-code");
        contract = ContractFixture.contract(10L, lessor);
    }

    @Nested
    @DisplayName("임시저장 (save)")
    class Save {

        @Test
        @DisplayName("신규 계약 생성 성공")
        void 신규생성_성공() {
            ContractSaveReqDto dto = ContractFixture.saveDto(null);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(lessor));
            when(contractRepository.save(any())).thenReturn(contract);
            when(contractFormDataRepository.findById(anyLong())).thenReturn(Optional.empty());

            Long savedId = contractService.save(dto, 1L);

            assertThat(savedId).isEqualTo(10L);
            verify(contractRepository).save(any(Contract.class));
            verify(contractFormDataRepository).save(any(ContractFormData.class));
            verify(contractPartyRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("기존 계약 수정 성공")
        void 기존계약수정_성공() {
            ContractSaveReqDto dto = ContractFixture.saveDto(10L);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(lessor));
            when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));
            when(contractFormDataRepository.findById(10L))
                    .thenReturn(Optional.of(ContractFormData.create(contract, "{}")));

            contractService.save(dto, 1L);

            verify(contractRepository, never()).save(any());
            verify(contractPartyRepository).deleteAllByContract(contract);
            verify(contractPartyRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("존재하지 않는 회원으로 저장 시 예외 발생")
        void 존재하지않는회원_예외() {
            when(memberRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> contractService.save(ContractFixture.saveDto(null), 99L))
                    .isInstanceOf(MemberNotFoundException.class);
        }

        @Test
        @DisplayName("다른 사람의 계약 수정 시도 시 예외 발생")
        void 권한없는수정_예외() {
            ContractSaveReqDto dto = ContractFixture.saveDto(10L);
            when(memberRepository.findById(2L)).thenReturn(Optional.of(lessee));
            when(contractRepository.findById(10L)).thenReturn(Optional.of(contract)); // 소유자는 lessor(1L)

            assertThatThrownBy(() -> contractService.save(dto, 2L))
                    .isInstanceOf(ContractAccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("확인 요청 (requestReview)")
    class RequestReview {

        @Test
        @DisplayName("임차인 조회 후 FCM 발송 성공")
        void 확인요청_성공() {
            ContractSaveReqDto saveDto = ContractFixture.saveDto(10L);
            ReviewRequestDto dto = new ReviewRequestDto();
            ReflectionTestUtils.setField(dto, "contract", saveDto);
            ReflectionTestUtils.setField(dto, "lesseePhoneNumber", "01033334444");

            MemberProfile lesseeProfile = ContractFixture.profile(lessee, "김임차", "lessee@test.com", "01033334444");
            MemberDevice lesseeDevice = MemberDevice.create(lessee, "device-1", "fcm-token-lessee", null);

            when(memberRepository.findById(1L)).thenReturn(Optional.of(lessor));
            when(contractFormDataRepository.findById(anyLong()))
                    .thenReturn(Optional.of(ContractFormData.create(contract, "{}")));
            when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));
            when(memberProfileRepository.findByPhoneNumberAndMemberStatus("01033334444", MemberStatus.ACTIVE))
                    .thenReturn(Optional.of(lesseeProfile));
            when(memberDeviceRepository.findAllByMember(lessee)).thenReturn(List.of(lesseeDevice));

            contractService.requestReview(dto, 1L);

            assertThat(contract.getStatus()).isEqualTo(ContractStatus.REVIEW_REQUESTED);
            assertThat(contract.getLesseeMember()).isEqualTo(lessee);
            verify(fcmService).sendToAll(List.of("fcm-token-lessee"), "계약서 확인 요청",
                    "임대인이 계약서 확인을 요청했습니다.", "10");
        }

        @Test
        @DisplayName("존재하지 않는 임차인 번호로 요청 시 예외 발생")
        void 존재하지않는임차인번호_예외() {
            ContractSaveReqDto saveDto = ContractFixture.saveDto(10L);
            ReviewRequestDto dto = new ReviewRequestDto();
            ReflectionTestUtils.setField(dto, "contract", saveDto);
            ReflectionTestUtils.setField(dto, "lesseePhoneNumber", "01099999999");

            when(memberRepository.findById(1L)).thenReturn(Optional.of(lessor));
            when(contractFormDataRepository.findById(anyLong()))
                    .thenReturn(Optional.of(ContractFormData.create(contract, "{}")));
            when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));
            when(memberProfileRepository.findByPhoneNumberAndMemberStatus(anyString(), any()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> contractService.requestReview(dto, 1L))
                    .isInstanceOf(ProfileNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("임차인 정보 제출 (submitLesseeInfo)")
    class SubmitLesseeInfo {

        @BeforeEach
        void setLessee() {
            contract.requestReview(lessee);
        }

        @Test
        @DisplayName("임차인 정보 제출 성공 - 상태 변경 및 임대인 FCM 발송")
        void 임차인정보제출_성공() {
            ContractSaveReqDto dto = ContractFixture.saveDto(10L);
            MemberDevice lessorDevice = MemberDevice.create(lessor, "device-1", "fcm-token-lessor", null);

            when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));
            when(contractFormDataRepository.findById(10L))
                    .thenReturn(Optional.of(ContractFormData.create(contract, "{}")));
            when(memberDeviceRepository.findAllByMember(lessor)).thenReturn(List.of(lessorDevice));

            contractService.submitLesseeInfo(10L, dto, 2L);

            assertThat(contract.getStatus()).isEqualTo(ContractStatus.LESSEE_SUBMITTED);
            verify(contractPartyRepository).deleteAllByContractAndRoleIn(any(), anyList());
            verify(fcmService).sendToAll(List.of("fcm-token-lessor"), "임차인 정보 입력 완료",
                    "임차인이 정보를 입력했습니다. 확인 후 결제를 진행해주세요.", "10");
        }

        @Test
        @DisplayName("임차인이 아닌 사용자 접근 시 예외 발생")
        void 권한없는접근_예외() {
            when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

            assertThatThrownBy(() -> contractService.submitLesseeInfo(10L, ContractFixture.saveDto(10L), 99L))
                    .isInstanceOf(ContractAccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("계약금 수령 확인 (confirmDownPayment)")
    class ConfirmDownPayment {

        @Test
        @DisplayName("영수자 정보 저장 및 상태 PAYMENT_PENDING 전환 성공")
        void 계약금확인_성공() {
            DownPaymentConfirmReqDto dto = new DownPaymentConfirmReqDto();
            ReflectionTestUtils.setField(dto, "receiverName", "홍임대");
            ReflectionTestUtils.setField(dto, "receiverPhone", "01011112222");
            when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

            contractService.confirmDownPayment(10L, dto, 1L);

            assertThat(contract.getStatus()).isEqualTo(ContractStatus.PAYMENT_PENDING);
            assertThat(contract.getReceiverName()).isEqualTo("홍임대");
            assertThat(contract.getReceiverPhone()).isEqualTo("01011112222");
        }

        @Test
        @DisplayName("임대인이 아닌 사용자 접근 시 예외 발생")
        void 권한없는접근_예외() {
            when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

            assertThatThrownBy(() -> contractService.confirmDownPayment(10L, new DownPaymentConfirmReqDto(), 2L))
                    .isInstanceOf(ContractAccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("계약서 조회 (getContract)")
    class GetContract {

        @BeforeEach
        void setLessee() {
            contract.requestReview(lessee);
        }

        @Test
        @DisplayName("임대인 조회 성공")
        void 임대인조회_성공() {
            when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));
            when(contractFormDataRepository.findById(10L))
                    .thenReturn(Optional.of(ContractFormData.create(contract, "{\"title\":\"테스트\"}")));

            ContractResDto result = contractService.getContract(10L, 1L);

            assertThat(result.getContractId()).isEqualTo(10L);
            assertThat(result.getFormJson()).isEqualTo("{\"title\":\"테스트\"}");
        }

        @Test
        @DisplayName("임차인 조회 성공")
        void 임차인조회_성공() {
            when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));
            when(contractFormDataRepository.findById(10L))
                    .thenReturn(Optional.of(ContractFormData.create(contract, "{}")));

            ContractResDto result = contractService.getContract(10L, 2L);

            assertThat(result.getContractId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("관련 없는 사용자 접근 시 예외 발생")
        void 관련없는사용자_예외() {
            when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

            assertThatThrownBy(() -> contractService.getContract(10L, 99L))
                    .isInstanceOf(ContractAccessDeniedException.class);
        }

        @Test
        @DisplayName("존재하지 않는 계약 조회 시 예외 발생")
        void 존재하지않는계약_예외() {
            when(contractRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> contractService.getContract(99L, 1L))
                    .isInstanceOf(ContractNotFoundException.class);
        }
    }
}
