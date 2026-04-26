package org.hyeong.booe.exception;

public class PaymentOrderInvalidException extends BusinessException {

    public PaymentOrderInvalidException() {
        super(ErrorCode.PAYMENT_ORDER_INVALID);
    }
}
