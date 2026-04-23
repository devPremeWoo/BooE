package org.hyeong.booe.payment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hyeong.booe.global.details.CustomUserDetails;
import org.hyeong.booe.payment.dto.PaymentConfirmReqDto;
import org.hyeong.booe.payment.dto.PaymentRefundReqDto;
import org.hyeong.booe.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirmPayment(
            @RequestBody @Valid PaymentConfirmReqDto reqDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        paymentService.confirmPayment(reqDto, userDetails.getMemberId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refund")
    public ResponseEntity<Void> refundPayment(
            @RequestBody @Valid PaymentRefundReqDto reqDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        paymentService.refundPayment(reqDto, userDetails.getMemberId());
        return ResponseEntity.ok().build();
    }
}
