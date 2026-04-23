package org.hyeong.booe.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentRefundReqDto {

    @NotNull
    private Long contractId;

    @NotBlank
    private String cancelReason;
}
