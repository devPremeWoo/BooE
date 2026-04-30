package org.hyeong.booe.auth.oauth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hyeong.booe.member.domain.Member;
import org.hyeong.booe.member.domain.MemberProfile;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class OauthSignupReqDto {

    @NotBlank(message = "가입 토큰은 필수입니다.")
    private String signupToken;

    @NotBlank(message = "이메일은 필수 입력입니다.")
    @Email(message = "올바른 이메일 형식이어야 합니다.")
    private String email;

    @NotBlank(message = "이름은 필수 입력입니다.")
    @Pattern(regexp = "^[가-힣a-zA-Z]+$", message = "이름은 한글 또는 영문 대소문자만 사용할 수 있습니다. (공백, 숫자, 특수문자 불가)")
    private String name;

    @NotNull(message = "생년월일은 필수 입력입니다.")
    @Past(message = "생년월일이 정확한지 확인해 주세요.")
    private LocalDate birthDate;

    @NotBlank(message = "휴대전화번호는 필수 입력입니다.")
    @Pattern(regexp = "^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$", message = "올바른 휴대전화번호 형식이 아닙니다.")
    private String phoneNum;

    @AssertTrue(message = "서비스 이용약관에 동의해야 합니다.")
    private Boolean termsAgreed;

    public MemberProfile toMemberProfile(Member member) {
        return MemberProfile.builder()
                .member(member)
                .email(this.email)
                .name(this.name)
                .phoneNumber(this.phoneNum)
                .birth(this.birthDate)
                .build();
    }
}
