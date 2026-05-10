package org.hyeong.booe.property.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class LandResDto {
    private final String jimok;     // 지목
    private final String landArea;  // 면적
}
