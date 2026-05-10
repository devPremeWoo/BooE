package org.hyeong.booe.property.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class BuildingUnitResDto {

    private List<BuildingUnits> buildingUnits;

    @Getter
    @Builder
    @NoArgsConstructor(force = true)
    @AllArgsConstructor
    public static class BuildingUnits {
        private String dongName;
        private List<FloorUnit> floors;
    }

    @Getter
    @Builder
    @NoArgsConstructor(force = true)
    @AllArgsConstructor
    public static class FloorUnit {
        private String floorName;
        private List<HoUnit> hos;
    }

    @Getter
    @Builder
    @NoArgsConstructor(force = true)
    @AllArgsConstructor
    public static class HoUnit {
        private String hoName;
        private Double area;
        private String structure;
        private String purpose;
    }
}
