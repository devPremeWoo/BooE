package org.hyeong.booe.exception.publicData.land;

import org.hyeong.booe.exception.BusinessException;
import org.hyeong.booe.exception.ErrorCode;

public class LandDataNotFoundException extends BusinessException {
    public LandDataNotFoundException() {
        super(ErrorCode.LAND_INFO_NOT_FOUND);
    }
}
