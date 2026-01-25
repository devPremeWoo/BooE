package org.hyeong.booe.exception;

public class VerificationPhoneMismatchException extends BusinessException {
    public VerificationPhoneMismatchException() {
        super(ErrorCode.VERIFICATION_PHONE_MISMATCH);
    }
}
