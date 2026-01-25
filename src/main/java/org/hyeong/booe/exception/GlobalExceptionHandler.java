package org.hyeong.booe.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<ErrorResponseDto> handleBusinessException(BusinessException e) {
        return ErrorResponseDto.toResponseEntity(e.getErrorCode());
    }

}
