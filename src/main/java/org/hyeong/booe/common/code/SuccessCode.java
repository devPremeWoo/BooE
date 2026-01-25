package org.hyeong.booe.common.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@AllArgsConstructor
@Getter
public enum SuccessCode {

    // 200 OK
    LOGIN_SUCCESS(OK, "MEMBER_200", "로그인에 성공했습니다."),
    SIGNUP_SUCCESS(CREATED, "MEMBER_201", "회원가입이 완료되었습니다."),
    LOGOUT_SUCCESS(OK, "MEMBER_202", "로그아웃이 완료되었습니다."),
    TOKEN_REISSUE_SUCCESS(OK, "AUTH_200", "토큰이 재발급되었습니다."),

    // 204 NO_CONTENT
    DELETE_SUCCESS(NO_CONTENT, "COMMON_204", "삭제가 완료되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
