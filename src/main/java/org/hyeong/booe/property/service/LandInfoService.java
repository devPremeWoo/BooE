package org.hyeong.booe.property.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.contract.dto.req.PropertyInfoReqDto;
import org.hyeong.booe.property.api.LandApiClient;
import org.hyeong.booe.property.api.LandRatioApiClient;
import org.hyeong.booe.property.dto.response.LandRatioDto;
import org.hyeong.booe.property.dto.response.LandResDto;
import org.hyeong.booe.property.dto.response.LdaregItem;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LandInfoService {

    private final LandApiClient landApiClient;
    private final LandRatioApiClient landRatioApiClient;
    private final PropertyCacheService cache;
    private final InFlightRequestRegistry inflight;

    public Mono<LandResDto> getLandInfo(PropertyInfoReqDto reqDto) {
        String pnu = reqDto.getPnu();

        LandResDto cached = cache.findLandInfo(pnu);
        if (cached != null) {
            log.debug("[LandInfo] cache HIT - pnu={}", pnu);
            return Mono.just(cached);
        }

        log.debug("[LandInfo] cache MISS - pnu={}", pnu);
        return inflight.dedupe("land:" + pnu, () -> landApiClient.fetchLandAttributes(reqDto)
                .doOnNext(result -> cache.cacheLandInfo(pnu, result)));
    }

    public Mono<LandRatioDto> getLandRatioInfo(PropertyInfoReqDto reqDto) {
        String pnu = reqDto.getPnu();

        LandRatioDto cached = cache.findLandRatio(pnu);
        if (cached != null) {
            log.debug("[LandRatio] cache HIT - pnu={}", pnu);
            return Mono.just(cached);
        }

        log.debug("[LandRatio] cache MISS - pnu={}", pnu);
        return inflight.dedupe("land-ratio:" + pnu, () -> landRatioApiClient.fetchAllPages(pnu)
                .map(this::groupItems)
                .map(this::toLandRatioDto)
                .doOnNext(result -> cache.cacheLandRatio(pnu, result)));
    }

    private LandRatioDto toLandRatioDto(
            Map<String, Map<String, Map<String, String>>> grouped) {

        List<LandRatioDto.DongDto> dongDtos =
                grouped.entrySet().stream()
                        .sorted((e1, e2) -> compareNumericString(e1.getKey(), e2.getKey()))
                        .map(this::toDongDto)
                        .toList();

        return new LandRatioDto(dongDtos);
    }

    private LandRatioDto.DongDto toDongDto(
            Map.Entry<String, Map<String, Map<String, String>>> dongEntry) {

        List<LandRatioDto.FloorDto> floorDtos =
                dongEntry.getValue().entrySet().stream()
                        .sorted((e1, e2) -> compareNumericString(e1.getKey(), e2.getKey()))
                        .map(this::toFloorDto)
                        .toList();

        return new LandRatioDto.DongDto(
                dongEntry.getKey(),
                floorDtos
        );
    }

    private LandRatioDto.FloorDto toFloorDto(
            Map.Entry<String, Map<String, String>> floorEntry) {

        List<LandRatioDto.HoDto> hoDtos =
                floorEntry.getValue().entrySet().stream()
                        .sorted((e1, e2) -> compareNumericString(e1.getKey(), e2.getKey()))
                        .map(e -> new LandRatioDto.HoDto(
                                e.getKey(),
                                e.getValue()
                        ))
                        .toList();

        return new LandRatioDto.FloorDto(
                floorEntry.getKey(),
                hoDtos
        );
    }

    private Map<String, Map<String, Map<String, String>>> groupItems(List<LdaregItem> items) {

        Map<String, Map<String, Map<String, String>>> result = new HashMap<>();

        for (LdaregItem item : items) {

            String dong = item.getDongNm();
            String floor = item.getFloorNm();
            String ho = item.getHoNm();
            String ratio = item.getLdaQotaRate();

            if (!result.containsKey(dong)) result.put(dong, new HashMap<>());

            Map<String, Map<String, String>> floorMap = result.get(dong);

            if (!floorMap.containsKey(floor)) floorMap.put(floor, new HashMap<>());

            Map<String, String> hoMap = floorMap.get(floor);

            if (!hoMap.containsKey(ho)) {
                hoMap.put(ho, ratio);
            }
        }

        return result;
    }

    private int compareNumericString(String a, String b) {
        try {
            return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
        } catch (NumberFormatException e) {
            return a.compareTo(b);
        }
    }
}
