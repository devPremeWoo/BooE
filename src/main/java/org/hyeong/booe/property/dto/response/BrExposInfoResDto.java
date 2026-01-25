package org.hyeong.booe.property.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BrExposInfoResDto {
    private Response response;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Header header;
        private Body body;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Body {
        private Items items;
        private int numOfRows;
        private int pageNo;
        private int totalCount;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Items {
        private List<Item> item;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString // 로그 확인용
    public static class Item {
        private String dongNm;
        private String hoNm;

        // API 응답의 실제 키값은 'area'일 확률이 매우 높습니다.
        @JsonProperty("area")
        private Double exposArea;    // 전용 면적

        @JsonProperty("strctCdNm")
        private String strctCdNm;    // 구조

        @JsonProperty("mainPurpsCdNm")
        private String mainPurpsCdNm; // 용도

        // 전유부 API에서 대지권비율은 보통 이 필드입니다.
        @JsonProperty("prevatArea")
        private String prevatArea;
    }

    // 데이터 존재 여부 확인 편의 메서드
    public boolean hasData() {
        return response != null &&
                response.getBody() != null &&
                response.getBody().getTotalCount() > 0 &&
                response.getBody().getItems() != null;
    }
}