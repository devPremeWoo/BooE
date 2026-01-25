package org.hyeong.booe.exception;

public class PhoneVerificationNotFoundException extends BusinessException {
    public PhoneVerificationNotFoundException() {
        super(ErrorCode.VERIFICATION_NOT_FOUND);
    }
}
