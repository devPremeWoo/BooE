package org.hyeong.booe.contract.dto.req;

import lombok.Builder;
import lombok.Getter;
import org.hyeong.booe.property.dto.BldRgstQueryDto;

@Getter
@Builder
public class PropertyUnitSelectionReqDto {

    private final String sigunguCd;   // 시군구 코드
    private final String bjdongCd;    // 법정동 코드
    private final String bun;         // 번
    private final String ji;          // 지


    public BldRgstQueryDto toQueryDto() {
        return BldRgstQueryDto.builder()
                .sigunguCd(sigunguCd)
                .bjdongCd(bjdongCd)
                .bun(bun)
                .ji(ji)
                .build();
    }
}
