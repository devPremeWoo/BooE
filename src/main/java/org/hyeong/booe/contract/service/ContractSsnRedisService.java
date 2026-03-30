package org.hyeong.booe.contract.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * SSN(주민번호)은 DB에 저장하지 않고 Redis에 TTL로 임시 보관.
 * PDF 생성 완료 후 즉시 삭제.
 */
@Service
@RequiredArgsConstructor
public class ContractSsnRedisService {

    private static final String LESSOR_KEY = "contract:%d:lessor_ssns";
    private static final String LESSEE_KEY = "contract:%d:lessee_ssns";
    private static final Duration TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;

    public void saveLessorSsns(Long contractId, List<String> encryptedSsns) {
        redisTemplate.opsForValue().set(lessorKey(contractId), encryptedSsns, TTL);
    }

    public void saveLesseeSsns(Long contractId, List<String> encryptedSsns) {
        redisTemplate.opsForValue().set(lesseeKey(contractId), encryptedSsns, TTL);
    }

    public boolean hasLessorSsns(Long contractId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(lessorKey(contractId)));
    }

    public boolean hasLesseeSsns(Long contractId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(lesseeKey(contractId)));
    }

    public boolean hasBothSsns(Long contractId) {
        return hasLessorSsns(contractId) && hasLesseeSsns(contractId);
    }

    @SuppressWarnings("unchecked")
    public List<String> getLessorSsns(Long contractId) {
        return (List<String>) redisTemplate.opsForValue().get(lessorKey(contractId));
    }

    @SuppressWarnings("unchecked")
    public List<String> getLesseeSsns(Long contractId) {
        return (List<String>) redisTemplate.opsForValue().get(lesseeKey(contractId));
    }

    public void deleteAll(Long contractId) {
        redisTemplate.delete(lessorKey(contractId));
        redisTemplate.delete(lesseeKey(contractId));
    }

    private String lessorKey(Long contractId) {
        return String.format(LESSOR_KEY, contractId);
    }

    private String lesseeKey(Long contractId) {
        return String.format(LESSEE_KEY, contractId);
    }
}
