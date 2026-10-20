package org.hyeong.booe.contract.service;

import org.hyeong.booe.contract.ContractSampleDtoFixture;
import org.hyeong.booe.contract.dto.req.ContractBaseReqDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 바운드 풀(PdfExecutorConfig 흉내: N=4, 큐 100, AbortPolicy)을 거쳐 PDF를 생성할 때의
 * 동시성별 throughput / 완료 latency / OOM / 거절 스캔.
 *
 * <p>기존 {@code PdfGenerationLoadTest.concurrencyScan}은 동시성 c만큼 스레드를 만들어 generatePdf를
 * "직접" 때렸다(= 무제한 동시 진입 → c=32 OOM). 본 테스트는 똑같은 generatePdf를 <b>고정 4워커 + 큐 100</b>
 * 풀로 보내, 들어오는 버스트 크기를 1→200으로 올려도 (1) OOM이 사라지고 (2) throughput이 4워커 천장에
 * 고정되며 (3) 완료 latency는 큐 대기로 증가하고 (4) 버스트가 풀+큐(104)를 넘으면 거절되는지 측정한다.
 *
 * <p>latency는 <b>제출→완료</b>(큐 대기 포함)로 잰다 — 큐에 쌓인 작업의 실제 체감 비용이기 때문.
 */
@DisplayName("바운드 풀(4/큐100) 경유 PDF 동시성 스캔")
class PdfPoolScanLoadTest {

    private static final Logger log = LoggerFactory.getLogger(PdfPoolScanLoadTest.class);

    private static final int WARMUP = 5;
    private static final int[] BURST = {1, 2, 4, 8, 16, 32, 64, 128, 200};

    private ContractPdfService service;
    private ContractBaseReqDto dto;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        service = new ContractPdfService(engine);
        dto = ContractSampleDtoFixture.build();
    }

    @Test
    @DisplayName("버스트 1→200을 4워커+큐100 풀로 — OOM/거절/throughput/완료latency")
    void poolScan() throws Exception {
        warmup();
        log.info("########## PDF POOL SCAN (workers=4, queue=100, AbortPolicy) ##########");

        for (int burst : BURST) {
            // PdfExecutorConfig 와 동일 구성의 풀을 레벨마다 새로 생성
            ThreadPoolExecutor pool = new ThreadPoolExecutor(
                    4, 4, 0L, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(100), new ThreadPoolExecutor.AbortPolicy());

            List<Future<Long>> futures = new ArrayList<>(burst);
            AtomicInteger rejected = new AtomicInteger();
            AtomicInteger oom = new AtomicInteger();

            long wallStart = System.nanoTime();
            for (int i = 0; i < burst; i++) {
                final long tSubmit = System.nanoTime();
                try {
                    futures.add(pool.submit(() -> {
                        service.generatePdf(dto);                       // 실제 렌더
                        return (System.nanoTime() - tSubmit) / 1_000;   // 제출→완료 micros
                    }));
                } catch (RejectedExecutionException e) {
                    rejected.incrementAndGet();
                }
            }

            List<Long> lat = new ArrayList<>(futures.size());
            for (Future<Long> f : futures) {
                try {
                    lat.add(f.get(2, TimeUnit.MINUTES));
                } catch (Throwable t) {
                    if (t.getCause() instanceof OutOfMemoryError || t instanceof OutOfMemoryError) oom.incrementAndGet();
                    else oom.incrementAndGet(); // 워커 내부 실패 일괄 집계
                }
            }
            long wallMs = (System.nanoTime() - wallStart) / 1_000_000;
            pool.shutdownNow();

            long[] sorted = lat.stream().mapToLong(Long::longValue).sorted().toArray();
            int accepted = burst - rejected.get();
            double throughput = accepted / (wallMs / 1000.0);

            log.info(String.format(
                    "[POOL-SCAN] burst=%3d  accepted=%3d  rejected=%3d  wall=%6dms  throughput=%5.1f PDF/s  " +
                    "완료lat(avg/p95)=%s/%sms  OOM=%d",
                    burst, accepted, rejected.get(), wallMs, throughput,
                    ms(avg(sorted)), ms(p(sorted, 95)), oom.get()));
        }
    }

    private void warmup() {
        for (int i = 0; i < WARMUP; i++) {
            try { service.generatePdf(dto); } catch (Exception e) { throw new RuntimeException(e); }
        }
    }

    private long avg(long[] m) {
        long s = 0; for (long v : m) s += v; return m.length == 0 ? 0 : s / m.length;
    }

    private long p(long[] sorted, int pct) {
        if (sorted.length == 0) return 0;
        int idx = (int) Math.ceil(pct / 100.0 * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(idx, sorted.length - 1))];
    }

    private String ms(long micros) { return String.format("%.1f", micros / 1000.0); }
}
