package org.hyeong.booe.auth.oauth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.auth.oauth.client.KakaoOauthClient;
import org.hyeong.booe.auth.oauth.dto.KakaoUserInfoDto;
import org.hyeong.booe.auth.oauth.dto.OauthAuthResDto;
import org.hyeong.booe.auth.oauth.dto.OauthSignupTempInfo;
import org.hyeong.booe.exception.ProfileNotFoundException;
import org.hyeong.booe.global.security.jwt.JwtProvider;
import org.hyeong.booe.global.security.jwt.RefreshTokenRedisService;
import org.hyeong.booe.global.security.jwt.TokenResDto;
import org.hyeong.booe.member.domain.Member;
import org.hyeong.booe.member.domain.MemberOauthConnection;
import org.hyeong.booe.member.domain.MemberProfile;
import org.hyeong.booe.member.domain.type.OauthProviderType;
import org.hyeong.booe.member.repository.MemberOauthConnectionRepository;
import org.hyeong.booe.member.repository.MemberProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoAuthService {

    private final KakaoOauthClient kakaoOauthClient;
    private final MemberOauthConnectionRepository memberOauthConnectionRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final OauthSignupRedisService oauthSignupRedisService;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRedisService refreshTokenRedisService;

    @Transactional
    public OauthAuthResDto kakaoLogin(String accessToken) {
        KakaoUserInfoDto userInfo = kakaoOauthClient.getUserInfo(accessToken);
        String providerUserId = String.valueOf(userInfo.getId());

        return memberOauthConnectionRepository
                .findByProviderTypeAndProviderUserId(OauthProviderType.KAKAO, providerUserId)
                .map(this::handleExistingMember)
                .orElseGet(() -> handleNewUser(providerUserId));
    }

    private OauthAuthResDto handleExistingMember(MemberOauthConnection connection) {
        connection.updateLastLogin();
        Member member = connection.getMember();
        MemberProfile profile = findProfile(member);
        TokenResDto token = generateToken(member);
        refreshTokenRedisService.save(member.getMemberCode(), token.getRefreshToken());
        return OauthAuthResDto.existing(member, profile, token);
    }

    private OauthAuthResDto handleNewUser(String providerUserId) {
        OauthSignupTempInfo info = new OauthSignupTempInfo(OauthProviderType.KAKAO, providerUserId);
        String signupToken = oauthSignupRedisService.save(info);
        return OauthAuthResDto.newUser(signupToken);
    }

    private MemberProfile findProfile(Member member) {
        return memberProfileRepository.findByMember(member)
                .orElseThrow(ProfileNotFoundException::new);
    }

    private TokenResDto generateToken(Member member) {
        String accessToken = jwtProvider.generateAccessToken(member.getMemberCode(), member.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(member.getMemberCode());
        return new TokenResDto(accessToken, refreshToken);
    }
}
