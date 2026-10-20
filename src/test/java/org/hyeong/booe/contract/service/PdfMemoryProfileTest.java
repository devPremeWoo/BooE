package org.hyeong.booe.contract.service;

import com.sun.management.ThreadMXBean;
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

import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PDF 생성 메모리 프로파일.
 *
 * <ul>
 *   <li>A. 1장당 할당 바이트 분해: 전체 generatePdf / HTML 렌더만 / 폰트 read(2회) — 폰트가 차지하는 비중을 정량화.
 *       {@link ThreadMXBean#getCurrentThreadAllocatedBytes()}로 측정 — 스레드 누적 할당 카운터라 GC 영향 X.
 *   <li>B. 동시성 × peak 힙 working set: 1, 2, 4, 8, 16개 동시 생성 시 한 시점에 살아있는 힙 피크를
 *       5ms 주기로 샘플링. 곡선이 동시성에 어떻게 증가하는지 → OOM 임계 예측, 향후 바운드 풀 N 결정 근거.
 * </ul>
 *
 * <p>외부 의존성 0. throughput 아닌 메모리 측정이므로 결과는 로그로 출력하고 단언은 sanity만.
 */
@DisplayName("PDF 생성 메모리 프로파일")
class PdfMemoryProfileTest {

    private static final Logger log = LoggerFactory.getLogger(PdfMemoryProfileTest.class);

    private static final int WARMUP = 5;
    private static final int ALLOC_SAMPLES = 20;
    private static final int[] CONCURRENCY = {1, 2, 4, 8, 16};

    private ContractPdfService service;
    private ContractBaseReqDto dto;
    private ThreadMXBean threadBean;

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

        threadBean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!threadBean.isThreadAllocatedMemorySupported()) {
            throw new IllegalStateException("ThreadAllocatedMemory 미지원 JVM");
        }
        threadBean.setThreadAllocatedMemoryEnabled(true);
    }

    @Test
    @DisplayName("A. 1장당 할당 바이트 분해 — 전체 vs HTML vs 폰트")
    void allocationBreakdown() {
        warmup();

        long perPdf = avgAllocPerCall(ALLOC_SAMPLES, () -> {
            try { service.generatePdf(dto); } catch (Exception e) { throw new RuntimeException(e); }
        });
        long perHtml = avgAllocPerCall(ALLOC_SAMPLES, () -> service.renderContractHtml(dto));
        long perFontRead = avgAllocPerCall(ALLOC_SAMPLES, this::readFontBytesTwice);

        long renderPlusFont = perPdf - perHtml;
        log.info("[PDF-MEM] 전체 PDF 1장        alloc avg={} MB", mb(perPdf));
        log.info("[PDF-MEM]   ├─ HTML 렌더 only       {} MB ({}%)", mb(perHtml), pct(perHtml, perPdf));
        log.info("[PDF-MEM]   └─ PDF 렌더+폰트 부분   {} MB ({}%)", mb(renderPlusFont), pct(renderPlusFont, perPdf));
        log.info("[PDF-MEM] 폰트 단순 read 2회       {} MB ({}% of 전체)",
                mb(perFontRead), pct(perFontRead, perPdf));

        assertThat(perPdf).as("전체 PDF가 HTML보다 많이 할당").isGreaterThan(perHtml);
    }

    @Test
    @DisplayName("B. 동시성 × peak/retained 힙 working set")
    void heapUnderConcurrency() throws InterruptedException {
        warmup();
        long maxHeapMb = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        log.info("########## PDF HEAP UNDER CONCURRENCY (-Xmx={}MB) ##########", maxHeapMb);

        for (int c : CONCURRENCY) {
            measureHeapAt(c);
        }
    }

    /** 동시성 c로 c*4개 PDF를 돌리면서, sampler 스레드가 5ms 주기로 used-heap을 찍어 peak를 추적. */
    private void measureHeapAt(int concurrency) throws InterruptedException {
        int tasks = concurrency * 4;
        forceGcAndSettle();
        long baselineUsed = usedHeap();

        AtomicLong peakUsed = new AtomicLong(baselineUsed);
        AtomicBoolean running = new AtomicBoolean(true);
        Thread sampler = new Thread(() -> {
            while (running.get()) {
                long u = usedHeap();
                peakUsed.updateAndGet(v -> Math.max(v, u));
                try { Thread.sleep(5); } catch (InterruptedException e) { return; }
            }
        }, "heap-sampler");
        sampler.setDaemon(true);
        sampler.start();

        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        CountDownLatch done = new CountDownLatch(tasks);
        AtomicInteger failures = new AtomicInteger();

        long wallStart = System.nanoTime();
        for (int i = 0; i < tasks; i++) {
            pool.submit(() -> {
                try {
                    service.generatePdf(dto);
                } catch (Throwable t) {
                    failures.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        boolean finished = done.await(2, TimeUnit.MINUTES);
        long wallMs = (System.nanoTime() - wallStart) / 1_000_000;
        running.set(false);
        pool.shutdownNow();
        sampler.join(500);

        long peakDelta = peakUsed.get() - baselineUsed;
        forceGcAndSettle();
        long retainedDelta = Math.max(0, usedHeap() - baselineUsed);

        log.info(String.format(
                "[PDF-MEM] c=%2d tasks=%2d wall=%5dms  peakΔ=%4dMB  retainedΔ=%3dMB  fail=%d  finished=%s",
                concurrency, tasks, wallMs, peakDelta / 1024 / 1024, retainedDelta / 1024 / 1024,
                failures.get(), finished));
    }

    // ── 측정 유틸 ─────────────────────────────────────────────────────────

    /** generatePdf 내부와 동일하게 malgun.ttf 스트림을 두 번 읽기만 함 (등록 비용 제외, 순수 I/O+버퍼). */
    private void readFontBytesTwice() {
        try {
            try (InputStream is = getClass().getResourceAsStream("/fonts/malgun.ttf")) {
                if (is == null) throw new IllegalStateException("malgun.ttf 없음");
                is.readAllBytes();
            }
            try (InputStream is = getClass().getResourceAsStream("/fonts/malgun.ttf")) {
                if (is == null) throw new IllegalStateException("malgun.ttf 없음");
                is.readAllBytes();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** ThreadMXBean 누적 카운터로 action 1회 평균 할당 바이트를 측정. */
    private long avgAllocPerCall(int samples, Runnable action) {
        action.run(); // 첫 호출 노이즈 흡수
        long total = 0;
        long tid = Thread.currentThread().getId();
        for (int i = 0; i < samples; i++) {
            long before = threadBean.getThreadAllocatedBytes(tid);
            action.run();
            long after = threadBean.getThreadAllocatedBytes(tid);
            total += (after - before);
        }
        return total / samples;
    }

    private void warmup() {
        for (int i = 0; i < WARMUP; i++) {
            try { service.generatePdf(dto); } catch (Exception e) { throw new RuntimeException(e); }
        }
    }

    private void forceGcAndSettle() {
        System.gc();
        try { Thread.sleep(150); } catch (InterruptedException ignored) {}
        System.gc();
        try { Thread.sleep(150); } catch (InterruptedException ignored) {}
    }

    private long usedHeap() {
        Runtime r = Runtime.getRuntime();
        return r.totalMemory() - r.freeMemory();
    }

    private String mb(long bytes) {
        return String.format("%.1f", bytes / 1024.0 / 1024.0);
    }

    private long pct(long part, long whole) {
        return whole == 0 ? 0 : Math.round(part * 100.0 / whole);
    }
}
