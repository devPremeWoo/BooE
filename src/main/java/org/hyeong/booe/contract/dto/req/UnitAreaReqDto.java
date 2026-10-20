package org.hyeong.booe.contract.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 동/호 선택 후 단건 전용면적 조회 요청.
 * dongNm/hoNm 은 동/호 리스트 응답의 dongValue/hoValue(원본 값)를 그대로 담는다.
 * 동이 없는 단일동 건물의 경우 dongNm 은 공백/빈값일 수 있다.
 */
@Getter
@NoArgsConstructor
public class UnitAreaReqDto {

    @NotBlank(message = "PNU 코드는 필수 입력 항목입니다.")
    @Size(min = 19, max = 19, message = "PNU 코드는 반드시 19자리여야 합니다.")
    @Pattern(regexp = "^[0-9]*$", message = "PNU 코드는 숫자만 포함할 수 있습니다.")
    private String pnu;

    @NotNull(message = "동 값은 필수입니다. (동이 없는 건물은 빈 문자열)")
    private String dongNm;

    @NotBlank(message = "호 값은 필수입니다.")
    private String hoNm;
}
