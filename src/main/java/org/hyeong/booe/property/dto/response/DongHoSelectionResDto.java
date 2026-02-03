package org.hyeong.booe.property.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DongHoSelectionResDto {

    private List<DongUnit> dongs;

    @Getter
    @Builder
    public static class DongUnit {
        private String dongName;
        private List<String> hoList;
    }
}
