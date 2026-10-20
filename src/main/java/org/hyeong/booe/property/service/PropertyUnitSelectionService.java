package org.hyeong.booe.property.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.exception.publicData.building.ExposInfoNotFoundException;
import org.hyeong.booe.property.api.BldRgstApiClient;
import org.hyeong.booe.property.dto.request.BuildingInfoReqDto;
import org.hyeong.booe.property.dto.response.BldRgstAreaItem;
import org.hyeong.booe.property.dto.response.BuildingUnitResDto;
import org.hyeong.booe.property.dto.response.Expos.ExposItemDto;
import org.hyeong.booe.property.dto.response.UnitAreaResDto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyUnitSelectionService {

    private final BldRgstApiClient apiClient;
    private final PropertyCacheService cache;
    private final InFlightRequestRegistry inflight;

    /**
     * 동/호 선택 리스트 조회.
     * 면적 행까지 폭증하는 전유공용면적 대신 전유부(세대 단위) API 를 사용한다.
     */
    public Mono<BuildingUnitResDto> getBuildingInfo(BuildingInfoReqDto queryDto) {
        String key = queryDto.toKey();

        BuildingUnitResDto cached = cache.findBuildingInfo(key);
        if (cached != null) {
            log.debug("[BuildingInfo] cache HIT - key={}", key);
            return Mono.just(cached);
        }

        log.debug("[BuildingInfo] cache MISS - key={}", key);
        return inflight.dedupe("building:" + key, () -> apiClient.fetchAllExposItems(queryDto)
                .doOnNext(items -> log.info("[BuildingInfo] 전유부 세대 수 = {}", items.size()))
                .map(this::toResponseDto)
                .doOnNext(result -> cache.cacheBuildingInfo(key, result)));
    }

    /**
     * 사용자가 선택한 단일 세대의 전용면적 조회.
     * dongNm/hoNm 은 리스트 응답의 원본 값(dongValue/hoValue)을 그대로 받는다.
     */
    public Mono<UnitAreaResDto> getUnitArea(BuildingInfoReqDto queryDto, String dongNm, String hoNm) {
        return apiClient.fetchUnitArea(queryDto, dongNm, hoNm)
                .map(this::toUnitArea);
    }

    private UnitAreaResDto toUnitArea(List<BldRgstAreaItem> items) {
        return items.stream()
                .filter(this::isExclusive)
                .findFirst()
                .map(this::buildUnitArea)
                .orElseThrow(ExposInfoNotFoundException::new);
    }

    private UnitAreaResDto buildUnitArea(BldRgstAreaItem item) {
        return UnitAreaResDto.builder()
                .exclusiveArea(item.getArea())
                .structure(item.getStrctCdNm())
                .purpose(item.getMainPurpsCdNm())
                .build();
    }

    private boolean isExclusive(BldRgstAreaItem item) {
        return item.getAreaTypeName() != null && item.getAreaTypeName().trim().equals("전유");
    }

    /**
     * 전유부 아이템들을 동 -> 층 -> 호 구조로 그룹핑한다. (호는 원본 값 그대로 보관)
     */
    private Map<String, Map<String, Set<String>>> groupItems(List<ExposItemDto> items) {
        Map<String, Map<String, Set<String>>> grouped = new HashMap<>();

        for (ExposItemDto item : items) {
            String dong = nullToEmpty(item.getDongNm());
            String floor = String.valueOf(item.getFlrNo());
            String ho = nullToEmpty(item.getHoNm());

            grouped.computeIfAbsent(dong, d -> new HashMap<>())
                    .computeIfAbsent(floor, f -> new HashSet<>())
                    .add(ho);
        }

        return grouped;
    }

    private BuildingUnitResDto toResponseDto(List<ExposItemDto> items) {
        List<BuildingUnitResDto.BuildingUnits> buildingUnits = groupItems(items).entrySet().stream()
                .sorted((e1, e2) -> compareNumericString(e1.getKey(), e2.getKey()))
                .map(this::toDongUnit)
                .toList();

        return BuildingUnitResDto.builder()
                .buildingUnits(buildingUnits)
                .build();
    }

    private BuildingUnitResDto.BuildingUnits toDongUnit(Map.Entry<String, Map<String, Set<String>>> dongEntry) {
        List<BuildingUnitResDto.FloorUnit> floorUnits = dongEntry.getValue().entrySet().stream()
                .sorted((e1, e2) -> compareNumericString(e1.getKey(), e2.getKey()))
                .map(this::toFloorUnit)
                .toList();

        return BuildingUnitResDto.BuildingUnits.builder()
                .dongValue(dongEntry.getKey())
                .dongLabel(stripSuffix(dongEntry.getKey(), "동"))
                .floors(floorUnits)
                .build();
    }

    private BuildingUnitResDto.FloorUnit toFloorUnit(Map.Entry<String, Set<String>> floorEntry) {
        List<BuildingUnitResDto.HoUnit> hoUnits = floorEntry.getValue().stream()
                .sorted(this::compareNumericString)
                .map(this::toHoUnit)
                .toList();

        return BuildingUnitResDto.FloorUnit.builder()
                .floorName(floorEntry.getKey())
                .hos(hoUnits)
                .build();
    }

    private BuildingUnitResDto.HoUnit toHoUnit(String hoRaw) {
        return BuildingUnitResDto.HoUnit.builder()
                .hoValue(hoRaw)
                .hoLabel(stripSuffix(hoRaw, "호"))
                .build();
    }

    /**
     * 화면 표시값: 꼬리의 "동"/"호" 접미어만 제거한다.
     * 숫자만 추출하면 "에이치동"·"상가1동" 같은 케이스가 깨지므로 접미어만 잘라낸다.
     */
    private String stripSuffix(String raw, String suffix) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.endsWith(suffix)) {
            return trimmed.substring(0, trimmed.length() - suffix.length());
        }
        return trimmed;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private int compareNumericString(String a, String b) {
        try {
            int numA = Integer.parseInt(a.replaceAll("[^0-9]", ""));
            int numB = Integer.parseInt(b.replaceAll("[^0-9]", ""));
            return Integer.compare(numA, numB);
        } catch (Exception e) {
            return a.compareTo(b);
        }
    }

    public Mono<BuildingUnitResDto> getSelectableDongHoWithTiming(BuildingInfoReqDto queryDto) {
        return Mono.defer(() -> {
            long start = System.nanoTime();

            return getBuildingInfo(queryDto)
                    .doFinally(signal -> {
                        long end = System.nanoTime();
                        long elapsedMs = (end - start) / 1_000_000;
                        log.info("[PERF] getSelectableDongHo total = {} ms (signal={})", elapsedMs, signal);
                    });
        });
    }

}
