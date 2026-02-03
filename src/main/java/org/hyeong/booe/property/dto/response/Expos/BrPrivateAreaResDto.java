package org.hyeong.booe.property.dto.response.Expos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BrPrivateAreaResDto {

    @JsonProperty("response")
    private Response response;

    @Getter @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        @JsonProperty("body")
        private Body body;
    }

    @Getter @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        @JsonProperty("items")
        private Items items;
    }

    @Getter @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {
        @JsonProperty("item")
        private List<BrPrivateAreaItemDto> item;
    }

    /**
     * 전체 리스트 중 "전유" 면적 항목만 필터링하여 반환합니다.
     * 공용 면적은 여기서 원천 차단됩니다.
     */
    public List<BrPrivateAreaItemDto> getPrivateAreaItems() {
        if (response == null || response.body == null ||
                response.body.items == null || response.body.items.item == null) {
            return Collections.emptyList();
        }

        return response.body.items.item.stream()
                .filter(i -> "전유".equals(i.getAreaGbNm()))
                .collect(Collectors.toList());
    }
}