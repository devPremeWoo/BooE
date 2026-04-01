package org.hyeong.booe.contract.dto.res;

import lombok.Getter;
import org.hyeong.booe.contract.domain.Contract;
import org.hyeong.booe.contract.domain.type.ContractStatus;

@Getter
public class ContractResDto {

    private final Long contractId;
    private final ContractStatus status;
    private final String formJson;

    private ContractResDto(Long contractId, ContractStatus status, String formJson) {
        this.contractId = contractId;
        this.status = status;
        this.formJson = formJson;
    }

    public static ContractResDto of(Contract contract, String formJson) {
        return new ContractResDto(contract.getId(), contract.getStatus(), formJson);
    }
}
