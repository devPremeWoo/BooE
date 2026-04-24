package org.hyeong.booe.payment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hyeong.booe.global.details.CustomUserDetails;
import org.hyeong.booe.payment.dto.reqeust.PaymentConfirmReqDto;
import org.hyeong.booe.payment.dto.reqeust.PaymentOrderReqDto;
import org.hyeong.booe.payment.dto.reqeust.PaymentRefundReqDto;
import org.hyeong.booe.payment.dto.response.PaymentOrderResDto;
import org.hyeong.booe.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirmPayment(
            @RequestBody @Valid PaymentConfirmReqDto reqDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String pdfPath = paymentService.confirmPayment(reqDto, userDetails.getMemberId());
        return ResponseEntity.ok(Map.of("pdfUrl", pdfPath));
    }

    @PostMapping("/refund")
    public ResponseEntity<Void> refundPayment(
            @RequestBody @Valid PaymentRefundReqDto reqDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        paymentService.refundPayment(reqDto, userDetails.getMemberId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/order")
    public ResponseEntity<PaymentOrderResDto> order(
            @RequestBody @Valid PaymentOrderReqDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(paymentService.createOrder(dto, userDetails.getMemberId()));
    }
}
