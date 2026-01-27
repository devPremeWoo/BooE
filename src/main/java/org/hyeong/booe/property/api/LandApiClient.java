package org.hyeong.booe.property.api;

import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.exception.publicData.building.PublicDataCommunicationException;
import org.hyeong.booe.exception.publicData.land.LandDataNotFoundException;
import org.hyeong.booe.exception.server.DataParsingException;
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

    private final VworldProperties vworldProperties;
    private final WebClient vworldWebClient;

    public LandApiClient(
            VworldProperties vworldProperties,
            @Qualifier("vworldWebClient") WebClient vworldWebClient) {
        this.vworldProperties = vworldProperties;
        this.vworldWebClient = vworldWebClient;
    }

    public Mono<LandResDto> fetchLandAttributes(String pnu) {
        return vworldWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("key", vworldProperties.getServiceKey())
                        .queryParam("pnu", pnu)
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        Mono.error(new PublicDataCommunicationException()))
                .bodyToMono(LandInfoResDto.class)
                .map(this::parseLandInfo);

    }

    private LandResDto parseLandInfo(LandInfoResDto response) {
        if (response.getContainer() == null || response.getContainer().getItems() == null || response.getContainer().getItems().isEmpty()) {
            throw new LandDataNotFoundException();
        }

        try {
            LandInfoResDto.LandVo item = response.getContainer().getItems().get(0);

            String mnnmSlno = item.getMnnmSlno();
            String[] parts = mnnmSlno.split("-");
            String bun = String.format("%04d", Integer.parseInt(parts[0]));
            String ji = (parts.length > 1) ? String.format("%04d", Integer.parseInt(parts[1])) : "0000";

            return LandResDto.builder()
                    .pnu(item.getPnu())
                    .addressNm(item.getAddressNm())
                    .jimok(item.getJimokNm())
                    .landArea(Double.valueOf(item.getLandArea())) // String -> Double 변환
                    .bun(bun)
                    .ji(ji)
                    .build();

        } catch (Exception e) {
            log.error("Land data parsing failed: ", e);
            throw new DataParsingException();
        }
    }
}

