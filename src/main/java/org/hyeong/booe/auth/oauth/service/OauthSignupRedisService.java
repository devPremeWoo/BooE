package org.hyeong.booe.auth.oauth.service;

import lombok.RequiredArgsConstructor;
import org.hyeong.booe.auth.oauth.dto.OauthSignupTempInfo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OauthSignupRedisService {

    private static final String KEY_PREFIX = "oauth:signup:";
    private static final long TTL_MINUTES = 10;

    private final RedisTemplate<String, Object> redisTemplate;

    public String save(OauthSignupTempInfo info) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue()
                .set(buildKey(token), info, TTL_MINUTES, TimeUnit.MINUTES);
        return token;
    }

    public Optional<OauthSignupTempInfo> find(String token) {
        Object value = redisTemplate.opsForValue().get(buildKey(token));
        return Optional.ofNullable((OauthSignupTempInfo) value);
    }

    public void delete(String token) {
        redisTemplate.delete(buildKey(token));
    }

    private String buildKey(String token) {
        return KEY_PREFIX + token;
    }
}
