package org.hyeong.booe.exception.server;

import org.hyeong.booe.exception.BusinessException;
import org.hyeong.booe.exception.ErrorCode;

public class DataParsingException extends BusinessException {
    public DataParsingException() {
        super(ErrorCode.DATA_PARSING_ERROR);
    }
}
