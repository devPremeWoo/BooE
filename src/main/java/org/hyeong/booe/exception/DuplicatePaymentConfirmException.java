package org.hyeong.booe.exception;

public class DuplicatePaymentConfirmException extends BusinessException {

    public DuplicatePaymentConfirmException() {
        super(ErrorCode.PAYMENT_DUPLICATE_CONFIRM);
    }
}
