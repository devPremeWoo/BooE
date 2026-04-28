package org.hyeong.booe.auth.oauth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoReqDto {

    @NotBlank(message = "카카오 액세스 토큰은 필수입니다.")
    private String accessToken;
}
