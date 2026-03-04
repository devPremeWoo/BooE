package org.hyeong.booe.contract.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PnuDetailDto {

    private final String sigunguCd;
    private final String bjdongCd;
    private final String bun;
    private final String ji;

    // PNU를 받아서 바로 이 객체를 생성
    public static PnuDetailDto fromPnu(String pnu) {
        return PnuDetailDto.builder()
                .sigunguCd(pnu.substring(0, 5))
                .bjdongCd(pnu.substring(5, 10))
                .bun(pnu.substring(11, 15))
                .ji(pnu.substring(15, 19))
                .build();
    }

}
