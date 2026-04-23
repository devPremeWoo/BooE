package org.hyeong.booe.payment.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.exception.PaymentConfirmFailedException;
import org.hyeong.booe.exception.PaymentRefundFailedException;
import org.hyeong.booe.payment.dto.TossPaymentCancelResDto;
import org.hyeong.booe.payment.dto.TossPaymentConfirmResDto;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TossPaymentApiClient {

    private final WebClient tossPaymentWebClient;

    public TossPaymentConfirmResDto confirmPayment(String paymentKey, String orderId, Long amount) {
        TossPaymentConfirmResDto response = callConfirmApi(paymentKey, orderId, amount);
        validateResponse(response);
        logSuccess(response);
        return response;
    }

    private TossPaymentConfirmResDto callConfirmApi(String paymentKey, String orderId, Long amount) {
        try {
            return tossPaymentWebClient.post()
                    .uri("/v1/payments/confirm")
                    .bodyValue(Map.of(
                            "paymentKey", paymentKey,
                            "orderId", orderId,
                            "amount", amount
                    ))
                    .retrieve()
                    .bodyToMono(TossPaymentConfirmResDto.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("[TossPayment] 결제 승인 실패 - status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new PaymentConfirmFailedException();
        }
    }

    private void validateResponse(TossPaymentConfirmResDto response) {
        if (response == null) {
            throw new PaymentConfirmFailedException();
        }
    }

    private void logSuccess(TossPaymentConfirmResDto response) {
        log.info("[TossPayment] 결제 승인 성공 - paymentKey={}, orderId={}, amount={}",
                response.getPaymentKey(), response.getOrderId(), response.getTotalAmount());
    }

    public TossPaymentCancelResDto cancelPayment(String paymentKey, String cancelReason) {
        TossPaymentCancelResDto response = callCancelApi(paymentKey, cancelReason);
        validateCancelResponse(response);
        log.info("[TossPayment] 환불 성공 - paymentKey={}", paymentKey);
        return response;
    }

    private TossPaymentCancelResDto callCancelApi(String paymentKey, String cancelReason) {
        try {
            return tossPaymentWebClient.post()
                    .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                    .bodyValue(Map.of("cancelReason", cancelReason))
                    .retrieve()
                    .bodyToMono(TossPaymentCancelResDto.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("[TossPayment] 환불 실패 - status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new PaymentRefundFailedException();
        }
    }

    private void validateCancelResponse(TossPaymentCancelResDto response) {
        if (response == null) {
            throw new PaymentRefundFailedException();
        }
    }
}
