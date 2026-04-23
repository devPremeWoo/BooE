package org.hyeong.booe.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TossPaymentConfirmResDto {

    private String paymentKey;
    private String orderId;
    private String status;
    private Long totalAmount;
    private String method;
    private String approvedAt;
}
