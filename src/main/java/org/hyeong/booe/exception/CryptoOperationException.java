package org.hyeong.booe.exception;

public class CryptoOperationException extends BusinessException {

    public CryptoOperationException(Throwable cause) {
        super(ErrorCode.CRYPTO_OPERATION_FAILED, cause);
    }
}
