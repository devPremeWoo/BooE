package org.hyeong.booe.property.api;

import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.exception.publicData.building.PublicDataCommunicationException;
import org.hyeong.booe.property.dto.response.LandRatioResDto;
import org.hyeong.booe.property.dto.response.LdaregItem;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
public class LandRatioApiClient {

    private final VworldLdaregProperties vworldLdaregProperties;
    private final WebClient vworldLdaregWebClient;

    private final static int NUM_OF_ROWS = 1000;

    public LandRatioApiClient(
            VworldLdaregProperties vworldLdaregProperties,
            @Qualifier("vworldLdaregWebClient") WebClient vworldLdaregWebClient
    ) {
        this.vworldLdaregProperties = vworldLdaregProperties;
        this.vworldLdaregWebClient = vworldLdaregWebClient;
    }

    public Mono<LandRatioResDto> fetchPage(String pnu, int pageNo) {
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
                .bodyToMono(LandRatioResDto.class);
    }

    public Mono<List<LdaregItem>> fetchAllPages(String pnu) {
        return fetchPage(pnu, 1)
                .expand(response -> {
                    int totalCount = Integer.parseInt(response.getWrapper().getTotalCount());
                    int currentPage = Integer.parseInt(response.getWrapper().getPageNo());

                    if ((long) currentPage * NUM_OF_ROWS < totalCount) {
                        return fetchPage(pnu, currentPage + 1);
                    }
                    else {
                        return Mono.empty();
                    }
                })
                .flatMap(response -> Flux.fromIterable(response.getWrapper().getItems()))
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
}
