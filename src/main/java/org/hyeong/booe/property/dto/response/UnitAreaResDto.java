package org.hyeong.booe.property.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 사용자가 동/호를 선택한 뒤 단건 조회한 전유부 면적 정보.
 */
@Getter
@Builder
public class UnitAreaResDto {

    private final Double exclusiveArea; // 전용면적
    private final String structure;     // 구조
    private final String purpose;       // 용도
}
