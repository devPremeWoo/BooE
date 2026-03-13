package org.hyeong.booe.property.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AreaReqDto {

    @JsonProperty("sigunguCd")
    private final String sigunguCd;

    @JsonProperty("bjdongCd")
    private final String bjdongCd;

    @JsonProperty("bun")
    private final String bun;

    @JsonProperty("ji")
    private final String ji;

    @JsonProperty("dongNm")
    private final String dongNm;

    @JsonProperty("hoNm")
    private final String hoNm;

    @Builder.Default
    @JsonProperty("numOfRows")
    private final int numOfRows = 30;
}
