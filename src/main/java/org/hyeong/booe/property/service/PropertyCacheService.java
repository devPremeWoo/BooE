package org.hyeong.booe.property.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.property.dto.response.BuildingUnitResDto;
import org.hyeong.booe.property.dto.response.LandRatioDto;
import org.hyeong.booe.property.dto.response.LandResDto;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class PropertyCacheService {

    private static final String LAND_INFO_KEY_PREFIX = "property:land:";
    private static final String LAND_RATIO_KEY_PREFIX = "property:land-ratio:";
    private static final String BUILDING_INFO_KEY_PREFIX = "property:building:";

    private static final long LAND_INFO_TTL_HOURS = 24 * 7;
    private static final long LAND_RATIO_TTL_HOURS = 24 * 7;
    private static final long BUILDING_INFO_TTL_HOURS = 24 * 7;

    private final RedisTemplate<String, Object> redisTemplate;

    public LandResDto findLandInfo(String pnu) {
        return findCached(LAND_INFO_KEY_PREFIX + pnu, LandResDto.class);
    }

    public void cacheLandInfo(String pnu, LandResDto value) {
        cache(LAND_INFO_KEY_PREFIX + pnu, value, LAND_INFO_TTL_HOURS);
    }

    public LandRatioDto findLandRatio(String pnu) {
        return findCached(LAND_RATIO_KEY_PREFIX + pnu, LandRatioDto.class);
    }

    public void cacheLandRatio(String pnu, LandRatioDto value) {
        cache(LAND_RATIO_KEY_PREFIX + pnu, value, LAND_RATIO_TTL_HOURS);
    }

    public BuildingUnitResDto findBuildingInfo(String key) {
        return findCached(BUILDING_INFO_KEY_PREFIX + key, BuildingUnitResDto.class);
    }

    public void cacheBuildingInfo(String key, BuildingUnitResDto value) {
        cache(BUILDING_INFO_KEY_PREFIX + key, value, BUILDING_INFO_TTL_HOURS);
    }

    @SuppressWarnings("unchecked")
    private <T> T findCached(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return type.isInstance(value) ? (T) value : null;
        } catch (Exception e) {
            log.warn("[Cache] 역직렬화 실패 → 깨진 캐시 삭제. key={}, reason={}", key, e.getMessage());
            redisTemplate.delete(key);
            return null;
        }
    }

    private void cache(String key, Object value, long ttlHours) {
        redisTemplate.opsForValue().set(key, value, ttlHours, TimeUnit.HOURS);
    }
}
