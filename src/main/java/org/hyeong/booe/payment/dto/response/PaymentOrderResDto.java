package org.hyeong.booe.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentOrderResDto {

    private String orderId;
    private Long amount;
    private String orderName;
}
