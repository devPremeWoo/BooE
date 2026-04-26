package org.hyeong.booe.member.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RefreshReqDto {

    @NotNull
    private String refreshToken;
}
