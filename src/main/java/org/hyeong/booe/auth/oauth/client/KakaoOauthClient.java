package org.hyeong.booe.auth.oauth.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.auth.oauth.dto.KakaoUserInfoDto;
import org.hyeong.booe.exception.OauthUserInfoFetchException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@RequiredArgsConstructor
@Slf4j
public class KakaoOauthClient {

    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final WebClient kakaoWebClient;

    public KakaoUserInfoDto getUserInfo(String accessToken) {
        try {
            return kakaoWebClient.get()
                    .uri(KAKAO_USER_INFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(KakaoUserInfoDto.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("[KakaoOauth] 사용자 정보 조회 실패 - status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new OauthUserInfoFetchException();
        }
    }
}
