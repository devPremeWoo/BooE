package org.hyeong.booe.property.api;

import lombok.RequiredArgsConstructor;
import org.hyeong.booe.exception.publicData.building.PublicDataAuthException;
import org.hyeong.booe.exception.publicData.building.PublicDataCommunicationException;
import org.hyeong.booe.property.api.properties.PublicDataProperties;
import org.hyeong.booe.property.dto.BldRgstQueryDto;
import org.hyeong.booe.property.dto.response.BldRgstApiResDto;
import org.hyeong.booe.property.dto.response.BldRgstAreaItem;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BldRgstApiClient {

    private static final int PAGE_SIZE = 100;
    private static final String PRIVATE_AREA_PATH = "/1613000/BldRgstHubService/getBrExposPubuseAreaInfo"; // 전유공용면적 api 조회

    private final WebClient publicDataWebClient;
    private final PublicDataProperties properties;


    public Mono<List<BldRgstAreaItem>> fetchAllAreaItems(BldRgstQueryDto query) {

        return fetchAreaPage(query, 1)
                .expand(state -> {
                    if (state.isLast()) {
                        return Mono.empty();
                    }
                    return fetchAreaPage(query, state.nextPage());
                })
                .flatMapIterable(AreaPageState::items)
                .collectList();
    }

    public Mono<AreaPageState> fetchAreaPage(BldRgstQueryDto query, int pageNo) {
        return publicDataWebClient.get()
                .uri(uriBuilder -> uriBuilder
                .path(PRIVATE_AREA_PATH)
                .queryParam("serviceKey", properties.getServiceKey())
                .queryParam("sigunguCd", query.getSigunguCd())
                .queryParam("bjdongCd", query.getBjdongCd())
                .queryParam("bun", query.getBun())
                .queryParam("ji", query.getJi())
                .queryParam("numOfRows", PAGE_SIZE)
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
            return pageNo * PAGE_SIZE >= totalCount;
        }

        int nextPage() {
            return pageNo + 1;
        }
    }
}
