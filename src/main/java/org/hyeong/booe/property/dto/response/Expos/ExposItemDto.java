package org.hyeong.booe.property.dto.response.Expos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExposItemDto {

    @JsonProperty("dongNm")
    private String dongNm;

    @JsonProperty("hoNm")
    private String hoNm;

    @JsonProperty("mgmBldrgstPk")
    private String mgmBldrgstPk;

    @JsonProperty("flrNo")
    private int flrNo;
}