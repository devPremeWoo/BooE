package org.hyeong.booe.exception;

public class PaymentRefundFailedException extends BusinessException {

    public PaymentRefundFailedException() {
        super(ErrorCode.PAYMENT_REFUND_FAILED);
    }
}
