package org.hyeong.booe.exception;

public class PhoneNumberRequiredException extends BusinessException {

    public PhoneNumberRequiredException() {
        super(ErrorCode.PHONE_NUMBER_REQUIRED);
    }
}
