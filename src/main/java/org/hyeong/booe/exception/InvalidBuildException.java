package org.hyeong.booe.exception;

public class InvalidBuildException extends BusinessException {

    public InvalidBuildException() {
        super(ErrorCode.INVALID_BUILD_PARAMETER);
    }
}
