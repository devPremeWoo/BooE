package org.hyeong.booe.exception;

public class AlreadyRegisteredMemberException extends BusinessException {

    public AlreadyRegisteredMemberException() {
        super(ErrorCode.ALREADY_REGISTERED_PHONE);
    }
}
