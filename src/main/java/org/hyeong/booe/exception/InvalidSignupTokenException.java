package org.hyeong.booe.exception;

public class InvalidSignupTokenException extends BusinessException {

    public InvalidSignupTokenException() {
        super(ErrorCode.INVALID_SIGNUP_TOKEN);
    }
}
