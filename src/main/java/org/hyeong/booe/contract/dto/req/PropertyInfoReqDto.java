package org.hyeong.booe.contract.dto.req;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
public class PropertyInfoReqDto {

    @NotBlank(message = "PNU 코드는 필수 입력 항목입니다.")
    @Size(min = 19, max = 19, message = "PNU 코드는 반드시 19자리여야 합니다.")
    @Pattern(regexp = "^[0-9]*$", message = "PNU 코드는 숫자만 포함할 수 있습니다.")
    private String pnu;
}
