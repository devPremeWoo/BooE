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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PDF 생성 비용 특성화 + 동시성 saturation 스캔.
 *
 * <p>PDF 생성은 confirmPayment 동기 경로 안에 있고 CPU 바운드(OpenHTMLtoPDF 렌더 + 폰트 파싱)다.
 * 이 테스트는 (1) 단일 생성 latency 분포, (2) HTML 렌더 vs 전체 PDF 분해, (3) 동시성별 throughput/
 * latency 곡선을 측정해 "어디서 직렬화되어 병목이 되는가"를 숫자로 드러낸다. → [D] 비동기 분리 근거.
 *
 * <p>외부 의존성 0 — Thymeleaf 엔진을 직접 구성해 {@code new ContractPdfService(engine)}로 측정.
 * throughput 측정이므로 결과는 로그로 출력하고, 단언은 정상 동작(비어있지 않은 PDF·전건 성공)만 확인.
 */
@DisplayName("PDF 생성 부하 — 비용 특성화 & saturation 스캔")
class PdfGenerationLoadTest {

    private static final Logger log = LoggerFactory.getLogger(PdfGenerationLoadTest.class);

    private static final int WARMUP = 5;            // JIT + 첫 폰트 로딩 비용 제외
    private static final int SAMPLES = 30;          // 단일 latency 표본
    private static final int[] CONCURRENCY = {1, 2, 4, 8, 16, 32};
    private static final int TASKS_PER_LEVEL = 32;

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
    @DisplayName("단일 스레드 latency 분포 + HTML 렌더 vs 전체 PDF 분해")
    void singleThreadLatencyBreakdown() {
        warmup();

        long[] htmlMicros = measure(SAMPLES, () -> service.renderContractHtml(dto));
        long[] pdfMicros = measure(SAMPLES, () -> {
            try {
                return service.generatePdf(dto);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        log.info("[PDF-LAT] HTML 렌더(Thymeleaf)  p50={}ms p95={}ms p99={}ms avg={}ms",
                ms(p(htmlMicros, 50)), ms(p(htmlMicros, 95)), ms(p(htmlMicros, 99)), ms(avg(htmlMicros)));
        log.info("[PDF-LAT] 전체 PDF 생성         p50={}ms p95={}ms p99={}ms avg={}ms",
                ms(p(pdfMicros, 50)), ms(p(pdfMicros, 95)), ms(p(pdfMicros, 99)), ms(avg(pdfMicros)));
        long renderShare = avg(pdfMicros) - avg(htmlMicros);
        log.info("[PDF-LAT] → PDF 렌더+폰트 부분(전체-HTML) avg={}ms ({}%)",
                ms(renderShare), pct(renderShare, avg(pdfMicros)));

        assertThat(avg(pdfMicros)).as("전체 PDF가 HTML 렌더보다 비쌈").isGreaterThan(avg(htmlMicros));
    }

    @Test
    @DisplayName("동시성 스캔 — throughput / latency saturation 지점 + 실패 지점 탐지")
    void concurrencyScan() throws InterruptedException {
        warmup();
        log.info("########## PDF CONCURRENCY SCAN (tasks/level={}) ##########", TASKS_PER_LEVEL);

        int sequentialFailures = -1;
        for (int c : CONCURRENCY) {
            int fails = scanLevel(c);
            if (c == 1) sequentialFailures = fails;
        }

        // 측정 테스트: 고동시성 실패는 saturation 신호로 로그만 남긴다. 단, 순차(c=1)는 안정해야 한다.
        assertThat(sequentialFailures).as("순차(c=1) 생성은 전건 성공해야 함").isZero();
    }

    /** 한 동시성 레벨을 측정하고 실패 건수를 반환. 실패 원인은 Throwable까지 잡아 로그. */
    private int scanLevel(int concurrency) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        List<Future<Long>> futures = new ArrayList<>(TASKS_PER_LEVEL);
        List<Throwable> errors = java.util.Collections.synchronizedList(new ArrayList<>());

        long wallStart = System.nanoTime();
        for (int i = 0; i < TASKS_PER_LEVEL; i++) {
            futures.add(pool.submit(() -> {
                long t0 = System.nanoTime();
                try {
                    byte[] pdf = service.generatePdf(dto);
                    if (pdf == null || pdf.length == 0) errors.add(new IllegalStateException("empty pdf"));
                } catch (Throwable e) {           // Error(OOM 등)까지 포착
                    errors.add(e);
                }
                return (System.nanoTime() - t0) / 1_000;   // micros
            }));
        }

        long[] latMicros = new long[TASKS_PER_LEVEL];
        for (int i = 0; i < futures.size(); i++) {
            try {
                latMicros[i] = futures.get(i).get(2, TimeUnit.MINUTES);
            } catch (Exception e) {
                errors.add(e);
            }
        }
        long wallMs = (System.nanoTime() - wallStart) / 1_000_000;
        pool.shutdownNow();

        double throughput = TASKS_PER_LEVEL / (wallMs / 1000.0);
        log.info(String.format(
                "[PDF-SCAN] c=%2d  wall=%6dms  throughput=%5.1f PDF/s  latency(avg/p95/p99)=%s/%s/%sms  fail=%d",
                concurrency, wallMs, throughput,
                ms(avg(latMicros)), ms(p(latMicros, 95)), ms(p(latMicros, 99)), errors.size()));
        if (!errors.isEmpty()) {
            Throwable sample = errors.get(0);
            log.warn("[PDF-SCAN] c={} 실패 {}건 — 대표 원인: {}: {}",
                    concurrency, errors.size(), sample.getClass().getName(), sample.getMessage());
        }
        return errors.size();
    }

    // ── 측정 유틸 ─────────────────────────────────────────────────────────

    private void warmup() {
        for (int i = 0; i < WARMUP; i++) {
            try {
                service.generatePdf(dto);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /** action을 count회 실행하며 각 호출의 소요시간(micros)을 정렬해 반환. */
    private long[] measure(int count, java.util.function.Supplier<?> action) {
        long[] micros = new long[count];
        for (int i = 0; i < count; i++) {
            long t0 = System.nanoTime();
            action.get();
            micros[i] = (System.nanoTime() - t0) / 1_000;
        }
        java.util.Arrays.sort(micros);
        return micros;
    }

    private long p(long[] sortedMicros, int percentile) {
        int idx = (int) Math.ceil(percentile / 100.0 * sortedMicros.length) - 1;
        return sortedMicros[Math.max(0, Math.min(idx, sortedMicros.length - 1))];
    }

    private long avg(long[] micros) {
        long sum = 0;
        for (long v : micros) sum += v;
        return micros.length == 0 ? 0 : sum / micros.length;
    }

    private String ms(long micros) {
        return String.format("%.1f", micros / 1000.0);
    }

    private long pct(long part, long whole) {
        return whole == 0 ? 0 : Math.round(part * 100.0 / whole);
    }
}
