package org.hyeong.booe.exception;

public class InvalidContractTermException extends BusinessException {
    public InvalidContractTermException() {
        super(ErrorCode.INVALID_CONTRACT_TERM);
    }
}
