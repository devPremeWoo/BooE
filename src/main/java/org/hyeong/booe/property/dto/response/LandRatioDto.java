package org.hyeong.booe.property.dto.response;

import java.util.List;

public record LandRatioDto(List<DongDto> dongs) {

    public record DongDto(
            String dong,
            List<FloorDto> floors
    ) {}

    public record FloorDto(
            String floor,
            List<HoDto> hos
    ) {}

    public record HoDto(
            String ho,
            String landRatio
    ) {}
}
