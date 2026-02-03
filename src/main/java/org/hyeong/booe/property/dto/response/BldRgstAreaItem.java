package org.hyeong.booe.property.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BldRgstAreaItem {

    @JsonProperty("dongNm")
    private String dongNm;

    @JsonProperty("hoNm")
    private String hoNm;

    @JsonProperty("area")
    private Double area;

    @JsonProperty("areaGbCdNm")
    private String areaTypeName; // 전유 / 공용
}
