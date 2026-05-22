package org.hyeong.booe.payment.domain.type;

public enum PaymentEventType {
    CREATED,
    APPROVED,
    CANCEL_SUCCEEDED,
    CANCEL_FAILED,
    RECONCILED,
    ABANDONED,
    AMOUNT_MISMATCH
}
