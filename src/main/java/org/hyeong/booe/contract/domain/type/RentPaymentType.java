package org.hyeong.booe.contract.domain.type;

public enum RentPaymentType {
    PRE("선불"), POST("후불");

    private final String label;

    RentPaymentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
