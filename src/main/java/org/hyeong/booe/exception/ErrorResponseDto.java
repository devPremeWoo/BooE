package org.hyeong.booe.exception;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

@Getter
@Builder
public class ErrorResponseDto {

    private final String code;
    private final String message;

    public static ResponseEntity<ErrorResponseDto> toResponseEntity(ErrorCode e) {
        return ResponseEntity.status(e.getHttpStatus())
                .body(ErrorResponseDto.builder()
                        .code(e.getCode())
                        .message(e.getMessage())
                        .build());
    }
}
