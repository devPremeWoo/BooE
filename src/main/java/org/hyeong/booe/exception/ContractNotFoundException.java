package org.hyeong.booe.exception;

public class ContractNotFoundException extends BusinessException {

    public ContractNotFoundException() {
        super(ErrorCode.CONTRACT_NOT_FOUND);
    }
}
