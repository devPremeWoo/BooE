package org.hyeong.booe.member.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class LocalLoginRequestDto {

    @NotBlank(message = "아이디는 필수 입력입니다.")
    @Pattern(regexp = "^[a-z0-9_-]{5,20}$", message = "아이디는 5~20자의 영문 소문자, 숫자, 특수기호(_),(-)만 사용할 수 있습니다.")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수 입력입니다.")
    @Size(min = 10, message = "비밀번호는 최소 10자 이상이어야 합니다.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{10,}$", message = "비밀번호는 영문자, 숫자, 특수문자를 모두 포함해야 합니다.")
    private String password;
}
