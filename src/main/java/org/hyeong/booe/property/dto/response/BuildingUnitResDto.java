package org.hyeong.booe.property.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BuildingUnitResDto {

    private List<BuildingUnits> buildingUnits;

    @Getter @Builder
    public static class BuildingUnits {
        private String dongName;
        private List<FloorUnit> floors;
    }

    @Getter @Builder
    public static class FloorUnit {
        private String floorName;
        private List<HoUnit> hos;
    }

    @Getter @Builder
    public static class HoUnit {
        private String hoName;
        private Double area;      // 전용면적
        private String structure; // 구조코드명 (strctCdNm)
        private String purpose;   // 용도코드명 (mainPurpsCdNm)
    }
}
