package org.hyeong.booe.member.dto.res;

import org.hyeong.booe.global.security.jwt.TokenResDto;
import org.hyeong.booe.member.domain.Member;
import org.hyeong.booe.member.domain.MemberProfile;

public record LocalLoginResDto(
            String memberCode,
            String name,
            String role,
            TokenResDto token
    ) {
    public static LocalLoginResDto of(Member member, MemberProfile memberProfile, TokenResDto token) {
        return new LocalLoginResDto(
                member.getMemberCode(),
                memberProfile.getName(),
                member.getRole().name(),
                token
        );
    }
}
