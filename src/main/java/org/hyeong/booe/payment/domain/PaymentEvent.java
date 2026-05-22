package org.hyeong.booe.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hyeong.booe.global.entity.BaseEntity;
import org.hyeong.booe.payment.domain.type.PaymentEventActor;
import org.hyeong.booe.payment.domain.type.PaymentEventType;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "payment_event", indexes = {
        @Index(name = "idx_payment_event_payment_id", columnList = "payment_id")
})
public class PaymentEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private PaymentEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor", nullable = false)
    private PaymentEventActor actor;

    @Column(name = "reason")
    private String reason;

    @Column(name = "payload", columnDefinition = "JSON")
    private String payload;

    @Builder
    private PaymentEvent(Payment payment, PaymentEventType eventType,
                         PaymentEventActor actor, String reason, String payload) {
        this.payment = payment;
        this.eventType = eventType;
        this.actor = actor;
        this.reason = reason;
        this.payload = payload;
    }

    public static PaymentEvent of(Payment payment, PaymentEventType eventType,
                                  PaymentEventActor actor, String reason, String payload) {
        return PaymentEvent.builder()
                .payment(payment)
                .eventType(eventType)
                .actor(actor)
                .reason(reason)
                .payload(payload)
                .build();
    }
}
