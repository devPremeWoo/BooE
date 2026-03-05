package org.hyeong.booe.property.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.property.api.BldRgstApiClient;
import org.hyeong.booe.property.dto.BuildingInfoReqDto;
import org.hyeong.booe.property.dto.UnitDetail;
import org.hyeong.booe.property.dto.response.BldRgstAreaItem;
import org.hyeong.booe.property.dto.response.BuildingUnitResDto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyUnitSelectionService {

    private final BldRgstApiClient apiClient;

    public Mono<BuildingUnitResDto> getBuildingInfo(BuildingInfoReqDto queryDto) {
        return apiClient.fetchAllAreaItems(queryDto)
                .doOnNext(items -> {
                    // 리스트가 비어있지 않다면 첫 번째 아이템의 원본 값을 출력
                    if (items != null && !items.isEmpty()) {
                        BldRgstAreaItem first = items.get(0);
                        log.info("[DEBUG] 첫 번째 데이터 확인 - 동: {}, 층: {}, 호: {}, 면적: {}, 구분: {}",
                                first.getDongNm(), first.getFlrNo(), first.getHoNm(), first.getArea(), first.getAreaTypeName());
                        log.info("[DEBUG] 전체 아이템 개수: {}", items.size());
                    } else {
                        log.warn("[DEBUG] API로부터 받은 데이터가 비어있습니다.");
                    }
                })
                .map(this::groupItem)
                .map(this::toResponseDto);
    }

    private Map<String, Map<String, Map<String, UnitDetail>>> groupItem(List<BldRgstAreaItem> items) {

        Map<String, Map<String, Map<String, UnitDetail>>> result = new HashMap<>();

        for (BldRgstAreaItem item : items) {
            if (!isExclusive(item)) continue;

            String dong = normalizeDong(item.getDongNm());
            String floor = item.getFlrNo();
            String ho = normalizeHo(item.getHoNm());

            if (dong == null || floor == null || ho == null) continue;

            UnitDetail detail = new UnitDetail(
                    item.getStrctCdNm(),
                    item.getMainPurpsCdNm(),
                    item.getArea()
            );

            result
                    .computeIfAbsent(dong, d -> new HashMap<>())
                    .computeIfAbsent(floor, f -> new HashMap<>())
                    .putIfAbsent(ho, detail);
        }

        return result;
    }

    private boolean isExclusive(BldRgstAreaItem item) {

        return item.getAreaTypeName() != null && item.getAreaTypeName().trim().equals("전유");
    }

    /**
     * 동 정규화: 101, 제101동, 101동 → 101동
     */
    private String normalizeDong(String dongNm) {
        if (dongNm == null) return null;
        String trimmed = dongNm.trim();
        String number = trimmed.replaceAll("[^0-9]", "");
        if (!number.isEmpty()) {
            return number + "동";
        }
        if (trimmed.endsWith("동")) {
            return trimmed;
        }

        return trimmed + "동";
    }

    /**
     * 호 정규화: 1003호 → 1003
     */
    private String normalizeHo(String hoNm) {
        if (hoNm == null) return null;
        return hoNm.replaceAll("[^0-9]", "");
    }

    /**
     * [변환 로직] 전체 Map -> DongHoSelectionResDto
     */
    private BuildingUnitResDto toResponseDto(Map<String, Map<String, Map<String, UnitDetail>>> grouped) {
        List<BuildingUnitResDto.BuildingUnits> buildingUnits = grouped.entrySet().stream()
                .sorted((e1, e2) -> compareNumericString(e1.getKey(), e2.getKey()))
                .map(this::toDongUnit) // 동 단위 변환 메서드 호출
                .toList();

        return BuildingUnitResDto.builder()
                .buildingUnits(buildingUnits)
                .build();
    }

    /**
     * [변환 로직] 동 Map -> DongUnit
     */
    private BuildingUnitResDto.BuildingUnits toDongUnit(
            Map.Entry<String, Map<String, Map<String, UnitDetail>>> dongEntry) {

        List<BuildingUnitResDto.FloorUnit> floorUnits = dongEntry.getValue().entrySet().stream()
                .sorted((e1, e2) -> compareNumericString(e1.getKey(), e2.getKey()))
                .map(this::toFloorUnit) // 층 단위 변환 메서드 호출
                .toList();

        return BuildingUnitResDto.BuildingUnits.builder()
                .dongName(dongEntry.getKey())
                .floors(floorUnits)
                .build();
    }

    /**
     * [변환 로직] 층 Map -> FloorUnit
     */
    private BuildingUnitResDto.FloorUnit toFloorUnit(
            Map.Entry<String, Map<String, UnitDetail>> floorEntry) {

        List<BuildingUnitResDto.HoUnit> hoUnits = floorEntry.getValue().entrySet().stream()
                .sorted((e1, e2) -> compareNumericString(e1.getKey(), e2.getKey()))
                .map(this::toHoUnit) // 호 단위 변환 메서드 호출
                .toList();

        return BuildingUnitResDto.FloorUnit.builder()
                .floorName(floorEntry.getKey())
                .hos(hoUnits)
                .build();
    }

    /**
     * [변환 로직] 상세 정보 -> HoUnit
     */
    private BuildingUnitResDto.HoUnit toHoUnit(Map.Entry<String, UnitDetail> hoEntry) {
        UnitDetail detail = hoEntry.getValue();
        return BuildingUnitResDto.HoUnit.builder()
                .hoName(hoEntry.getKey())
                .area(detail.exclusiveArea())
                .structure(detail.structure())
                .purpose(detail.purpose())
                .build();
    }

    private int compareNumericString(String a, String b) {
        try {
            // "101동"이나 "1층" 같은 경우 숫자만 발라내서 비교
            int numA = Integer.parseInt(a.replaceAll("[^0-9]", ""));
            int numB = Integer.parseInt(b.replaceAll("[^0-9]", ""));
            return Integer.compare(numA, numB);
        } catch (Exception e) {
            return a.compareTo(b);
        }
    }

    public Mono<BuildingUnitResDto> getSelectableDongHoWithTiming(
            BuildingInfoReqDto queryDto
    ) {
        return Mono.defer(() -> {
            long start = System.nanoTime();

            return getBuildingInfo(queryDto)
                    .doFinally(signal -> {
                        long end = System.nanoTime();
                        long elapsedMs = (end - start) / 1_000_000;
                        log.info("[PERF] getSelectableDongHo total = {} ms (signal={})",
                                elapsedMs, signal);
                    });
        });
    }

}
