package org.hyeong.booe.auth.oauth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.auth.oauth.dto.OauthSignupReqDto;
import org.hyeong.booe.auth.oauth.dto.OauthSignupResDto;
import org.hyeong.booe.auth.oauth.dto.OauthSignupTempInfo;
import org.hyeong.booe.exception.InvalidSignupTokenException;
import org.hyeong.booe.global.security.jwt.JwtProvider;
import org.hyeong.booe.global.security.jwt.RefreshTokenRedisService;
import org.hyeong.booe.global.security.jwt.TokenResDto;
import org.hyeong.booe.member.domain.Member;
import org.hyeong.booe.member.domain.MemberOauthConnection;
import org.hyeong.booe.member.domain.MemberProfile;
import org.hyeong.booe.member.repository.MemberOauthConnectionRepository;
import org.hyeong.booe.member.repository.MemberProfileRepository;
import org.hyeong.booe.member.repository.MemberRepository;
import org.hyeong.booe.member.service.util.MemberCodeGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OauthSignupService {

    private final OauthSignupRedisService oauthSignupRedisService;
    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final MemberOauthConnectionRepository memberOauthConnectionRepository;
    private final MemberCodeGenerator memberCodeGenerator;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRedisService refreshTokenRedisService;

    @Transactional
    public OauthSignupResDto signup(OauthSignupReqDto reqDto) {
        OauthSignupTempInfo info = findTempInfo(reqDto.getSignupToken());

        Member member = createMember();
        MemberProfile profile = memberProfileRepository.save(reqDto.toMemberProfile(member));
        createOauthConnection(member, info);

        TokenResDto token = issueToken(member);
        oauthSignupRedisService.delete(reqDto.getSignupToken());

        return OauthSignupResDto.of(member, profile, token);
    }

    private OauthSignupTempInfo findTempInfo(String signupToken) {
        return oauthSignupRedisService.find(signupToken)
                .orElseThrow(InvalidSignupTokenException::new);
    }

    private Member createMember() {
        Member member = Member.builder()
                .memberCode(memberCodeGenerator.generate())
                .build();
        return memberRepository.save(member);
    }

    private void createOauthConnection(Member member, OauthSignupTempInfo info) {
        MemberOauthConnection connection = MemberOauthConnection.create(
                member, info.getProviderType(), info.getProviderUserId()
        );
        memberOauthConnectionRepository.save(connection);
    }

    private TokenResDto issueToken(Member member) {
        String accessToken = jwtProvider.generateAccessToken(member.getMemberCode(), member.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(member.getMemberCode());
        TokenResDto token = new TokenResDto(accessToken, refreshToken);
        refreshTokenRedisService.save(member.getMemberCode(), refreshToken);
        return token;
    }
}
