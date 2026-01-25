package org.hyeong.booe.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@AllArgsConstructor
@Getter
public enum ErrorCode {

    // --- 400 BAD REQUEST (클라이언트 요청 오류) ---
    // 인증(Verification) 관련
    VERIFICATION_NOT_FOUND(BAD_REQUEST, "V001", "인증 기록을 찾을 수 없습니다."),
    VERIFICATION_NOT_COMPLETED(BAD_REQUEST, "V002", "인증이 완료되지 않았습니다."),
    VERIFICATION_PHONE_MISMATCH(BAD_REQUEST, "V003", "인증받은 번호와 입력한 번호가 일치하지 않습니다."),
    VERIFICATION_EXPIRED(BAD_REQUEST, "V004", "인증 시간이 만료되었습니다."),

    // 객체 생성 및 연관관계 관련
    MISSING_RELATED_ENTITY(BAD_REQUEST, "M006", "필수 연관 엔티티가 누락되었습니다."),
    INVALID_BUILD_PARAMETER(BAD_REQUEST, "M007", "객체 생성 파라미터가 유효하지 않습니다."),
    INVALID_PROFILE_DATA(BAD_REQUEST, "M008", "프로필 정보 생성 데이터가 올바르지 않습니다."),
    INVALID_CREDENTIAL_DATA(BAD_REQUEST, "M009", "인증 정보 생성 데이터가 올바르지 않습니다."),


    // --- 401 UNAUTHORIZED (권한/비밀번호 오류) ---
    INVALID_PASSWORD(UNAUTHORIZED, "A001", "비밀번호가 올바르지 않습니다."),
    PASSWORD_MISMATCH(BAD_REQUEST, "A002", "비밀번호 확인이 일치하지 않습니다."),
    LOGIN_ID_NOT_FOUND(UNAUTHORIZED, "A003", "존재하지 않는 아이디입니다."),
    ACCOUNT_LOCKED(FORBIDDEN, "A004", "비밀번호 5회 실패로 계정이 잠겼습니다."),

    // 공공데이터 API 인증 관련
    PUBLIC_DATA_AUTH_FAILED(UNAUTHORIZED, "P001", "공공데이터 API 인증키가 유효하지 않습니다."),

    // --- 404 NOT FOUND (자원 없음) ---
    MEMBER_NOT_FOUND(NOT_FOUND, "M001", "존재하지 않는 회원입니다."),
    PROFILE_NOT_FOUND(NOT_FOUND, "M011", "회원 프로필 정보를 찾을 수 없습니다."),

    // 부동산 데이터 관련
    BUILDING_DATA_NOT_FOUND(NOT_FOUND, "P010", "해당 조건의 건축물 대장 정보를 찾을 수 없습니다."),
    LAND_INFO_NOT_FOUND(NOT_FOUND, "L001", "토지 정보를 조회할 수 없습니다."),
    LAND_RATIO_NOT_FOUND(NOT_FOUND, "L002", "해당 호수의 대지권 비율 정보가 없습니다."),
    EXPOS_INFO_NOT_FOUND(NOT_FOUND, "B002", "해당 호수의 전유부 정보를 찾을 수 없습니다."),

    // --- 409 CONFLICT (비즈니스 규칙 위반/중복) ---
    DUPLICATE_LOGIN_ID(CONFLICT, "M002", "이미 사용 중인 아이디입니다."),
    DUPLICATE_EMAIL(CONFLICT, "M003", "이미 가입된 이메일입니다."),
    ALREADY_REGISTERED_PHONE(CONFLICT, "M004", "이미 해당 번호로 가입된 계정이 존재합니다."),
    PHONE_NUMBER_REQUIRED(UNAUTHORIZED, "M005", "휴대폰 번호 인증이 필요한 계정입니다."),
    UNVERIFIED_PHONE_NUMBER(UNAUTHORIZED, "M005", "휴대폰 번호가 등록되지 않은 계정입니다. 재인증이 필요합니다."),
    DUPLICATE_MEMBER_CODE(CONFLICT, "M010", "중복된 회원 식별 코드가 생성되었습니다."),
    INACTIVE_MEMBER(CONFLICT, "M012", "탈퇴 혹은 정지된 계정입니다."), // 추가: 계정 상태 이상

    // --- 500 INTERNAL SERVER ERROR ---
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S001", "서버 내부 오류가 발생했습니다."),
    SAVING_MEMBER_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR, "S002", "회원 저장 중 오류가 발생했습니다."),
    MEMBER_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S003", "회원 식별 코드 생성에 실패했습니다."),
    // 외부 API 통신 관련
    PUBLIC_DATA_COMMUNICATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S010", "공공데이터 서버와의 통신 중 오류가 발생했습니다."),
    DATA_PARSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S011", "데이터 파싱 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
