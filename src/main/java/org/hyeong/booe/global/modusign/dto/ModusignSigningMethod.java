package org.hyeong.booe.global.modusign.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/**
 * 참여자 서명 수령 방식
 * KAKAO_MOBILE: 카카오 메시지로 서명 링크 발송 (value = 휴대폰 번호)
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModusignSigningMethod {

    private final String type;
    private final String value;

    private ModusignSigningMethod(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public static ModusignSigningMethod kakao(String mobileNumber) {
        return new ModusignSigningMethod("KAKAO_MOBILE", mobileNumber);
    }
}
