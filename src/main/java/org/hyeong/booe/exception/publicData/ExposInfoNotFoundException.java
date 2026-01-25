package org.hyeong.booe.exception.publicData;

import org.hyeong.booe.exception.BusinessException;
import org.hyeong.booe.exception.ErrorCode;

public class ExposInfoNotFoundException extends BusinessException {
    public ExposInfoNotFoundException() {
        super(ErrorCode.EXPOS_INFO_NOT_FOUND);
    }
}
