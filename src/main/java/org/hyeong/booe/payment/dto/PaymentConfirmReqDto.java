package org.hyeong.booe.payment.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentConfirmReqDto {

    private Long contractId;
    private String paymentKey;
    private String orderId;
    private Long amount;

}
