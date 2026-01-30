package org.hyeong.booe.property.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LdaregItem {
    @JsonProperty("buldDongNm")
    private String dongNm;

    @JsonProperty("buldHoNm")
    private String hoNm;

    @JsonProperty("buldNm")
    private String buildingName;

    @JsonProperty("ldaQotaRate")
    private String ldaQotaRate; // 대지권 비율 (예: 45.2/3580.5)

    public String getNormalizedDong() {
        return dongNm == null ? "" : dongNm.replaceAll("^0+", "").trim();
    }

    public String getNormalizedHo() {
        return hoNm == null ? "" : hoNm.replaceAll("^0+", "").trim();
    }
}
