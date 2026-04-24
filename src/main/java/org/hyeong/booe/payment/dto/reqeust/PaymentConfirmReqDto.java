package org.hyeong.booe.payment.dto.reqeust;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentConfirmReqDto {

    @NotNull
    private Long contractId;

    @NotBlank
    private String paymentKey;

    @NotBlank
    private String orderId;

    @NotNull
    private Long amount;
}
