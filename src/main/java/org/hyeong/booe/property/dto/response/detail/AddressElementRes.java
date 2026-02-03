package org.hyeong.booe.property.dto.response.detail;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AddressElementRes {

    private List<String> elements;
    private int totalCount;
}
