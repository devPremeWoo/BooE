package org.hyeong.booe.property.api;

import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.exception.publicData.building.PublicDataCommunicationException;
import org.hyeong.booe.property.api.properties.VworldLdaregProperties;
import org.hyeong.booe.property.dto.response.BldRgstAreaItem;
import org.hyeong.booe.property.dto.response.LandRatioResDto;
import org.hyeong.booe.property.dto.response.LdaregItem;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Component
@Slf4j
public class LandRatioApiClient {

    private final VworldLdaregProperties vworldLdaregProperties;
    private final WebClient vworldLdaregWebClient;

    private final static int NUM_OF_ROWS = 1000;
    private static final int CONCURRENCY = 10;

    public LandRatioApiClient(
            VworldLdaregProperties vworldLdaregProperties,
            @Qualifier("vworldLdaregWebClient") WebClient vworldLdaregWebClient
    ) {
        this.vworldLdaregProperties = vworldLdaregProperties;
        this.vworldLdaregWebClient = vworldLdaregWebClient;
    }

    public Mono<pageState> fetchPage(String pnu, int pageNo) {
        return vworldLdaregWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("key", vworldLdaregProperties.getServiceKey())
                        .queryParam("pnu", pnu)
                        .queryParam("format", "json")
                        .queryParam("numOfRows", NUM_OF_ROWS)
                        .queryParam("pageNo", pageNo)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        Mono.error(new PublicDataCommunicationException()))
                .bodyToMono(LandRatioResDto.class)
                .map(res -> {
                    var wrapper = res.getWrapper();
                    if (wrapper == null) {
                        return new pageState(pageNo, 0, List.of());
                    }

                    int total = (wrapper.getTotalCount() != null) ? Integer.parseInt(wrapper.getTotalCount()) : 0;
                    var items = (wrapper.getItems() != null) ? wrapper.getItems() : List.<LdaregItem>of();

                    return new pageState(pageNo, total, items);
                })
                .onErrorResume(e -> {
                    log.error("[Vworld API Error] page: {}, msg: {}", pageNo, e.getMessage());
                    return Mono.just(new pageState(pageNo, 0, List.of()));
                });
    }

    public Mono<List<LdaregItem>> fetchAllPages(String pnu) {

        return fetchPage(pnu, 1)
                .flatMapMany(firstPage -> {
                    int totalCount = firstPage.totalCount();
                    int totalPages = (int) Math.ceil((double) totalCount / NUM_OF_ROWS);
                    log.info("[Vworld] PNU: {}, 전체 데이터 수: {}, 전체 페이지: {}", pnu, totalCount, totalPages);

                    if (totalPages <= 1) {
                        return Flux.fromIterable(firstPage.items());
                    }

                    return Flux.range(1, totalPages)
                            .delayElements(Duration.ofMillis(30))
                            .flatMap(pageNo -> fetchPage(pnu, pageNo), CONCURRENCY)
                            .timeout(Duration.ofSeconds(5))
                            .retryWhen(
                                    Retry.backoff(3, Duration.ofMillis(500))
                            )
                            .flatMapIterable(pageState::items);
                })
                .collectList();
    }


    public Mono<String> getLandRatio(String pnu, String targetDong, String targetHo) {
        return fetchAllPages(pnu) // 모든 페이지 데이터(List<LdaregItem>) 확보
                .map(items -> items.stream()
                        .filter(item -> isMatching(item, targetDong, targetHo))
                        .map(LdaregItem::getLdaQotaRate)
                        .findFirst()
                        .orElse("0/0") // 데이터를 못 찾았을 때
                );
    }

    private boolean isMatching(LdaregItem item, String dong, String ho) {
        String userDong = dong.replaceAll("^0+", "").trim();
        String userHo = ho.replaceAll("^0+", "").trim();

        return item.getNormalizedDong().equals(userDong) && item.getNormalizedHo().equals(userHo);
    }

    private record pageState(int pageNo, int totalCount, List<LdaregItem> items) {
        boolean isLast() {
            return pageNo * NUM_OF_ROWS >= totalCount;
        }

        int nextPage() {
            return pageNo + 1;
        }
    }
}
