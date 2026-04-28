package org.hyeong.booe.auth.oauth.dto;

import org.hyeong.booe.global.security.jwt.TokenResDto;
import org.hyeong.booe.member.domain.Member;
import org.hyeong.booe.member.domain.MemberProfile;

public record OauthAuthResDto(
        boolean isMember,
        String memberCode,
        String name,
        String role,
        TokenResDto token,
        String signupToken
) {
    public static OauthAuthResDto existing(Member member, MemberProfile profile, TokenResDto token) {
        return new OauthAuthResDto(
                true,
                member.getMemberCode(),
                profile.getName(),
                member.getRole().name(),
                token,
                null
        );
    }

    public static OauthAuthResDto newUser(String signupToken) {
        return new OauthAuthResDto(
                false,
                null,
                null,
                null,
                null,
                signupToken
        );
    }
}
