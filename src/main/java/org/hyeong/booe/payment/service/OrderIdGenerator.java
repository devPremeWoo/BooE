package org.hyeong.booe.payment.service;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrderIdGenerator {

    public String generate(Long contractId) {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return "BOOE-" + contractId + "-" + uuid;
    }
}
