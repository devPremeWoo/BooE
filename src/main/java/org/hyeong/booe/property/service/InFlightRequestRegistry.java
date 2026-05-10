package org.hyeong.booe.property.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 같은 키로 동시에 들어온 요청에 대해 외부 호출을 1회로 축소하고 결과를 공유한다.
 * 키 prefix(예: "land:", "building:")로 도메인별 충돌을 방지한다.
 */
@Component
@Slf4j
public class InFlightRequestRegistry {

    private final ConcurrentHashMap<String, Mono<?>> inflight = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> Mono<T> dedupe(String key, Supplier<Mono<T>> loader) {
        return (Mono<T>) inflight.computeIfAbsent(key, k -> buildSharedMono(k, loader));
    }

    private <T> Mono<T> buildSharedMono(String key, Supplier<Mono<T>> loader) {
        return loader.get()
                .doFinally(sig -> inflight.remove(key))
                .cache();
    }
}
