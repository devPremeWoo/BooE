package org.hyeong.booe.contract.domain;


import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hyeong.booe.contract.domain.type.PaymentStatus;
import org.hyeong.booe.contract.domain.type.PaymentType;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "contract_payment_schedule")
public class ContractPaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type")
    private PaymentType type; // 계약금, 중도금, 잔금, 월세

    @Column(name = "amount")
    private Long amount;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "payment_order")
    private Integer paymentOrder;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

//    @Builder
//    private ContractPaymentSchedule(Contract contract, PaymentType type, Long amount,
//                                    LocalDate dueDate, Integer paymentOrder) {
//        this.contract = contract;
//        this.type = type;
//        this.amount = amount;
//        this.dueDate = dueDate;
//        this.paymentOrder = (paymentOrder != null) ? paymentOrder : 1;
//        this.status = PaymentStatus.WAITING;
//    }
//
//    // 상태 업데이트 로직 (납부 완료 처리 등)
//    public void markAsPaid() {
//        this.status = PaymentStatus.PAID;
//    }
}
