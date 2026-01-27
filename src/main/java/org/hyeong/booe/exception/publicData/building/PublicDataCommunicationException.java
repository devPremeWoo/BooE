package org.hyeong.booe.exception.publicData.building;

import org.hyeong.booe.exception.BusinessException;
import org.hyeong.booe.exception.ErrorCode;

public class PublicDataCommunicationException extends BusinessException {
    public PublicDataCommunicationException() {
        super(ErrorCode.PUBLIC_DATA_COMMUNICATION_ERROR);
    }
}
