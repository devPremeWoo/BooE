package org.hyeong.booe.exception;

public class OauthUserInfoFetchException extends BusinessException {

    public OauthUserInfoFetchException() {
        super(ErrorCode.OAUTH_USER_INFO_FETCH_FAILED);
    }
}
