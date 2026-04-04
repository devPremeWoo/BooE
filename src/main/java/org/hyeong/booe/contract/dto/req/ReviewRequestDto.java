package org.hyeong.booe.contract.dto.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.groups.ConvertGroup;
import jakarta.validation.groups.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 임대인이 임차인에게 계약서 확인 요청 시 사용.
 * 최종 계약 내용 저장 + 임차인 전화번호로 FCM 발송.
 */
@Getter
@NoArgsConstructor
public class ReviewRequestDto {

    @NotNull
    @Valid
    @ConvertGroup(from = Default.class, to = ContractBaseReqDto.ReviewRequest.class)
    private ContractBaseReqDto contract;

    @NotBlank
    private String lesseePhoneNumber;
}
