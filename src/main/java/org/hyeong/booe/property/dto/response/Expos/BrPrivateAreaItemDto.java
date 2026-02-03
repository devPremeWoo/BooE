package org.hyeong.booe.property.dto.response.Expos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BrPrivateAreaItemDto {

    @JsonProperty("exposPubuseGbCdNm")
    private String areaGbNm;         // 전유공용구분명 (필터링용: "전유"만 사용)

    @JsonProperty("strctCdNm")
    private String structureNm;      // 구조코드명 (예: 철근콘크리트구조)

    @JsonProperty("mainPrposCdNm")
    private String mainPurposeNm;    // 주용도코드명 (예: 아파트)

    @JsonProperty("area")
    private double area;             // 면적 (전유 항목일 경우 전용면적)
}