package org.hyeong.booe.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.payment.api.TossPaymentApiClient;
import org.hyeong.booe.payment.domain.Payment;
import org.hyeong.booe.payment.domain.type.PaymentStatus;
import org.hyeong.booe.payment.dto.response.PaymentStatusCheckResDto;
import org.hyeong.booe.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentReconciliationService {

    private static final long PENDING_THRESHOLD_MINUTES = 5;

    private final TossPaymentApiClient tossPaymentApiClient;
    private final PaymentRepository paymentRepository;
    private final PaymentRecordService paymentRecordService;
    private final ObjectMapper objectMapper;

    public void reconcilePayments() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(PENDING_THRESHOLD_MINUTES);
        List<Payment> pendings = paymentRepository
                .findByStatusAndCreatedAtBefore(PaymentStatus.PENDING, threshold);

        if (pendings.isEmpty()) {
            return;
        }

        log.info("[Reconcile] 시작 - 대상 {}건", pendings.size());
        pendings.forEach(this::reconcileOne);
    }

    private void reconcileOne(Payment payment) {
        try {
            PaymentStatusCheckResDto response =
                    tossPaymentApiClient.getPaymentStatus(payment.getPaymentKey());
            if (response == null) {
                return;
            }
            dispatchByStatus(payment, response);
        } catch (Exception e) {
            log.error("[Reconcile] 처리 실패 - paymentId={}", payment.getId(), e);
        }
    }

    private void dispatchByStatus(Payment payment, PaymentStatusCheckResDto response) {
        String tossStatus = response.getStatus();
        switch (tossStatus) {
            case "DONE" -> handleDone(payment, response);
            case "CANCELED", "ABORTED", "EXPIRED" -> handleCanceledOnToss(payment, tossStatus);
            case "IN_PROGRESS", "WAITING_FOR_DEPOSIT", "READY" -> {
                // 토스 측 진행 중 → 다음 회차에 재시도
            }
            default -> log.warn("[Reconcile] 알 수 없는 토스 status - paymentId={}, status={}",
                    payment.getId(), tossStatus);
        }
    }

    private void handleDone(Payment payment, PaymentStatusCheckResDto response) {
        if (!isMatched(payment, response)) {
            paymentRecordService.markAbandoned(payment.getId(),
                    "amount/orderId mismatch with toss");
            return;
        }
        paymentRecordService.reconcileToApprove(
                payment.getId(), payment.getContract().getId(),
                response, serializeToJson(response));
    }

    private void handleCanceledOnToss(Payment payment, String tossStatus) {
        paymentRecordService.reconcileToCancel(
                payment.getId(), LocalDateTime.now(),
                "toss status: " + tossStatus);
    }

    private boolean isMatched(Payment payment, PaymentStatusCheckResDto response) {
        return Objects.equals(payment.getOrderId(), response.getOrderId())
                && Objects.equals(payment.getAmount(), response.getTotalAmount());
    }

    private String serializeToJson(PaymentStatusCheckResDto response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.warn("[Reconcile] JSON 직렬화 실패", e);
            return null;
        }
    }
}
