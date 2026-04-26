package org.hyeong.booe.contract.dto.res;

import lombok.Getter;
import org.hyeong.booe.contract.domain.Contract;
import org.hyeong.booe.contract.domain.type.ContractStatus;

@Getter
public class ContractResDto {

    private final Long contractId;
    private final ContractStatus status;
    private final String formJson;
    private final boolean terminatedByCounterpart;

    private ContractResDto(Long contractId, ContractStatus status,
                           String formJson, boolean terminatedByCounterpart) {
        this.contractId = contractId;
        this.status = status;
        this.formJson = formJson;
        this.terminatedByCounterpart = terminatedByCounterpart;
    }

    public static ContractResDto of(Contract contract, String formJson, Long memberId) {
        boolean terminated = resolveTerminated(contract, memberId);
        return new ContractResDto(contract.getId(), contract.getStatus(), formJson, terminated);
    }

    private static boolean resolveTerminated(Contract contract, Long memberId) {
        if (contract.isLessor(memberId)) return contract.isDeletedByLessee();
        if (contract.isLessee(memberId)) return contract.isDeletedByLessor();
        return false;
    }
}
