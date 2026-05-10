package org.hyeong.booe.auth.oauth.service;

import org.hyeong.booe.auth.oauth.client.KakaoOauthClient;
import org.hyeong.booe.auth.oauth.dto.KakaoUserInfoDto;
import org.hyeong.booe.auth.oauth.dto.OauthAuthResDto;
import org.hyeong.booe.auth.oauth.dto.OauthSignupTempInfo;
import org.hyeong.booe.exception.ProfileNotFoundException;
import org.hyeong.booe.global.security.jwt.JwtProvider;
import org.hyeong.booe.global.security.jwt.RefreshTokenRedisService;
import org.hyeong.booe.member.domain.Member;
import org.hyeong.booe.member.domain.MemberOauthConnection;
import org.hyeong.booe.member.domain.MemberProfile;
import org.hyeong.booe.member.domain.type.OauthProviderType;
import org.hyeong.booe.member.repository.MemberOauthConnectionRepository;
import org.hyeong.booe.member.repository.MemberProfileRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KakaoAuthServiceTest {

    @Mock private KakaoOauthClient kakaoOauthClient;
    @Mock private MemberOauthConnectionRepository memberOauthConnectionRepository;
    @Mock private MemberProfileRepository memberProfileRepository;
    @Mock private OauthSignupRedisService oauthSignupRedisService;
    @Mock private JwtProvider jwtProvider;
    @Mock private RefreshTokenRedisService refreshTokenRedisService;

    @InjectMocks private KakaoAuthService kakaoAuthService;

    private KakaoUserInfoDto userInfo(long id) {
        KakaoUserInfoDto dto = new KakaoUserInfoDto();
        ReflectionTestUtils.setField(dto, "id", id);
        return dto;
    }

    @Nested
    @DisplayName("기존 회원")
    class ExistingMember {

        @Test
        @DisplayName("OauthConnection 있으면 JWT 발급 + RefreshToken Redis 저장")
        void issue_token_for_existing() {
            Member member = Member.builder().memberCode("booe_xyz").build();
            ReflectionTestUtils.setField(member, "id", 1L);
            MemberProfile profile = MemberProfile.builder()
                    .member(member).email("a@b.com").name("홍길동")
                    .phoneNumber("01012345678").birth(LocalDate.of(1990, 1, 1)).build();

            MemberOauthConnection connection = MemberOauthConnection.create(
                    member, OauthProviderType.KAKAO, "kakao_42"
            );

            when(kakaoOauthClient.getUserInfo("access")).thenReturn(userInfo(42L));
            when(memberOauthConnectionRepository.findByProviderTypeAndProviderUserId(
                    OauthProviderType.KAKAO, "42")).thenReturn(Optional.of(connection));
            when(memberProfileRepository.findByMember(member)).thenReturn(Optional.of(profile));
            when(jwtProvider.generateAccessToken(any(), any())).thenReturn("access-jwt");
            when(jwtProvider.generateRefreshToken(any())).thenReturn("refresh-jwt");

            OauthAuthResDto result = kakaoAuthService.kakaoLogin("access");

            assertThat(result.isMember()).isTrue();
            assertThat(result.memberCode()).isEqualTo("booe_xyz");
            assertThat(result.token().getAccessToken()).isEqualTo("access-jwt");
            verify(refreshTokenRedisService).save("booe_xyz", "refresh-jwt");
        }

        @Test
        @DisplayName("프로필이 없으면 ProfileNotFoundException")
        void profile_not_found() {
            Member member = Member.builder().memberCode("booe_xyz").build();
            MemberOauthConnection connection = MemberOauthConnection.create(
                    member, OauthProviderType.KAKAO, "kakao_42"
            );

            when(kakaoOauthClient.getUserInfo("access")).thenReturn(userInfo(42L));
            when(memberOauthConnectionRepository.findByProviderTypeAndProviderUserId(
                    OauthProviderType.KAKAO, "42")).thenReturn(Optional.of(connection));
            when(memberProfileRepository.findByMember(member)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> kakaoAuthService.kakaoLogin("access"))
                    .isInstanceOf(ProfileNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("신규 회원")
    class NewUser {

        @Test
        @DisplayName("OauthConnection 없으면 signupToken만 Redis에 저장하고 반환")
        void issue_signup_token_for_new() {
            when(kakaoOauthClient.getUserInfo("access")).thenReturn(userInfo(42L));
            when(memberOauthConnectionRepository.findByProviderTypeAndProviderUserId(
                    OauthProviderType.KAKAO, "42")).thenReturn(Optional.empty());
            when(oauthSignupRedisService.save(any(OauthSignupTempInfo.class))).thenReturn("token-uuid");

            OauthAuthResDto result = kakaoAuthService.kakaoLogin("access");

            assertThat(result.isMember()).isFalse();
            assertThat(result.signupToken()).isEqualTo("token-uuid");
            assertThat(result.memberCode()).isNull();

            ArgumentCaptor<OauthSignupTempInfo> captor = ArgumentCaptor.forClass(OauthSignupTempInfo.class);
            verify(oauthSignupRedisService).save(captor.capture());
            OauthSignupTempInfo saved = captor.getValue();
            assertThat(saved.getProviderType()).isEqualTo(OauthProviderType.KAKAO);
            assertThat(saved.getProviderUserId()).isEqualTo("42");
        }
    }
}
