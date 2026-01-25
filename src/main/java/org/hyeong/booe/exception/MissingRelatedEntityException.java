package org.hyeong.booe.exception;

public class MissingRelatedEntityException extends BusinessException {

    public MissingRelatedEntityException() {
        super(ErrorCode.MISSING_RELATED_ENTITY);
    }
}
