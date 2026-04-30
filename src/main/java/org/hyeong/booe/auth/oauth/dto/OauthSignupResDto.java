package org.hyeong.booe.auth.oauth.dto;

import org.hyeong.booe.global.security.jwt.TokenResDto;
import org.hyeong.booe.member.domain.Member;
import org.hyeong.booe.member.domain.MemberProfile;

public record OauthSignupResDto(
        String memberCode,
        String name,
        String role,
        TokenResDto token
) {
    public static OauthSignupResDto of(Member member, MemberProfile profile, TokenResDto token) {
        return new OauthSignupResDto(
                member.getMemberCode(),
                profile.getName(),
                member.getRole().name(),
                token
        );
    }
}
