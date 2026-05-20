package org.hyeong.booe.payment.service;

import lombok.RequiredArgsConstructor;
import org.hyeong.booe.payment.dto.PaymentOrderInfo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PaymentOrderRedisService {

    private static final String ORDER_KEY_PREFIX = "payment:order:";
    private static final long ORDER_TTL = 15;
    private static final String CONFIRM_KEY_PREFIX = "payment:confirm:";
    private static final long CONFIRM_TTL = 60;

    private final RedisTemplate<String, Object> redisTemplate;

    public void saveOrderInfo(Long contractId, PaymentOrderInfo orderInfo) {
        redisTemplate.opsForValue()
                .set(buildOrderKey(contractId), orderInfo, ORDER_TTL, TimeUnit.MINUTES);
    }

    public PaymentOrderInfo findOrderInfo(Long contractId) {
        return (PaymentOrderInfo) redisTemplate.opsForValue()
                .get(buildOrderKey(contractId));
    }

    public boolean savePaymentConfirm(String paymentKey) {
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(buildConfirmKey(paymentKey), "PROCESSING",
                CONFIRM_TTL, TimeUnit.SECONDS);

        return Boolean.TRUE.equals(result);
    }

    public void delete(Long contractId) {
        redisTemplate.delete(buildOrderKey(contractId));
    }

    private String buildOrderKey(Long contractId) {return ORDER_KEY_PREFIX + contractId;}
    private String buildConfirmKey(String paymentKey) {return CONFIRM_KEY_PREFIX + paymentKey;}
}
