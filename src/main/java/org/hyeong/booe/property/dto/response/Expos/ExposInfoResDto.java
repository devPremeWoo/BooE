package org.hyeong.booe.property.dto.response.Expos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExposInfoResDto {

    @JsonProperty("response")
    private Response response;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        @JsonProperty("body")
        private Body body;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        @JsonProperty("items")
        private Items items;

        @JsonProperty("totalCount")
        private Integer totalCount;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {
        @JsonProperty("item")
        private List<ExposItemDto> item;
    }

    public List<ExposItemDto> getItemList() {
        if (response == null || response.body == null || response.body.items == null
                || response.body.items.item == null) {
            return Collections.emptyList();
        }

        return response.body.items.item;
    }

    public int getTotalCount() {
        if (response == null || response.body == null || response.body.totalCount == null) {
            return 0;
        }

        return response.body.totalCount;
    }
}