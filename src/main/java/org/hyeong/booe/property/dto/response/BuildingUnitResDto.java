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
        private String dongValue;   // 면적 API 재호출용 원본 값 (예: "1동", "" )
        private String dongLabel;   // 화면 표시용 (예: "1") — 클라이언트가 "동" 접미어를 붙임
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
        private String hoValue;     // 면적 API 재호출용 원본 값 (예: "702호", "4704")
        private String hoLabel;     // 화면 표시용 (예: "702") — 클라이언트가 "호" 접미어를 붙임
    }
}
