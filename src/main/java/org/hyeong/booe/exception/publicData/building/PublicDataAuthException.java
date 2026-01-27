package org.hyeong.booe.exception.publicData.building;

import org.hyeong.booe.exception.BusinessException;
import org.hyeong.booe.exception.ErrorCode;

public class PublicDataAuthException extends BusinessException {
    public PublicDataAuthException() {
        super(ErrorCode.PUBLIC_DATA_AUTH_FAILED);
    }
}
