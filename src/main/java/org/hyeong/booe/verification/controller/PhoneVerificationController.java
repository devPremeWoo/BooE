package org.hyeong.booe.verification.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hyeong.booe.verification.dto.req.PhoneVerificationConfirmDto;
import org.hyeong.booe.verification.dto.req.PhoneVerifyRequestDto;
import org.hyeong.booe.verification.dto.res.VerificationConfirmResponseDto;
import org.hyeong.booe.verification.service.PhoneVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/phone-verification")
public class PhoneVerificationController {

    private final PhoneVerificationService verificationService;

    // 인증 코드 발송
    @PostMapping("/send")
    public ResponseEntity<String> sendSms(@RequestBody PhoneVerifyRequestDto requestDto) {
        verificationService.sendCode(requestDto.getPhoneNum());
        return ResponseEntity.ok("인증 코드가 발송되었습니다. (서버 로그를 확인하세요)");
    }

    // 인증 코드 검증
    @PostMapping("/confirm")
    public ResponseEntity<VerificationConfirmResponseDto> confirmSms(@RequestBody @Valid PhoneVerificationConfirmDto requestDto) {
        Long verificationId = verificationService.verifyCode(requestDto.getPhoneNum(), requestDto.getCode());
        return ResponseEntity.ok(new VerificationConfirmResponseDto(verificationId, requestDto.getPhoneNum()));
    }
}
