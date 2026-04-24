package org.hyeong.booe.payment.dto.reqeust;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentOrderReqDto {

    @NotNull
    private Long ContractId;
}
