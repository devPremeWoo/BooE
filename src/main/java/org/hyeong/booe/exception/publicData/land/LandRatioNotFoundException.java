package org.hyeong.booe.exception.publicData.land;

import org.hyeong.booe.exception.BusinessException;
import org.hyeong.booe.exception.ErrorCode;

public class LandRatioNotFoundException extends BusinessException {
    public LandRatioNotFoundException() {
        super(ErrorCode.LAND_RATIO_NOT_FOUND);
    }
}
