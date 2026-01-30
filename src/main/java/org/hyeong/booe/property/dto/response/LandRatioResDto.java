package org.hyeong.booe.property.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LandRatioResDto {
    @JsonProperty("ldaregVOList")
    private LdaregDataWrapper wrapper;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LdaregDataWrapper {
        private String totalCount;
        private String pageNo;
        private String numOfRows;

        @JsonProperty("ldaregVOList")
        private List<LdaregItem> items;
    }

}