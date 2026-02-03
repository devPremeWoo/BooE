package org.hyeong.booe.property.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BldRgstQueryDto {

    private final String sigunguCd;   // 시군구 코드
    private final String bjdongCd;    // 법정동 코드
    private final String bun;         // 번
    private final String ji;          // 지


    public String toKey() {
        return sigunguCd + "-" + bjdongCd + "-" + bun + "-" + ji;
    }
}
