package org.hyeong.booe.verification.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class PhoneVerifyRequestDto {

    @NotBlank(message = "휴대폰 번호는 필수입니다.")
    @Pattern(regexp = "^01(?:0|1|[6-9])\\d{7,8}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
    private String phoneNum;
}