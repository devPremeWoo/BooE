package org.hyeong.booe.property.service;

import org.hyeong.booe.property.dto.request.BuildingInfoReqDto;
import org.hyeong.booe.property.dto.response.BuildingUnitResDto;
import org.hyeong.booe.property.dto.response.UnitAreaResDto;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * 전유부 리스트 -> value/label -> 선택값으로 단건 면적 조회까지 서비스 경로 전체 통합 검증.
 * 동이 있는 단지(더샵)와 동이 없는 단일동 건물 두 케이스를 확인한다.
 */
@SpringBootTest(properties = {
        "booe.crypto.ci.hmac-key=dGVzdC1obWFjLWtleS1mb3ItdGVzdC1vbmx5LXVzZS1wYWRkaW5n",
        "booe.crypto.ci.aes-key=dGVzdC1hZXMta2V5LXdpdGgtMzItYnl0ZS1wYWRkaW5nLWFiY2RlZg==",
        "api.public-data.service-key=mqbVrTExhyHssEcrak%2FHzS46eOEzi7aQK9%2B8kmpctOiotwNIyx6ItkxNOCBLT7NBR294hM29Jw0g5LAkIAYU%2Bw%3D%3D",
        "api.public-data.timeout=50000"
})
class PropertyUnitSelectionFlowTest {

    private static final Logger log = LoggerFactory.getLogger(PropertyUnitSelectionFlowTest.class);
    private final StringBuilder report = new StringBuilder();

    @Autowired
    private PropertyUnitSelectionService service;

    @Test
    void flow() throws Exception {
        runPnu("2818510600100040001"); // 더샵퍼스트월드 (동 있음)
        runPnu("1121510700100210023"); // 단일동 (동 공백)
        Path out = Path.of("build", "flow-result.txt");
        Files.writeString(out, report.toString(), StandardCharsets.UTF_8);
        log.info("[FLOW] 결과 파일 -> {}", out.toAbsolutePath());
    }

    private void runPnu(String pnu) {
        line("==================================================");
        line("PNU=" + pnu);
        BuildingInfoReqDto query = BuildingInfoReqDto.fromPnu(pnu);

        BuildingUnitResDto list = service.getBuildingInfo(query).block(Duration.ofMinutes(2));
        if (list == null || list.getBuildingUnits().isEmpty()) {
            line("  리스트 비어있음 ❌");
            return;
        }
        BuildingUnitResDto.BuildingUnits dong = list.getBuildingUnits().get(0);
        BuildingUnitResDto.FloorUnit floor = dong.getFloors().get(0);
        BuildingUnitResDto.HoUnit ho = floor.getHos().get(0);
        line("  동 수=" + list.getBuildingUnits().size());
        line("  첫 동: value='" + dong.getDongValue() + "' label='" + dong.getDongLabel() + "'");
        line("  첫 층: " + floor.getFloorName());
        line("  첫 호: value='" + ho.getHoValue() + "' label='" + ho.getHoLabel() + "'");

        UnitAreaResDto area = service.getUnitArea(query, dong.getDongValue(), ho.getHoValue())
                .block(Duration.ofSeconds(30));
        line("  단건 면적 조회 -> 전용면적=" + area.getExclusiveArea()
                + " 구조=" + area.getStructure() + " 용도=" + area.getPurpose());
        line("  결론: " + (area.getExclusiveArea() != null ? "성공 ✅" : "실패 ❌"));
    }

    private void line(String s) {
        report.append(s).append('\n');
    }
}
