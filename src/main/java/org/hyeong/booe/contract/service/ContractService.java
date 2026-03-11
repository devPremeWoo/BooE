package org.hyeong.booe.contract.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.contract.repository.ContractDetailRepository;
import org.hyeong.booe.contract.repository.ContractPartyRepository;
import org.hyeong.booe.contract.repository.ContractPaymentScheduleRepository;
import org.hyeong.booe.contract.repository.ContractRepository;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractPartyRepository contractPartyRepository;
    private final ContractDetailRepository contractDetailRepository;
    private final ContractPaymentScheduleRepository paymentScheduleRepository;

    public void save() {

    }
}
