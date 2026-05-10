package org.hyeong.booe.contract.dto.res;

import lombok.Getter;
import org.hyeong.booe.contract.domain.Contract;
import org.hyeong.booe.contract.domain.ContractPaymentSchedule;
import org.hyeong.booe.contract.domain.type.ContractRole;
import org.hyeong.booe.contract.domain.type.ContractStatus;
import org.hyeong.booe.contract.domain.type.ContractType;
import org.hyeong.booe.contract.domain.type.PaymentType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;

@Getter
public class ContractListResDto {

    private final Long contractId;
    private final String address;
    private final ContractStatus status;
    private final ContractType contractType;
    private final ContractRole myRole;
    private final boolean deletedByCounterpart;
    private final LocalDateTime createdAt;
    private final PaymentType nextPaymentType;
    private final LocalDate nextPaymentDueDate;
    private final Long nextPaymentAmount;

    private ContractListResDto(Long contractId, String address, ContractStatus status,
                               ContractType contractType, ContractRole myRole,
                               boolean deletedByCounterpart, LocalDateTime createdAt,
                               PaymentType nextPaymentType, LocalDate nextPaymentDueDate, Long nextPaymentAmount) {
        this.contractId = contractId;
        this.address = address;
        this.status = status;
        this.contractType = contractType;
        this.myRole = myRole;
        this.deletedByCounterpart = deletedByCounterpart;
        this.createdAt = createdAt;
        this.nextPaymentType = nextPaymentType;
        this.nextPaymentDueDate = nextPaymentDueDate;
        this.nextPaymentAmount = nextPaymentAmount;
    }

    public static ContractListResDto of(Contract contract, Long memberId) {
        ContractRole myRole = resolveRole(contract, memberId);
        boolean deletedByCounterpart = resolveDeletedByCounterpart(contract, myRole);
        Optional<ContractPaymentSchedule> next = findNextPayment(contract);

        return new ContractListResDto(
                contract.getId(),
                contract.getAddress(),
                contract.getStatus(),
                contract.getType(),
                myRole,
                deletedByCounterpart,
                contract.getCreatedAt(),
                next.map(ContractPaymentSchedule::getType).orElse(null),
                next.map(ContractPaymentSchedule::getDueDate).orElse(null),
                next.map(ContractPaymentSchedule::getAmount).orElse(null)
        );
    }

    private static ContractRole resolveRole(Contract contract, Long memberId) {
        return contract.isLessor(memberId) ? ContractRole.LESSOR : ContractRole.LESSEE;
    }

    private static boolean resolveDeletedByCounterpart(Contract contract, ContractRole myRole) {
        return myRole == ContractRole.LESSOR
                ? contract.isDeletedByLessee()
                : contract.isDeletedByLessor();
    }

    private static Optional<ContractPaymentSchedule> findNextPayment(Contract contract) {
        LocalDate today = LocalDate.now();
        return contract.getPaymentSchedules().stream()
                .filter(s -> !s.getDueDate().isBefore(today))
                .min(Comparator.comparing(ContractPaymentSchedule::getDueDate));
    }
}
