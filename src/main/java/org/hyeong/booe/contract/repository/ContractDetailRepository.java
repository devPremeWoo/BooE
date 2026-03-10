package org.hyeong.booe.contract.repository;

import org.hyeong.booe.contract.domain.ContractDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractDetailRepository extends JpaRepository<ContractDetail, Long> {
}
