package org.hyeong.booe.auth.local.service;

import org.hyeong.booe.auth.local.dto.req.LocalLoginRequestDto;
import org.hyeong.booe.auth.local.dto.req.LocalSignupRequestDto;
import org.hyeong.booe.auth.local.dto.res.LocalLoginResDto;
import org.hyeong.booe.auth.local.dto.res.LocalSignupResDto;
import org.hyeong.booe.exception.AlreadyRegisteredMemberException;
import org.hyeong.booe.exception.DuplicateLoginIdException;
import org.hyeong.booe.exception.InvalidPasswordException;
import org.hyeong.booe.exception.MemberNotFoundException;
import org.hyeong.booe.exception.PasswordMismatchException;
import org.hyeong.booe.exception.PhoneVerificationNotFoundException;
import org.hyeong.booe.exception.ProfileNotFoundException;
import org.hyeong.booe.exception.VerificationNotCompletedException;
import org.hyeong.booe.exception.VerificationPhoneMismatchException;
import org.hyeong.booe.global.security.jwt.JwtProvider;
import org.hyeong.booe.global.security.jwt.RefreshTokenRedisService;
import org.hyeong.booe.member.domain.Member;
import org.hyeong.booe.member.domain.MemberCredential;
import org.hyeong.booe.member.domain.MemberProfile;
import org.hyeong.booe.member.domain.type.MemberStatus;
import org.hyeong.booe.member.repository.MemberCredentialRepository;
import org.hyeong.booe.member.repository.MemberProfileRepository;
import org.hyeong.booe.member.repository.MemberRepository;
import org.hyeong.booe.member.service.util.MemberCodeGenerator;
import org.hyeong.booe.verification.domain.PhoneVerification;
import org.hyeong.booe.verification.repository.PhoneVerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalAuthServiceTest {

    @Mock private MemberRepository memberRepository;
    @Mock private MemberCredentialRepository memberCredentialRepository;
    @Mock private MemberProfileRepository memberProfileRepository;
    @Mock private PhoneVerificationRepository phoneVerificationRepository;
    @Mock private MemberCodeGenerator memberCodeGenerator;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtProvider jwtProvider;
    @Mock private RefreshTokenRedisService refreshTokenRedisService;

    @InjectMocks private LocalAuthService localAuthService;

    @Nested
    @DisplayName("signup")
    class Signup {

        private LocalSignupRequestDto buildSignupDto() {
            LocalSignupRequestDto dto = new LocalSignupRequestDto();
            ReflectionTestUtils.setField(dto, "loginId", "user01");
            ReflectionTestUtils.setField(dto, "password", "Password1234!");
            ReflectionTestUtils.setField(dto, "passwordConfirm", "Password1234!");
            ReflectionTestUtils.setField(dto, "name", "홍길동");
            ReflectionTestUtils.setField(dto, "nickname", "닉네임");
            ReflectionTestUtils.setField(dto, "birthDate", LocalDate.of(1990, 1, 1));
            ReflectionTestUtils.setField(dto, "email", "a@b.com");
            ReflectionTestUtils.setField(dto, "phoneNum", "01012345678");
            ReflectionTestUtils.setField(dto, "termsAgreed", true);
            ReflectionTestUtils.setField(dto, "verificationId", 1L);
            return dto;
        }

        private PhoneVerification verifiedRecord(String phone) {
            PhoneVerification verification = PhoneVerification.builder()
                    .phoneNumber(phone)
                    .verificationCode("123456")
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .build();
            verification.verify();
            return verification;
        }

        @Test
        @DisplayName("정상 가입 시 Member/Profile/Credential이 모두 저장된다")
        void success() {
            LocalSignupRequestDto dto = buildSignupDto();
            when(phoneVerificationRepository.findById(1L)).thenReturn(Optional.of(verifiedRecord("01012345678")));
            when(memberProfileRepository.findByPhoneNumberAndMemberStatus("01012345678", MemberStatus.ACTIVE))
                    .thenReturn(Optional.empty());
            when(memberCredentialRepository.existsByLoginId("user01")).thenReturn(false);
            when(memberCodeGenerator.generate()).thenReturn("booe_abc");
            when(passwordEncoder.encode("Password1234!")).thenReturn("encoded");
            when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

            LocalSignupResDto result = localAuthService.signup(dto);

            assertThat(result.getMemberCode()).isEqualTo("booe_abc");
            verify(memberRepository).save(any(Member.class));
            verify(memberProfileRepository).save(any(MemberProfile.class));
            verify(memberCredentialRepository).save(any(MemberCredential.class));
        }

        @Test
        @DisplayName("PhoneVerification이 없으면 PhoneVerificationNotFoundException")
        void phone_verification_not_found() {
            LocalSignupRequestDto dto = buildSignupDto();
            when(phoneVerificationRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> localAuthService.signup(dto))
                    .isInstanceOf(PhoneVerificationNotFoundException.class);
        }

        @Test
        @DisplayName("PhoneVerification이 미인증이면 VerificationNotCompletedException")
        void phone_verification_not_completed() {
            LocalSignupRequestDto dto = buildSignupDto();
            PhoneVerification pending = PhoneVerification.builder()
                    .phoneNumber("01012345678")
                    .verificationCode("123456")
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .build();
            when(phoneVerificationRepository.findById(1L)).thenReturn(Optional.of(pending));

            assertThatThrownBy(() -> localAuthService.signup(dto))
                    .isInstanceOf(VerificationNotCompletedException.class);
        }

        @Test
        @DisplayName("PhoneVerification 번호와 입력 번호가 다르면 VerificationPhoneMismatchException")
        void phone_mismatch() {
            LocalSignupRequestDto dto = buildSignupDto();
            when(phoneVerificationRepository.findById(1L)).thenReturn(Optional.of(verifiedRecord("01099998888")));

            assertThatThrownBy(() -> localAuthService.signup(dto))
                    .isInstanceOf(VerificationPhoneMismatchException.class);
        }

        @Test
        @DisplayName("같은 사람(이름+생일 일치)이 같은 번호로 재가입 시도하면 AlreadyRegisteredMemberException")
        void same_person_already_registered() {
            LocalSignupRequestDto dto = buildSignupDto();
            Member existingMember = Member.builder().memberCode("booe_old").build();
            MemberProfile existingProfile = MemberProfile.builder()
                    .member(existingMember)
                    .email("old@b.com").name("홍길동")
                    .phoneNumber("01012345678")
                    .birth(LocalDate.of(1990, 1, 1))
                    .build();
            when(phoneVerificationRepository.findById(1L)).thenReturn(Optional.of(verifiedRecord("01012345678")));
            when(memberProfileRepository.findByPhoneNumberAndMemberStatus("01012345678", MemberStatus.ACTIVE))
                    .thenReturn(Optional.of(existingProfile));

            assertThatThrownBy(() -> localAuthService.signup(dto))
                    .isInstanceOf(AlreadyRegisteredMemberException.class);
        }

        @Test
        @DisplayName("loginId가 중복이면 DuplicateLoginIdException")
        void duplicate_login_id() {
            LocalSignupRequestDto dto = buildSignupDto();
            when(phoneVerificationRepository.findById(1L)).thenReturn(Optional.of(verifiedRecord("01012345678")));
            when(memberProfileRepository.findByPhoneNumberAndMemberStatus("01012345678", MemberStatus.ACTIVE))
                    .thenReturn(Optional.empty());
            when(memberCredentialRepository.existsByLoginId("user01")).thenReturn(true);
            lenient().when(memberCodeGenerator.generate()).thenReturn("booe_abc");
            lenient().when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

            assertThatThrownBy(() -> localAuthService.signup(dto))
                    .isInstanceOf(DuplicateLoginIdException.class);
        }

        @Test
        @DisplayName("password와 passwordConfirm 불일치 시 PasswordMismatchException")
        void password_mismatch() {
            LocalSignupRequestDto dto = buildSignupDto();
            ReflectionTestUtils.setField(dto, "passwordConfirm", "Different1234!");
            when(phoneVerificationRepository.findById(1L)).thenReturn(Optional.of(verifiedRecord("01012345678")));
            when(memberProfileRepository.findByPhoneNumberAndMemberStatus("01012345678", MemberStatus.ACTIVE))
                    .thenReturn(Optional.empty());
            when(memberCredentialRepository.existsByLoginId("user01")).thenReturn(false);
            lenient().when(memberCodeGenerator.generate()).thenReturn("booe_abc");
            lenient().when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

            assertThatThrownBy(() -> localAuthService.signup(dto))
                    .isInstanceOf(PasswordMismatchException.class);
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("정상 로그인 시 토큰 발급 + RefreshToken Redis 저장")
        void success() {
            Member member = Member.builder().memberCode("booe_xyz").build();
            ReflectionTestUtils.setField(member, "id", 1L);
            MemberProfile profile = MemberProfile.builder()
                    .member(member).email("a@b.com").name("홍길동")
                    .phoneNumber("01012345678").birth(LocalDate.of(1990, 1, 1)).build();
            MemberCredential credential = MemberCredential.builder()
                    .member(member).loginId("user01").encodedPassword("encoded").build();

            LocalLoginRequestDto reqDto = new LocalLoginRequestDto();
            ReflectionTestUtils.setField(reqDto, "loginId", "user01");
            ReflectionTestUtils.setField(reqDto, "password", "Password1234!");

            when(memberCredentialRepository.findByLoginId("user01")).thenReturn(Optional.of(credential));
            when(passwordEncoder.matches("Password1234!", "encoded")).thenReturn(true);
            when(memberProfileRepository.findByMember(member)).thenReturn(Optional.of(profile));
            when(jwtProvider.generateAccessToken(any(), any())).thenReturn("access");
            when(jwtProvider.generateRefreshToken(any())).thenReturn("refresh");

            LocalLoginResDto result = localAuthService.login(reqDto);

            assertThat(result).isNotNull();
            verify(refreshTokenRedisService).save("booe_xyz", "refresh");
        }

        @Test
        @DisplayName("loginId가 없으면 MemberNotFoundException")
        void member_not_found() {
            LocalLoginRequestDto reqDto = new LocalLoginRequestDto();
            ReflectionTestUtils.setField(reqDto, "loginId", "missing");
            ReflectionTestUtils.setField(reqDto, "password", "x");
            when(memberCredentialRepository.findByLoginId("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> localAuthService.login(reqDto))
                    .isInstanceOf(MemberNotFoundException.class);
        }

        @Test
        @DisplayName("비밀번호 불일치 시 InvalidPasswordException")
        void invalid_password() {
            Member member = Member.builder().memberCode("booe_xyz").build();
            MemberCredential credential = MemberCredential.builder()
                    .member(member).loginId("user01").encodedPassword("encoded").build();
            LocalLoginRequestDto reqDto = new LocalLoginRequestDto();
            ReflectionTestUtils.setField(reqDto, "loginId", "user01");
            ReflectionTestUtils.setField(reqDto, "password", "wrong");

            when(memberCredentialRepository.findByLoginId("user01")).thenReturn(Optional.of(credential));
            when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

            assertThatThrownBy(() -> localAuthService.login(reqDto))
                    .isInstanceOf(InvalidPasswordException.class);
        }

        @Test
        @DisplayName("프로필이 없으면 ProfileNotFoundException")
        void profile_not_found() {
            Member member = Member.builder().memberCode("booe_xyz").build();
            MemberCredential credential = MemberCredential.builder()
                    .member(member).loginId("user01").encodedPassword("encoded").build();
            LocalLoginRequestDto reqDto = new LocalLoginRequestDto();
            ReflectionTestUtils.setField(reqDto, "loginId", "user01");
            ReflectionTestUtils.setField(reqDto, "password", "Password1234!");

            when(memberCredentialRepository.findByLoginId("user01")).thenReturn(Optional.of(credential));
            when(passwordEncoder.matches("Password1234!", "encoded")).thenReturn(true);
            when(memberProfileRepository.findByMember(member)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> localAuthService.login(reqDto))
                    .isInstanceOf(ProfileNotFoundException.class);
        }
    }
}
