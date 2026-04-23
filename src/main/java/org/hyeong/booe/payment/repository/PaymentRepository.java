package org.hyeong.booe.payment.repository;

import org.hyeong.booe.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
