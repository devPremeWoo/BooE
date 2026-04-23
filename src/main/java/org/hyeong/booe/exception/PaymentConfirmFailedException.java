package org.hyeong.booe.exception;

public class PaymentConfirmFailedException extends BusinessException {

    public PaymentConfirmFailedException() {
        super(ErrorCode.PAYMENT_CONFIRM_FAILED);
    }
}
