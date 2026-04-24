package org.hyeong.booe.payment.service;

import lombok.RequiredArgsConstructor;
import org.hyeong.booe.payment.dto.PaymentOrderInfo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PaymentOrderRedisService {

    private static final String KEY_PREFIX = "payment:order:";
    private static final long ORDER_TTL = 15;

    private final RedisTemplate<String, Object> redisTemplate;

    public void save(Long contractId, PaymentOrderInfo orderInfo) {
        redisTemplate.opsForValue()
                .set(buildKey(contractId), orderInfo, ORDER_TTL, TimeUnit.MINUTES);
    }

    public PaymentOrderInfo find(Long contractId) {
        return (PaymentOrderInfo) redisTemplate.opsForValue()
                .get(buildKey(contractId));
    }

    public void delete(Long contractId) {
        redisTemplate.delete(buildKey(contractId));
    }

    private String buildKey(Long contractId) {
        return KEY_PREFIX + contractId;
    }
}
