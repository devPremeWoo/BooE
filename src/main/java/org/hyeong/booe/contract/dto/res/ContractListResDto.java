package org.hyeong.booe.contract.dto.res;

import lombok.Getter;
import org.hyeong.booe.contract.domain.Contract;
import org.hyeong.booe.contract.domain.type.ContractStatus;

import java.time.LocalDateTime;

@Getter
public class ContractListResDto {

    private final Long contractId;
    private final String title;
    private final String address;
    private final ContractStatus status;
    private final boolean deletedByCounterpart;
    private final LocalDateTime createdAt;

    private ContractListResDto(Long contractId, String title, String address,
                               ContractStatus status, boolean deletedByCounterpart,
                               LocalDateTime createdAt) {
        this.contractId = contractId;
        this.title = title;
        this.address = address;
        this.status = status;
        this.deletedByCounterpart = deletedByCounterpart;
        this.createdAt = createdAt;
    }

    public static ContractListResDto of(Contract contract, Long memberId) {
        boolean deletedByCounterpart = resolveDeletedByCounterpart(contract, memberId);
        return new ContractListResDto(
                contract.getId(), contract.getTitle(), contract.getAddress(),
                contract.getStatus(), deletedByCounterpart, contract.getCreatedAt());
    }

    private static boolean resolveDeletedByCounterpart(Contract contract, Long memberId) {
        if (contract.isLessor(memberId)) return contract.isDeletedByLessee();
        if (contract.isLessee(memberId)) return contract.isDeletedByLessor();
        return false;
    }
}
