package org.hyeong.booe.auth.oauth.service;

import org.hyeong.booe.auth.oauth.dto.OauthSignupReqDto;
import org.hyeong.booe.auth.oauth.dto.OauthSignupResDto;
import org.hyeong.booe.auth.oauth.dto.OauthSignupTempInfo;
import org.hyeong.booe.exception.InvalidSignupTokenException;
import org.hyeong.booe.global.security.jwt.JwtProvider;
import org.hyeong.booe.global.security.jwt.RefreshTokenRedisService;
import org.hyeong.booe.member.domain.Member;
import org.hyeong.booe.member.domain.MemberOauthConnection;
import org.hyeong.booe.member.domain.MemberProfile;
import org.hyeong.booe.member.domain.type.OauthProviderType;
import org.hyeong.booe.member.repository.MemberOauthConnectionRepository;
import org.hyeong.booe.member.repository.MemberProfileRepository;
import org.hyeong.booe.member.repository.MemberRepository;
import org.hyeong.booe.member.service.util.MemberCodeGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OauthSignupServiceTest {

    @Mock private OauthSignupRedisService oauthSignupRedisService;
    @Mock private MemberRepository memberRepository;
    @Mock private MemberProfileRepository memberProfileRepository;
    @Mock private MemberOauthConnectionRepository memberOauthConnectionRepository;
    @Mock private MemberCodeGenerator memberCodeGenerator;
    @Mock private JwtProvider jwtProvider;
    @Mock private RefreshTokenRedisService refreshTokenRedisService;

    @InjectMocks private OauthSignupService oauthSignupService;

    private OauthSignupReqDto buildReqDto() {
        OauthSignupReqDto dto = new OauthSignupReqDto();
        ReflectionTestUtils.setField(dto, "signupToken", "token-uuid");
        ReflectionTestUtils.setField(dto, "email", "a@b.com");
        ReflectionTestUtils.setField(dto, "name", "홍길동");
        ReflectionTestUtils.setField(dto, "birthDate", LocalDate.of(1990, 1, 1));
        ReflectionTestUtils.setField(dto, "phoneNum", "01012345678");
        ReflectionTestUtils.setField(dto, "termsAgreed", true);
        return dto;
    }

    @Nested
    @DisplayName("signup 정상 흐름")
    class Success {

        @Test
        @DisplayName("Redis 임시정보로 Member/Profile/OauthConnection 생성, JWT 발급, 임시토큰 삭제")
        void create_full_account() {
            OauthSignupReqDto reqDto = buildReqDto();
            OauthSignupTempInfo tempInfo = new OauthSignupTempInfo(OauthProviderType.KAKAO, "42");

            when(oauthSignupRedisService.find("token-uuid")).thenReturn(Optional.of(tempInfo));
            when(memberCodeGenerator.generate()).thenReturn("booe_new");
            when(memberRepository.save(any(Member.class))).thenAnswer(inv -> {
                Member m = inv.getArgument(0);
                ReflectionTestUtils.setField(m, "id", 100L);
                return m;
            });
            when(memberProfileRepository.save(any(MemberProfile.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jwtProvider.generateAccessToken(any(), any())).thenReturn("access-jwt");
            when(jwtProvider.generateRefreshToken(any())).thenReturn("refresh-jwt");

            OauthSignupResDto result = oauthSignupService.signup(reqDto);

            assertThat(result.memberCode()).isEqualTo("booe_new");
            assertThat(result.name()).isEqualTo("홍길동");
            assertThat(result.token().getAccessToken()).isEqualTo("access-jwt");
            assertThat(result.token().getRefreshToken()).isEqualTo("refresh-jwt");

            verify(memberRepository).save(any(Member.class));
            verify(memberProfileRepository).save(any(MemberProfile.class));
            verify(memberOauthConnectionRepository).save(any(MemberOauthConnection.class));
            verify(refreshTokenRedisService).save("booe_new", "refresh-jwt");
            verify(oauthSignupRedisService).delete("token-uuid");
        }

        @Test
        @DisplayName("OauthConnection이 카카오 + providerUserId로 저장된다")
        void connection_persists_provider_info() {
            OauthSignupReqDto reqDto = buildReqDto();
            OauthSignupTempInfo tempInfo = new OauthSignupTempInfo(OauthProviderType.KAKAO, "kakao_42");

            when(oauthSignupRedisService.find("token-uuid")).thenReturn(Optional.of(tempInfo));
            when(memberCodeGenerator.generate()).thenReturn("booe_new");
            when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));
            when(memberProfileRepository.save(any(MemberProfile.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jwtProvider.generateAccessToken(any(), any())).thenReturn("a");
            when(jwtProvider.generateRefreshToken(any())).thenReturn("r");

            oauthSignupService.signup(reqDto);

            ArgumentCaptor<MemberOauthConnection> captor = ArgumentCaptor.forClass(MemberOauthConnection.class);
            verify(memberOauthConnectionRepository).save(captor.capture());
            MemberOauthConnection saved = captor.getValue();
            assertThat(saved.getProviderType()).isEqualTo(OauthProviderType.KAKAO);
            assertThat(saved.getProviderUserId()).isEqualTo("kakao_42");
        }
    }

    @Nested
    @DisplayName("signup 예외 흐름")
    class Failure {

        @Test
        @DisplayName("signupToken이 만료/없으면 InvalidSignupTokenException")
        void invalid_signup_token() {
            OauthSignupReqDto reqDto = buildReqDto();
            when(oauthSignupRedisService.find("token-uuid")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> oauthSignupService.signup(reqDto))
                    .isInstanceOf(InvalidSignupTokenException.class);

            verify(memberRepository, never()).save(any());
            verify(memberProfileRepository, never()).save(any());
            verify(memberOauthConnectionRepository, never()).save(any());
            verify(oauthSignupRedisService, never()).delete(any());
        }
    }
}
