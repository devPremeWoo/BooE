package org.hyeong.booe.contract.repository;

import org.hyeong.booe.contract.domain.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<Contract, Long> {

}
