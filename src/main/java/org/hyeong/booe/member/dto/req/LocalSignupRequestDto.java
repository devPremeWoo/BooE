package org.hyeong.booe.member.dto.req;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hyeong.booe.member.domain.Member;
import org.hyeong.booe.member.domain.MemberCredential;
import org.hyeong.booe.member.domain.MemberProfile;

import java.time.LocalDate;

@Getter
@ToString
@NoArgsConstructor
public class LocalSignupRequestDto {

    @NotBlank(message = "아이디는 필수 입력입니다.")
    @Pattern(regexp = "^[a-z0-9_-]{5,20}$", message = "아이디는 5~20자의 영문 소문자, 숫자, 특수기호(_),(-)만 사용할 수 있습니다.")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수 입력입니다.")
    @Size(min = 10, message = "비밀번호는 최소 10자 이상이어야 합니다.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{10,}$", message = "비밀번호는 영문자, 숫자, 특수문자를 모두 포함해야 합니다.")
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수 입력입니다.")
    private String passwordConfirm;

    @NotBlank(message = "이름은 필수 입력입니다.")
    @Pattern(regexp = "^[가-힣a-zA-Z]+$",message = "이름은 한글 또는 영문 대소문자만 사용할 수 있습니다. (공백, 숫자, 특수문자 불가)")
    private String name;

    @NotBlank(message = "닉네임은 필수 입력입니다.")
    @Size(min = 2, max = 10, message = "닉네임은 2~10자 사이여야 합니다.")
    private String nickname;

    @NotNull(message = "생년월일은 필수 입력입니다.")
    @Past(message = "생년월일이 정확한지 확인해 주세요.")
    private LocalDate birthDate;

    @NotBlank(message = "이메일은 필수 입력입니다.")
    @Email(message = "올바른 이메일 형식이어야 합니다.")
    private String email;

    @NotBlank(message = "휴대전화번호는 필수 입력입니다.")
    @Pattern(regexp = "^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$", message = "올바른 휴대전화번호 형식이 아닙니다.")
    private String phoneNum;

    // 추가 제안: 약관 동의 여부 등
    @AssertTrue(message = "서비스 이용약관에 동의해야 합니다.")
    private Boolean termsAgreed;

    @NotNull
    private Long verificationId;

    public MemberProfile toMemberProfile(Member member) {
        return MemberProfile.builder()
                .member(member)
                .email(this.getEmail())
                .name(this.getName())
                .phoneNumber(this.phoneNum)
                .birth(this.getBirthDate())
                .build();

    }

    public MemberCredential toMemberCredential(Member member, String password) {
        return MemberCredential.builder()
                .member(member)
                .loginId(this.getLoginId())
                .encodedPassword(password)
                .build();
    }


}
