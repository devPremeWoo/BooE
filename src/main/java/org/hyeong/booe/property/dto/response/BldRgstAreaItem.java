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

    @JsonProperty("flrNo")
    private String flrNo;

    @JsonProperty("hoNm")
    private String hoNm;

    @JsonProperty("strctCdNm")
    private String strctCdNm;

    @JsonProperty("mainPurpsCdNm")
    private String mainPurpsCdNm;

    @JsonProperty("area")
    private Double area;

    @JsonProperty("exposPubuseGbCdNm")
    private String areaTypeName; // 전유 / 공용
}
