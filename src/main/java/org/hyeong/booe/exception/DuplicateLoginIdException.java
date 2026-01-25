package org.hyeong.booe.exception;

public class DuplicateLoginIdException extends BusinessException{

    public DuplicateLoginIdException() {
        super(ErrorCode.DUPLICATE_LOGIN_ID);
    }

}
