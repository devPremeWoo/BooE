package org.hyeong.booe.auth.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoUserInfoDto {

    private Long id;  // 필수! 카카오 고유 식별자

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    @Getter
    @NoArgsConstructor
    public static class KakaoAccount {

        private String email;
        private String name;

        @JsonProperty("phone_number")
        private String phoneNumber;

        // profile 통째로 삭제
    }

    public String getEmail() {
        if (kakaoAccount == null) return null;
        return kakaoAccount.getEmail();
    }

    public String getName() {
        if (kakaoAccount == null) return null;
        return kakaoAccount.getName();
    }

    public String getPhoneNumber() {
        if (kakaoAccount == null) return null;
        return kakaoAccount.getPhoneNumber();
    }
}