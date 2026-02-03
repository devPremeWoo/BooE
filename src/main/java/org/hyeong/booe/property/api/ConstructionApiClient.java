package org.hyeong.booe.property.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.exception.publicData.building.PublicDataAuthException;
import org.hyeong.booe.exception.publicData.building.PublicDataCommunicationException;
import org.hyeong.booe.property.api.properties.PublicDataProperties;
import org.hyeong.booe.property.dto.request.AreaReqDto;
import org.hyeong.booe.property.dto.response.Expos.BrPrivateAreaResDto;
import org.hyeong.booe.property.dto.response.Expos.ExposInfoResDto;
import org.hyeong.booe.property.dto.response.BrTitleInfoResDto;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;


@Component
@RequiredArgsConstructor
@Slf4j
public class ConstructionApiClient {

    private final WebClient publicDataWebClient;
    private final PublicDataProperties properties;

    private static final String TITLE_INFO_PATH = "/1613000/BldRgstHubService/getBrTitleInfo"; // 표제부 조회
    private static final String EXPOS_INFO_PATH = "/1613000/BldRgstHubService/getBrExposInfo"; // 전유부 api 조회
    private static final String PRIVATE_AREA_PATH = "/1613000/BldRgstHubService/getBrExposPubuseAreaInfo"; // 전유공용면적 api 조회
    private static final String QUERY_PARAMS = "?serviceKey=%s&sigunguCd=%s&bjdongCd=%s&bun=%s&ji=%s&_type=json";


    // 동, 호수 패턴 확인 위한 전유부 api
    public Mono<ExposInfoResDto> fetchExposInfoSample (String sigunguCd, String bjdongCd, String bun, String ji, int numOfRow) {

        return publicDataWebClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path(EXPOS_INFO_PATH)
                                .queryParam("serviceKey", properties.getServiceKey())
                                .queryParam("sigunguCd", sigunguCd)
                                .queryParam("bjdongCd", bjdongCd)
                                .queryParam("bun", bun)
                                .queryParam("ji", ji)
                                .queryParam("numOfRows", numOfRow)
                                .queryParam("pageNo", "1")
                                .queryParam("_type", "json")
                                .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, res ->
                        res.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new RuntimeException("전유부 API 호출 실패: " + errorBody))))
                .bodyToMono(ExposInfoResDto.class);
    }

    // 1. 표제부 조회
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

    // 정제된 동, 호수를 적용한 전용면적 조회 api
    public Mono<BrPrivateAreaResDto> fetchPrivateAreaInfo(AreaReqDto requestDto) {
        return publicDataWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(PRIVATE_AREA_PATH)
                        .queryParam("serviceKey", properties.getServiceKey())
                        .queryParam("sigunguCd", requestDto.getSigunguCd())
                        .queryParam("bjdongCd", requestDto.getBjdongCd())
                        .queryParam("bun", requestDto.getBun())
                        .queryParam("ji", requestDto.getJi())
                        .queryParam("dongNm", requestDto.getDongNm())
                        .queryParam("hoNm", requestDto.getHoNm())
                        .queryParam("numOfRows", requestDto.getNumOfRows()) // 여기서 30 사용
                        .queryParam("pageNo", "1")
                        .queryParam("_type", "json")
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, res ->
                        res.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new RuntimeException("면적 API 호출 실패: " + errorBody))))
                .bodyToMono(BrPrivateAreaResDto.class);
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