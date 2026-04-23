package org.hyeong.booe.exception;

public class PaymentAmountMismatchException extends BusinessException {

    public PaymentAmountMismatchException() {
        super(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
    }
}
