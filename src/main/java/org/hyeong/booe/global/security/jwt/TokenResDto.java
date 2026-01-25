package org.hyeong.booe.global.security.jwt;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TokenResDto {

    private String accessToken;
    private String refreshToken;
}
