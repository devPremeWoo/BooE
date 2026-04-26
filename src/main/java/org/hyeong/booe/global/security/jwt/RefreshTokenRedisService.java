package org.hyeong.booe.global.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenRedisService {

    private static final String KEY_PREFIX = "refresh:";
    private static final long TTL_DAYS = 7;

    private final StringRedisTemplate redisTemplate;

    public void save(String memberCode, String refreshToken) {
        redisTemplate.opsForValue().set(buildKey(memberCode), refreshToken, TTL_DAYS, TimeUnit.DAYS);
    }

    public String find(String memberCode) {
        return redisTemplate.opsForValue().get(buildKey(memberCode));
    }

    public void delete(String memberCode) {
        redisTemplate.delete(buildKey(memberCode));
    }

    private String buildKey(String memberCode) {
        return KEY_PREFIX + memberCode;
    }
}
