package org.hyeong.booe.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderInfo {

    private String orderId;
    private Long amount;
}
