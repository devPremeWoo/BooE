package org.hyeong.booe.property.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.exception.publicData.building.PublicDataAuthException;
import org.hyeong.booe.exception.publicData.building.PublicDataCommunicationException;
import org.hyeong.booe.property.dto.response.BrExposInfoResDto;
import org.hyeong.booe.property.dto.response.BrTitleInfoResDto;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConstructionApiClient {

    private final WebClient publicDataWebClient;
    private final PublicDataProperties properties;

    private static final String TITLE_INFO_PATH = "/1613000/BldRgstHubService/getBrTitleInfo"; // 표제부 조회
    private static final String EXPOS_DETAIL_PATH = "/1613000/BldRgstHubService/getBrExposPubuseAreaInfo"; // 전유부 조회
    private static final String QUERY_PARAMS = "?serviceKey=%s&sigunguCd=%s&bjdongCd=%s&bun=%s&ji=%s&_type=json";
    private static final String EXPOS_QUERY_PARAMS = QUERY_PARAMS + "&dongNm=%s&hoNm=%s";

    private static final String[][] SEARCH_SUFFIX_PATTERNS = {
            {"동", ""},   // 1. 퍼스트월드 스타일
            {"동", "호"}, // 2. 성지 리벨루스 스타일
            {"", "호"},   // 3. 기타 예외
            {"", ""}      // 4. 숫자만
    };

    // 1. 표제부 조회 (기존 유지)
    public Mono<BrTitleInfoResDto> fetchTitleSection(String sigunguCd, String bjdongCd, String bun, String ji) {
        String fullUrl = String.format(properties.getBaseUrl() + TITLE_INFO_PATH + QUERY_PARAMS,
                properties.getServiceKey(), sigunguCd, bjdongCd, bun, ji);

        log.info("Title Section Request URL: {}", fullUrl);

        return publicDataWebClient.get()
                .uri(URI.create(fullUrl))
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleApiError)
                .bodyToMono(BrTitleInfoResDto.class);
    }

    // 2. 전유부 상세 조회 - 4가지 패턴 재시도 로직
    public Mono<BrExposInfoResDto> fetchExposWithRetry(String sigunguCd, String bjdongCd, String bun, String ji, String dong, String ho) {
        return Flux.fromArray(SEARCH_SUFFIX_PATTERNS)
                .concatMap(s -> fetchExposDetail(sigunguCd, bjdongCd, bun, ji, dong + s[0], ho + s[1]))
                .filter(BrExposInfoResDto::hasData)
                .next()
                .switchIfEmpty(Mono.error(new PublicDataCommunicationException()));
    }

    private Mono<BrExposInfoResDto> fetchExposDetail(String sigunguCd, String bjdongCd, String bun, String ji, String dongNm, String hoNm) {
        String fullUrl = String.format(properties.getBaseUrl() + EXPOS_DETAIL_PATH + EXPOS_QUERY_PARAMS,
                properties.getServiceKey(), sigunguCd, bjdongCd, bun, ji,
                URLEncoder.encode(dongNm, StandardCharsets.UTF_8),
                URLEncoder.encode(hoNm, StandardCharsets.UTF_8));

        return publicDataWebClient.get()
                .uri(URI.create(fullUrl))
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleApiError)
                .bodyToMono(BrExposInfoResDto.class)
                .doOnNext(res -> log.info("🔎 API 시도: [{} , {}] -> 결과: {}건",
                        dongNm, hoNm, res.hasData() ? "성공" : "0"));
    }

    // 공통 에러 핸들링 (기존 유지)
    private Mono<Throwable> handleApiError(ClientResponse response) {
        return response.bodyToMono(String.class)
                .flatMap(body -> {
                    if (response.statusCode().value() == 401) {
                        return Mono.error(new PublicDataAuthException());
                    }
                    return Mono.error(new PublicDataCommunicationException());
                });
    }
}