package org.hyeong.booe.global.security.jwt;

import lombok.RequiredArgsConstructor;
import org.hyeong.booe.exception.InvalidTokenException;
import org.hyeong.booe.exception.MemberNotFoundException;
import org.hyeong.booe.member.domain.Member;
import org.hyeong.booe.member.repository.MemberRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;
    private final RefreshTokenRedisService refreshTokenRedisService;

    public TokenResDto refresh(String refreshToken) {

        if (!jwtProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException();
        }

        String memberCode = jwtProvider.getClaims(refreshToken).getSubject();
        validateStoredToken(memberCode, refreshToken);

        Member member = memberRepository.findByMemberCode(memberCode)
                .orElseThrow(MemberNotFoundException::new);

        String accessToken = jwtProvider.generateAccessToken(memberCode, member.getRole());

        return new TokenResDto(accessToken, refreshToken);
    }

    private void validateStoredToken(String memberCode, String refreshToken) {
        String stored = refreshTokenRedisService.find(memberCode);
        if (stored == null || !stored.equals(refreshToken)) {
            throw new InvalidTokenException();
        }
    }
}
