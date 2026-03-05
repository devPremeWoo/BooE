package org.hyeong.booe.exception.publicData.building;

import org.hyeong.booe.exception.BusinessException;
import org.hyeong.booe.exception.ErrorCode;

public class BuildingDataNotFoundException extends BusinessException {
    public BuildingDataNotFoundException() {
        super(ErrorCode.BUILDING_DATA_NOT_FOUND);
    }
}
