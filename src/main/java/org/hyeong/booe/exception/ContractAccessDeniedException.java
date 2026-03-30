package org.hyeong.booe.exception;

public class ContractAccessDeniedException extends BusinessException {

    public ContractAccessDeniedException() {
        super(ErrorCode.CONTRACT_ACCESS_DENIED);
    }
}
