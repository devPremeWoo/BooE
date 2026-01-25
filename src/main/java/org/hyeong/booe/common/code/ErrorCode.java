package org.hyeong.booe.common.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@AllArgsConstructor
@Getter
public enum ErrorCode {

    // 401 Error
    INVALID_PASSWORD(UNAUTHORIZED, "AUTH_001", "비밀번호가 일치하지 않습니다."),

    // 404 Error
    NOT_EXIST_MEMBER_ID(NOT_FOUND, "MEMBER_001", "존재하지 않는 회원입니다."),
    // 409 Error
    EXISTING_MEMBER_ID(CONFLICT, "MEMBER_002", "이미 가입된 Email 입니다."),

    // 500 Error
    SAVING_MEMBER_FAILURE(INTERNAL_SERVER_ERROR, "SERVER_001", "회원 저장 중 오류가 발생했습니다."),
    ;


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
