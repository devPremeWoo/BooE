package org.hyeong.booe.property.api;

import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.contract.dto.req.PropertyInfoReqDto;
import org.hyeong.booe.exception.publicData.building.PublicDataCommunicationException;
import org.hyeong.booe.exception.publicData.land.LandDataNotFoundException;
import org.hyeong.booe.exception.server.DataParsingException;
import org.hyeong.booe.property.api.properties.VworldLadfrlProperties;
import org.hyeong.booe.property.dto.response.LandInfoResDto;
import org.hyeong.booe.property.dto.response.LandResDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class LandApiClient {

    private final VworldLadfrlProperties vworldLadfrlProperties;
    private final WebClient vworldLadfrlWebClient;

    public LandApiClient(
            VworldLadfrlProperties vworldLadfrlProperties,
            @Qualifier("vworldLadfrlWebClient") WebClient vworldLadfrlWebClient) {
        this.vworldLadfrlProperties = vworldLadfrlProperties;
        this.vworldLadfrlWebClient = vworldLadfrlWebClient;
    }

    public Mono<LandResDto> fetchLandAttributes(PropertyInfoReqDto reqDto) {
        return vworldLadfrlWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("key", vworldLadfrlProperties.getServiceKey())
                        .queryParam("pnu", reqDto.getPnu())
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        Mono.error(new PublicDataCommunicationException()))
                .bodyToMono(LandInfoResDto.class)
                .map(this::parseLandInfo);

    }

    private LandResDto parseLandInfo(LandInfoResDto response) {
        if (response.getWrapper() == null || response.getWrapper().getItems() == null || response.getWrapper().getItems().isEmpty()) {
            throw new LandDataNotFoundException();
        }

        try {
            LandInfoResDto.LandVo item = response.getWrapper().getItems().get(0);

            return LandResDto.builder()
                    .jimok(item.getJimokNm())
                    .landArea(item.getLandArea()) // String -> Double 변환
                    .build();

        } catch (Exception e) {
            log.error("Land data parsing failed: ", e);
            throw new DataParsingException();
        }
    }
}

