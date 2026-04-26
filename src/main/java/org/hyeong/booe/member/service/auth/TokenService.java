package org.hyeong.booe.member.service.auth;

import lombok.RequiredArgsConstructor;
import org.hyeong.booe.exception.InvalidTokenException;
import org.hyeong.booe.exception.MemberNotFoundException;
import org.hyeong.booe.global.security.jwt.JwtProvider;
import org.hyeong.booe.global.security.jwt.TokenResDto;
import org.hyeong.booe.member.domain.Member;
import org.hyeong.booe.member.repository.MemberRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;
    public TokenResDto refresh(String refreshToken) {

        if (!jwtProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException();
        }

        String memberCode = jwtProvider.getClaims(refreshToken).getSubject();
        Member member = memberRepository.findByMemberCode(memberCode)
                .orElseThrow(MemberNotFoundException::new);

        String accessToken = jwtProvider.generateAccessToken(memberCode, member.getRole());

        return new TokenResDto(accessToken, refreshToken);
    }
}
