package org.hyeong.booe.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.concurrent.TimeoutException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<ErrorResponseDto> handleBusinessException(BusinessException e) {
        return ErrorResponseDto.toResponseEntity(e.getErrorCode());
    }

    @ExceptionHandler({WebClientResponseException.class, TimeoutException.class})
    public ResponseEntity<ErrorResponseDto> handleExternalApiException(Exception e) {
        log.error("[외부 API 오류] {}", e.getMessage(), e);
        return ErrorResponseDto.toResponseEntity(ErrorCode.EXTERNAL_API_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleException(Exception e) {
        log.error("[서버 오류] {}", e.getMessage(), e);
        return ErrorResponseDto.toResponseEntity(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
