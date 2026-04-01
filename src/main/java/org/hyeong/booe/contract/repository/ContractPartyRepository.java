package org.hyeong.booe.contract.repository;

import org.hyeong.booe.contract.domain.Contract;
import org.hyeong.booe.contract.domain.ContractParty;
import org.hyeong.booe.contract.domain.type.PartyRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractPartyRepository extends JpaRepository<ContractParty, Long> {

    void deleteAllByContract(Contract contract);

    void deleteAllByContractAndRoleIn(Contract contract, List<PartyRole> roles);
}
