package org.hyeong.booe.payment.scheduler;

import lombok.RequiredArgsConstructor;
import org.hyeong.booe.payment.service.PaymentReconciliationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentReconciliationScheduler {

    private final PaymentReconciliationService reconciliationService;

    @Scheduled(
            fixedDelay = 5 * 60 * 1000,  // 5분
            initialDelay = 60 * 1000      // 어플 시작 1분 후 첫 실행
    )
    public void reconcilePayments() {
        reconciliationService.reconcilePayments();
    }
}
