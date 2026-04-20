package org.hyeong.booe.property.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LandResDto {
    private final String jimok;     // 지목
    private final String landArea;  // 면적
}
