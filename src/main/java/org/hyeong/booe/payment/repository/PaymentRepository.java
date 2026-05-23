package org.hyeong.booe.payment.repository;

import org.hyeong.booe.payment.domain.Payment;
import org.hyeong.booe.payment.domain.type.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByContractIdAndStatus(Long contractId, PaymentStatus status);

    List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime threshold);
}
