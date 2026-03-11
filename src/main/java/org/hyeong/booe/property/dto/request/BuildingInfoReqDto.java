package org.hyeong.booe.property.dto.request;

import lombok.Builder;
import lombok.Getter;
import org.hyeong.booe.contract.dto.req.PropertyInfoReqDto;

@Getter
@Builder
public class BuildingInfoReqDto {

    private final String sigunguCd;   // 시군구 코드
    private final String bjdongCd;    // 법정동 코드
    private final String bun;         // 번
    private final String ji;          // 지


    public String toKey() {
        return sigunguCd + "-" + bjdongCd + "-" + bun + "-" + ji;
    }

    public static BuildingInfoReqDto from(PropertyInfoReqDto reqDto) {
        String pnu = reqDto.getPnu();
        return BuildingInfoReqDto.builder()
                .sigunguCd(pnu.substring(0, 5))
                .bjdongCd(pnu.substring(5, 10))
                .bun(pnu.substring(11, 15))
                .ji(pnu.substring(15, 19))
                .build();
    }
}
