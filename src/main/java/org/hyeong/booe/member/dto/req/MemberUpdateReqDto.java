package org.hyeong.booe.member.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberUpdateReqDto {

    @NotBlank
    private String name;

    @NotBlank
    private String email;
}
