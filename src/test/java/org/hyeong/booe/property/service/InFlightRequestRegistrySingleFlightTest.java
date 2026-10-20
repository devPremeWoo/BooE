package org.hyeong.booe.property.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Single-flight 패턴 설계 검증 (InFlightRequestRegistry).
 *
 * <p>주장: 동일 키로 동시에 N개 요청이 몰려도 업스트림(외부 API)은 정확히 1회만 실행되고
 * 결과는 모든 호출자가 공유한다 (cache stampede 회피). 단, 영구 캐시가 아니라 in-flight 윈도우
 * 동안만 공유하며 완료 시 키를 제거한다.
 *
 * <p>외부 의존성 0 — {@code new InFlightRequestRegistry()} 단독 단위 테스트. throughput이 아니라
 * <b>업스트림 실행 횟수</b>라는 불변식을 카운터로 증명한다. {@code computeIfAbsent}는 매핑 함수를
 * 1회만 실행하고 {@code .cache()}는 동시 구독자가 많아도 소스를 1회만 구독한다.
 */
@DisplayName("Single-flight (InFlightRequestRegistry) 설계 검증")
class InFlightRequestRegistrySingleFlightTest {

    private static final Logger log = LoggerFactory.getLogger(InFlightRequestRegistrySingleFlightTest.class);

    private static final int THREADS = 100;
    private static final String KEY = "building:PNU-2818510600100040001";

    private final InFlightRequestRegistry registry = new InFlightRequestRegistry();

    @Test
    @DisplayName("동일 키 동시 100요청 → 업스트림 1회만 실행, 모두 같은 결과 공유")
    void concurrentSameKey_executesUpstreamOnce() throws InterruptedException {
        AtomicInteger upstream = new AtomicInteger();
        CountDownLatch proceed = new CountDownLatch(1); // 전 스레드가 in-flight 윈도우에 모일 때까지 소스 보류
        Supplier<Mono<String>> loader = gatedCountingLoader(upstream, proceed);

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch entered = new CountDownLatch(THREADS);
        CountDownLatch done = new CountDownLatch(THREADS);
        Set<String> results = ConcurrentHashMap.newKeySet();
        AtomicInteger errors = new AtomicInteger();

        for (int i = 0; i < THREADS; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    Mono<String> shared = registry.dedupe(KEY, loader); // 공유 Mono 획득
                    entered.countDown();
                    results.add(shared.block());
                } catch (Exception e) {
                    errors.incrementAndGet();
                    log.warn("[SF] 예상치 못한 예외", e);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();                          // 동시 발사
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue(); // 전 스레드 dedupe 진입
        proceed.countDown();                        // 그제서야 업스트림 진행
        boolean finished = done.await(10, TimeUnit.SECONDS);
        pool.shutdownNow();

        log.info("[SF] threads={} upstreamExecutions={} distinctResults={} errors={}",
                THREADS, upstream.get(), results.size(), errors.get());

        assertThat(finished).as("모든 스레드 완료").isTrue();
        assertThat(errors.get()).as("예외 없음").isZero();
        assertThat(upstream.get()).as("업스트림은 정확히 1회만 실행").isEqualTo(1);
        assertThat(results).as("모든 요청이 동일 결과 공유").containsExactly("RESULT");
    }

    @Test
    @DisplayName("완료 후 키 제거 → 동일 키 재요청 시 업스트림 재실행 (영구 캐시 아닌 in-flight 전용)")
    void afterCompletion_keyEvicted_allowsReload() {
        AtomicInteger upstream = new AtomicInteger();
        Supplier<Mono<String>> loader = countingLoader(upstream);

        registry.dedupe(KEY, loader).block(); // 1회차 완료 → doFinally가 키 제거
        registry.dedupe(KEY, loader).block(); // 2회차 → in-flight 아님 → 재로드

        assertThat(upstream.get()).as("두 번의 분리된 요청은 각각 실행").isEqualTo(2);
    }

    @Test
    @DisplayName("서로 다른 키는 dedupe되지 않고 각각 업스트림 실행")
    void differentKeys_areIsolated() {
        AtomicInteger upstream = new AtomicInteger();
        Supplier<Mono<String>> loader = countingLoader(upstream);

        registry.dedupe("building:A", loader).block();
        registry.dedupe("land:B", loader).block();

        assertThat(upstream.get()).as("키별로 독립 실행").isEqualTo(2);
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────

    /** gate가 열릴 때까지 소스 실행을 보류해, 동시 호출이 in-flight 윈도우 안에 모이도록 강제. */
    private Supplier<Mono<String>> gatedCountingLoader(AtomicInteger counter, CountDownLatch gate) {
        return () -> Mono.fromCallable(() -> {
            gate.await();
            counter.incrementAndGet();
            return "RESULT";
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Supplier<Mono<String>> countingLoader(AtomicInteger counter) {
        return () -> Mono.fromCallable(() -> {
            counter.incrementAndGet();
            return "RESULT";
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
