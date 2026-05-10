package org.hyeong.booe.contract.domain;

import org.hyeong.booe.contract.domain.type.PaymentStatus;
import org.hyeong.booe.contract.domain.type.PaymentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ContractPaymentScheduleTest {

    @Test
    @DisplayName("createPaymentSchedule: 기본 status=PENDING")
    void create_with_default_pending() {
        Contract contract = new Contract();
        LocalDate due = LocalDate.of(2026, 6, 1);

        ContractPaymentSchedule schedule = ContractPaymentSchedule.createPaymentSchedule(
                contract, PaymentType.MONTHLY_RENT, 500_000L, due, 1
        );

        assertThat(schedule.getContract()).isEqualTo(contract);
        assertThat(schedule.getType()).isEqualTo(PaymentType.MONTHLY_RENT);
        assertThat(schedule.getAmount()).isEqualTo(500_000L);
        assertThat(schedule.getDueDate()).isEqualTo(due);
        assertThat(schedule.getPaymentOrder()).isEqualTo(1);
        assertThat(schedule.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("createPaymentSchedule: BALANCE 타입도 정상 생성")
    void create_balance() {
        Contract contract = new Contract();

        ContractPaymentSchedule schedule = ContractPaymentSchedule.createPaymentSchedule(
                contract, PaymentType.BALANCE, 10_000_000L, LocalDate.of(2026, 1, 1), 0
        );

        assertThat(schedule.getType()).isEqualTo(PaymentType.BALANCE);
        assertThat(schedule.getAmount()).isEqualTo(10_000_000L);
        assertThat(schedule.getPaymentOrder()).isZero();
    }
}
