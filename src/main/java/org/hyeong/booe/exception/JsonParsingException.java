package org.hyeong.booe.exception;

public class JsonParsingException extends BusinessException {
    public JsonParsingException() {
        super(ErrorCode.JSON_PARSING_ERROR);
    }
}
