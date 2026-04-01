package org.hyeong.booe.contract.dto.req;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 임대인이 계약금 수령 확인 후 영수자 정보를 입력하고 결제 요청으로 넘어가는 DTO
 */
@Getter
@NoArgsConstructor
public class DownPaymentConfirmReqDto {

    private String receiverName;   // 영수자 이름
    private String receiverPhone;  // 영수자 연락처
}
