package org.hyeong.booe.exception;

public class VerificationNotCompletedException extends BusinessException {

    public VerificationNotCompletedException() {
        super(ErrorCode.VERIFICATION_NOT_COMPLETED);
    }
}
