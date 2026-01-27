package org.hyeong.booe.property.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LandResDto {
    private final String pnu;
    private final String addressNm; // 법정동명
    private final String jimok;     // 지목
    private final Double landArea;  // Double로 형변환됨
    private final String bun;       // "0089" 형태로 가공됨
    private final String ji;        // "0025" 형태로 가공됨
}
