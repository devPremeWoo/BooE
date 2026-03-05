package org.hyeong.booe.contract.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.hyeong.booe.property.dto.response.BuildingUnitResDto;
import org.hyeong.booe.property.dto.response.LandRatioDto;
import org.hyeong.booe.property.dto.response.LandResDto;

@Getter
@Builder
@AllArgsConstructor
public class PropertyInfoResDto {

    private BuildingUnitResDto buildingUnitResDto;
    private LandResDto landResDto;
    private LandRatioDto landRatioDto;

}
