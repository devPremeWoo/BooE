package org.hyeong.booe.property.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.contract.dto.req.PropertyInfoReqDto;
import org.hyeong.booe.contract.dto.res.PropertyInfoResDto;
import org.hyeong.booe.property.dto.request.BuildingInfoReqDto;
import org.hyeong.booe.property.dto.response.BuildingUnitResDto;
import org.hyeong.booe.property.dto.response.LandRatioDto;
import org.hyeong.booe.property.dto.response.LandResDto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyCompositeService {
    private final LandInfoService landInfoService;
    private final PropertyUnitSelectionService propertyUnitSelectionService;

    public Mono<PropertyInfoResDto> getCompositePropertyInfo(PropertyInfoReqDto reqDto) {
        return Mono.zip(
                getSafeLandInfo(reqDto),
                getSafeLandRatioInfo(reqDto),
                getSafeBuildingInfo(reqDto)
        ).map(tuple -> PropertyInfoResDto.builder()
                .landResDto(tuple.getT1())
                .landRatioDto(tuple.getT2())
                .buildingUnitResDto(tuple.getT3())
                .build());
    }

    private Mono<LandResDto> getSafeLandInfo(PropertyInfoReqDto reqDto) {
        return landInfoService.getLandInfo(reqDto)
                .doOnError(e -> logError("LAND_INFO", reqDto.getPnu(), e))
                // 에러 발생 시 빈 값으로 복구 (사용자가 앱에서 직접 입력 가능하도록)
                .onErrorReturn(LandResDto.builder().jimok("").landArea("0").build());
    }

    /**
     * [대지권 비율] 에러 시 로그 기록 및 빈 객체 반환
     */
    private Mono<LandRatioDto> getSafeLandRatioInfo(PropertyInfoReqDto reqDto) {
        return landInfoService.getLandRatioInfo(reqDto)
                .doOnError(e -> logError("LAND_RATIO", reqDto.getPnu(), e))
                .onErrorReturn(new LandRatioDto(java.util.Collections.emptyList()));
    }

    /**
     * [건축물 상세] 에러 시 로그 기록 및 빈 객체 반환
     */
    private Mono<BuildingUnitResDto> getSafeBuildingInfo(PropertyInfoReqDto reqDto) {
        return propertyUnitSelectionService.getBuildingInfo(BuildingInfoReqDto.from(reqDto))
                .doOnError(e -> logError("BUILDING_INFO", reqDto.getPnu(), e))
                .onErrorReturn(BuildingUnitResDto.builder()
                        .buildingUnits(java.util.Collections.emptyList())
                        .build());
    }

    /**
     * 공통 에러 로깅 로직 (추후 모니터링 툴 연동 시 이 메서드만 수정)
     */
    private void logError(String domain, String pnu, Throwable e) {
        // e가 어떤 예외인지에 따라 파싱 문제인지 데이터 없음 문제인지 로그로 확인 가능
        log.error("[CHECK_ERROR][{}] PNU: {}, Reason: {}, ExceptionType: {}",
                domain, pnu, e.getMessage(), e.getClass().getSimpleName());
    }
}