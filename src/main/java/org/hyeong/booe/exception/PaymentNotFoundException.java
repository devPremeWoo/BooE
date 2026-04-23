package org.hyeong.booe.exception;

public class PaymentNotFoundException extends BusinessException {

    public PaymentNotFoundException() {
        super(ErrorCode.PAYMENT_NOT_FOUND);
    }
}
