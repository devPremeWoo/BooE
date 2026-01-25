package org.hyeong.booe.exception;

public class UnverifiedPhoneNumberException extends BusinessException {
    public UnverifiedPhoneNumberException() {
        super(ErrorCode.UNVERIFIED_PHONE_NUMBER);
    }
}
