package org.hyeong.booe.property.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LandInfoResDto {
    @JsonProperty("ladfrlVOList")
    private LandWrapper wrapper;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LandWrapper {
        @JsonProperty("ladfrlVOList")
        private List<LandVo> items;
        private String totalCount;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LandVo {

        @JsonProperty("lndpclAr")
        private String landArea; // 면적 (JSON에서 "376.9" 문자열로 옴)

        @JsonProperty("lndcgrCodeNm")
        private String jimokNm;  // 지목명 (예: "대")

//        private String pnu;
//
//        @JsonProperty("ldCodeNm")
//        private String addressNm; // 법정동명
//
//        @JsonProperty("mnnmSlno") // 지번(본번-부번) 필드 추가
//        private String mnnmSlno;
    }
}