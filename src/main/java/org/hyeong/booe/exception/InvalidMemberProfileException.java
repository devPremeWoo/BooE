package org.hyeong.booe.exception;

public class InvalidMemberProfileException extends BusinessException {
    public InvalidMemberProfileException() {
        super(ErrorCode.INVALID_PROFILE_DATA);
    }
}
