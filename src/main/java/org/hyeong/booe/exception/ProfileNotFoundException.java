package org.hyeong.booe.exception;

public class ProfileNotFoundException extends BusinessException {

    public ProfileNotFoundException() {
        super(ErrorCode.PROFILE_NOT_FOUND);
    }
}
