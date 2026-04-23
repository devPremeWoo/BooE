package org.hyeong.booe.payment.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hyeong.booe.contract.domain.Contract;
import org.hyeong.booe.global.entity.BaseEntity;
import org.hyeong.booe.member.domain.Member;
import org.hyeong.booe.payment.domain.type.PaymentStatus;
import org.hyeong.booe.payment.domain.type.PaymentType;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "payment")
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "payment_key", nullable = false)
    private String paymentKey;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "method")
    private String method;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PaymentType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "raw_response", columnDefinition = "JSON")
    private String rawResponse;

    @Builder
    private Payment(Contract contract, Member member, String paymentKey,
                    String orderId, Long amount, String method,
                    PaymentType type, PaymentStatus status,
                    LocalDateTime approvedAt, String rawResponse) {
        this.contract = contract;
        this.member = member;
        this.paymentKey = paymentKey;
        this.orderId = orderId;
        this.amount = amount;
        this.method = method;
        this.type = type;
        this.status = status;
        this.approvedAt = approvedAt;
        this.rawResponse = rawResponse;
    }

    public static Payment createPayment(Contract contract, Member member,
                                        String paymentKey, String orderId,
                                        Long amount, String method,
                                        LocalDateTime approvedAt, String rawResponse) {
        return Payment.builder()
                .contract(contract)
                .member(member)
                .paymentKey(paymentKey)
                .orderId(orderId)
                .amount(amount)
                .method(method)
                .type(PaymentType.PAYMENT)
                .status(PaymentStatus.DONE)
                .approvedAt(approvedAt)
                .rawResponse(rawResponse)
                .build();
    }

    public void refund() {
        this.status = PaymentStatus.REFUNDED;
    }
}
