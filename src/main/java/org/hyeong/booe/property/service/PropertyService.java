package org.hyeong.booe.property.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.property.api.ConstructionApiClient;
import org.hyeong.booe.property.dto.response.Expos.BrPrivateAreaItemDto;
import org.hyeong.booe.property.dto.response.Expos.ExposItemDto;
import org.hyeong.booe.property.dto.request.AreaReqDto;
import org.hyeong.booe.property.util.AddressPatternManager;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyService {

    private final ConstructionApiClient constructionApiClient;
    private final AddressPatternManager addressPatternManager;

    public Mono<AreaReqDto> prepareAreaRequest(String sigunguCd, String bjdongCd,
                                               String bun, String ji,
                                               String userDong, String userHo) {

        return constructionApiClient.fetchExposInfoSample(sigunguCd, bjdongCd, bun, ji, 100)
                .map(resDto -> {
                    List<ExposItemDto> items = resDto.getItemList();

                    return addressPatternManager.castToApiRequest(
                            items, sigunguCd, bjdongCd, bun, ji, userDong, userHo
                    );
                })
                .doOnNext(dto -> log.info("최종 조립된 요청 정보 - 동: {}, 호: {}", dto.getDongNm(), dto.getHoNm()))
                .doOnError(e -> log.error("패턴 분석 중 오류 발생: {}", e.getMessage()));
    }

    public Mono<Double> getPrivateAreaValue(AreaReqDto areaReqDto) {

        return constructionApiClient.fetchPrivateAreaInfo(areaReqDto)
                .map(areaResDto -> {
                    return areaResDto.getPrivateAreaItems().stream()
                            .mapToDouble(BrPrivateAreaItemDto::getArea)
                            .sum();
                })
                .doOnNext(total -> log.info("동: {}, 호: {} -> 전용면적 합계: {}㎡",
                        areaReqDto.getDongNm(), areaReqDto.getHoNm(), total))
                .doOnError(e -> log.error("면적 조회 실패: {}", e.getMessage()));
    }
}
