package org.hyeong.booe.verification.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VerificationConfirmResponseDto {

    private Long verificationId;
    private String phoneNum;

}
