package org.hyeong.booe.payment.repository;

import org.hyeong.booe.payment.domain.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {
}
