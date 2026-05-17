package org.hyeong.booe.property.api;

import org.hyeong.booe.property.dto.request.BuildingInfoReqDto;
import org.hyeong.booe.property.dto.response.BldRgstAreaItem;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest(properties = {
        "booe.crypto.ci.hmac-key=dGVzdC1obWFjLWtleS1mb3ItdGVzdC1vbmx5LXVzZS1wYWRkaW5n",
        "booe.crypto.ci.aes-key=dGVzdC1hZXMta2V5LXdpdGgtMzItYnl0ZS1wYWRkaW5nLWFiY2RlZg==",
        "api.public-data.service-key=mqbVrTExhyHssEcrak%2FHzS46eOEzi7aQK9%2B8kmpctOiotwNIyx6ItkxNOCBLT7NBR294hM29Jw0g5LAkIAYU%2Bw%3D%3D",
        "api.public-data.timeout=50000"
})
class ConcurrencyTuningTest {

    private static final Logger log = LoggerFactory.getLogger(ConcurrencyTuningTest.class);

    @Autowired
    private BldRgstApiClient client;

    // PNU 2818510600100040001
    private static final BuildingInfoReqDto QUERY = BuildingInfoReqDto.builder()
            .sigunguCd("28185")
            .bjdongCd("10600")
            .bun("0004")
            .ji("0001")
            .build();

    private static final int[] CONCURRENCY_LEVELS = {1, 4, 8, 16, 30, 50, 80, 120, 160};
    private static final long[] DELAY_LEVELS_MS = {0, 10, 25, 50, 100};
    private static final int REPEAT = 3;
    private static final long REST_BETWEEN_MS = 2000;

    @Test
    void scanConcurrency() {
        log.info("########## CONCURRENCY SCAN START (delay=50ms) ##########");
        List<Result> results = new ArrayList<>();
        for (int c : CONCURRENCY_LEVELS) {
            for (int i = 1; i <= REPEAT; i++) {
                results.add(measure(c, 50, i));
                sleep(REST_BETWEEN_MS);
            }
        }
        log.info("########## CONCURRENCY SCAN SUMMARY ##########");
        printSummary(results);
    }

    @Test
    void scanDelay() {
        log.info("########## DELAY SCAN START (concurrency=8) ##########");
        List<Result> results = new ArrayList<>();
        for (long d : DELAY_LEVELS_MS) {
            for (int i = 1; i <= REPEAT; i++) {
                results.add(measure(8, d, i));
                sleep(REST_BETWEEN_MS);
            }
        }
        log.info("########## DELAY SCAN SUMMARY ##########");
        printSummary(results);
    }

    private Result measure(int concurrency, long delayMs, int run) {
        long start = System.currentTimeMillis();
        try {
            List<BldRgstAreaItem> result = client.fetchAllAreaItemsForTuning(QUERY, concurrency, delayMs)
                    .block(Duration.ofMinutes(3));
            long elapsed = System.currentTimeMillis() - start;
            int size = result == null ? 0 : result.size();
            log.info(String.format("[TUNE] c=%3d d=%4dms run=%d elapsed=%6dms items=%5d OK",
                    concurrency, delayMs, run, elapsed, size));
            return new Result(concurrency, delayMs, run, elapsed, size, null);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error(String.format("[TUNE] c=%3d d=%4dms run=%d elapsed=%6dms FAIL: %s - %s",
                    concurrency, delayMs, run, elapsed,
                    e.getClass().getSimpleName(), e.getMessage()));
            return new Result(concurrency, delayMs, run, elapsed, 0, e.getClass().getSimpleName());
        }
    }

    private void printSummary(List<Result> results) {
        for (Result r : results) {
            String status = r.error == null ? "OK" : "FAIL(" + r.error + ")";
            log.info(String.format("  c=%3d d=%4dms run=%d  elapsed=%6dms  items=%5d  %s",
                    r.concurrency, r.delayMs, r.run, r.elapsed, r.items, status));
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private record Result(int concurrency, long delayMs, int run,
                          long elapsed, int items, String error) {}
}
