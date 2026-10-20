package org.hyeong.booe.property.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.exception.publicData.building.PublicDataAuthException;
import org.hyeong.booe.exception.publicData.building.PublicDataCommunicationException;
import org.hyeong.booe.property.api.properties.PublicDataProperties;
import org.hyeong.booe.property.dto.request.BuildingInfoReqDto;
import org.hyeong.booe.property.dto.response.BldRgstApiResDto;
import org.hyeong.booe.property.dto.response.BldRgstAreaItem;
import org.hyeong.booe.property.dto.response.Expos.ExposInfoResDto;
import org.hyeong.booe.property.dto.response.Expos.ExposItemDto;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.retry.Retry;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BldRgstApiClient {

    private static final int NUM_OF_ROWS = 100;
    private static final int CONCURRENCY = 30;
    private static final long PAGE_DELAY_MS = 50;
    private static final String PRIVATE_AREA_PATH = "/1613000/BldRgstHubService/getBrExposPubuseAreaInfo"; // 전유공용면적 api 조회
    private static final String EXPOS_INFO_PATH = "/1613000/BldRgstHubService/getBrExposInfo"; // 전유부 api 조회

    private final WebClient publicDataWebClient;
    private final PublicDataProperties properties;


    public Mono<List<BldRgstAreaItem>> fetchAllAreaItems(BuildingInfoReqDto query) {

        return fetchAreaPage(query, 1)
                .flatMapMany(firstPage -> {
                    int totalCount = firstPage.totalCount();
                    log.info(""+totalCount);
                    int totalPages =
                            (int) Math.ceil((double) totalCount / NUM_OF_ROWS);

                    return Flux.range(1, totalPages)
                            .delayElements(Duration.ofMillis(50))
                            .flatMap(pageNo -> fetchAreaPage(query, pageNo), CONCURRENCY)
                            .timeout(Duration.ofSeconds(5))
                            .retryWhen(
                                    Retry.backoff(3, Duration.ofMillis(500))
                            )
                            .flatMapIterable(AreaPageState::items);
                })
                .collectList();
    }


    public Mono<AreaPageState> fetchAreaPage(BuildingInfoReqDto query, int pageNo) {
        return publicDataWebClient.get()
                .uri(uriBuilder -> uriBuilder
                .path(PRIVATE_AREA_PATH)
                .queryParam("serviceKey", properties.getServiceKey())
                .queryParam("sigunguCd", query.getSigunguCd())
                .queryParam("bjdongCd", query.getBjdongCd())
                .queryParam("bun", query.getBun())
                .queryParam("ji", query.getJi())
                .queryParam("numOfRows", NUM_OF_ROWS)
                .queryParam("pageNo", pageNo)
                .queryParam("_type", "json")
                .build()
        )
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleApiError)
                .bodyToMono(BldRgstApiResDto.class)
                .map(res -> {
                    var body = res.getResponse().getBody();
                    var items = body.getItems() != null ? body.getItems().getItem() : List.<BldRgstAreaItem>of();

                    return new AreaPageState(pageNo, body.getTotalCount(), items);
                });
    }

    /**
     * 전유부(getBrExposInfo) 전체 조회.
     * 전유공용면적과 달리 공용 면적 행이 없어 세대 수 수준의 행만 반환되므로
     * 동/호 선택 리스트 생성에는 이 API 를 사용한다.
     */
    public Mono<List<ExposItemDto>> fetchAllExposItems(BuildingInfoReqDto query) {
        return fetchExposPage(query, 1)
                .flatMapMany(firstPage -> {
                    int totalPages = totalPages(firstPage.totalCount());
                    return Flux.range(1, totalPages)
                            .delayElements(Duration.ofMillis(PAGE_DELAY_MS))
                            .flatMap(pageNo -> fetchExposPage(query, pageNo), CONCURRENCY)
                            .timeout(Duration.ofSeconds(5))
                            .retryWhen(Retry.backoff(3, Duration.ofMillis(500)))
                            .flatMapIterable(ExposPageState::items);
                })
                .collectList();
    }

    private Mono<ExposPageState> fetchExposPage(BuildingInfoReqDto query, int pageNo) {
        return publicDataWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(EXPOS_INFO_PATH)
                        .queryParam("serviceKey", properties.getServiceKey())
                        .queryParam("sigunguCd", query.getSigunguCd())
                        .queryParam("bjdongCd", query.getBjdongCd())
                        .queryParam("bun", query.getBun())
                        .queryParam("ji", query.getJi())
                        .queryParam("numOfRows", NUM_OF_ROWS)
                        .queryParam("pageNo", pageNo)
                        .queryParam("_type", "json")
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleApiError)
                .bodyToMono(ExposInfoResDto.class)
                .map(res -> new ExposPageState(res.getTotalCount(), res.getItemList()));
    }

    /**
     * 선택된 단일 세대의 전유공용면적 조회.
     * publicDataWebClient 는 EncodingMode.NONE 이므로 한글 dongNm/hoNm 은 직접 인코딩한다.
     * 동이 없는 건물은 dongNm 이 공백으로 저장되는데, 공백 값을 그대로 보내면 0건이 나오므로
     * 비어있으면 파라미터 자체를 생략한다.
     */
    public Mono<List<BldRgstAreaItem>> fetchUnitArea(BuildingInfoReqDto query, String dongNm, String hoNm) {
        String encodedHo = encode(hoNm);
        String encodedDong = encodeDongOrNull(dongNm);

        return publicDataWebClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(PRIVATE_AREA_PATH)
                            .queryParam("serviceKey", properties.getServiceKey())
                            .queryParam("sigunguCd", query.getSigunguCd())
                            .queryParam("bjdongCd", query.getBjdongCd())
                            .queryParam("bun", query.getBun())
                            .queryParam("ji", query.getJi());
                    if (encodedDong != null) {
                        uriBuilder.queryParam("dongNm", encodedDong);
                    }
                    return uriBuilder
                            .queryParam("hoNm", encodedHo)
                            .queryParam("numOfRows", NUM_OF_ROWS)
                            .queryParam("pageNo", 1)
                            .queryParam("_type", "json")
                            .build();
                })
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleApiError)
                .bodyToMono(BldRgstApiResDto.class)
                .map(this::extractItems);
    }

    private List<BldRgstAreaItem> extractItems(BldRgstApiResDto res) {
        var body = res.getResponse().getBody();
        return body.getItems() != null ? body.getItems().getItem() : List.of();
    }

    private int totalPages(int totalCount) {
        return (int) Math.ceil((double) totalCount / NUM_OF_ROWS);
    }

    private String encodeDongOrNull(String dongNm) {
        if (dongNm == null || dongNm.trim().isEmpty()) {
            return null;
        }
        return encode(dongNm);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record ExposPageState(int totalCount, List<ExposItemDto> items) {}

    private Mono<Throwable> handleApiError(ClientResponse response) {
        return response.bodyToMono(String.class)
                .flatMap(body -> {
                    if (response.statusCode().value() == 401) {
                        return Mono.error(new PublicDataAuthException());
                    }
                    return Mono.error(new PublicDataCommunicationException());
                });
    }

    private record AreaPageState(int pageNo, int totalCount, List<BldRgstAreaItem> items) {
        boolean isLast() {
            return pageNo * NUM_OF_ROWS >= totalCount;
        }

        int nextPage() {
            return pageNo + 1;
        }
    }

    public Mono<List<BldRgstAreaItem>> fetchAllAreaItemsWithTiming(BuildingInfoReqDto query, String label) {
        return fetchAllAreaItems(query)
                .elapsed()
                .doOnNext(tuple ->
                        log.info("[{}] 전유공용면적 전체 조회 소요 시간 = {} ms",
                                label,
                                tuple.getT1())
                )
                .map(Tuple2::getT2);
    }

    /**
     * 동시성·딜레이 튜닝용. retryWhen 없이 raw 측정.
     */
    public Mono<List<BldRgstAreaItem>> fetchAllAreaItemsForTuning(
            BuildingInfoReqDto query, int concurrency, long delayMs) {

        return fetchAreaPage(query, 1)
                .flatMapMany(firstPage -> {
                    int totalCount = firstPage.totalCount();
                    int totalPages = (int) Math.ceil((double) totalCount / NUM_OF_ROWS);

                    return Flux.range(1, totalPages)
                            .delayElements(Duration.ofMillis(delayMs))
                            .flatMap(pageNo -> fetchAreaPage(query, pageNo), concurrency)
                            .timeout(Duration.ofSeconds(10))
                            .flatMapIterable(AreaPageState::items);
                })
                .collectList();
    }

}
