package org.hyeong.booe.contract.service;

import jdk.jfr.Configuration;
import jdk.jfr.Recording;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * PDF 생성 시점의 CPU 샘플링·할당 hotspot·GC 패턴을 Java Flight Recorder로 캡처.
 *
 * <p>실행 후 {@code build/pdf-jfr/pdf-profile.jfr} 생성. 분석은 JMC(JDK Mission Control)로:
 * <ol>
 *   <li>JMC 다운로드 — https://www.oracle.com/java/technologies/jdk-mission-control.html
 *   <li>JMC 실행 → File → Open File → {@code build/pdf-jfr/pdf-profile.jfr}
 *   <li>핵심 탭:
 *     <ul>
 *       <li><b>Memory → Allocations</b>: 1장당 41MB의 dominant 클래스 (폰트? 출력 버퍼? PDFBox 내부?)
 *       <li><b>Method Profiling → Hot Methods</b>: CPU 시간 가장 많이 쓰는 메서드
 *       <li><b>Garbage Collections</b>: 부하 동안 GC 빈도·pause 분포
 *       <li><b>Threads → Java Application</b>: 워커 스레드 상태(Running/Blocked/Sleeping) 분포
 *     </ul>
 * </ol>
 *
 * <p>JMC 없이도 가능한 CLI 확인:
 * <pre>
 *   jfr summary build/pdf-jfr/pdf-profile.jfr        # 이벤트 종류·개수
 *   jfr print --events jdk.ObjectAllocationInNewTLAB build/pdf-jfr/pdf-profile.jfr | head -50
 * </pre>
 */
@DisplayName("PDF 생성 JFR 프로파일 캡처")
class PdfGenerationJfrProfileTest {

    private static final Logger log = LoggerFactory.getLogger(PdfGenerationJfrProfileTest.class);
    private static final Path JFR_DIR = Path.of("build", "pdf-jfr");
    private static final Path JFR_FILE = JFR_DIR.resolve("pdf-profile.jfr");

    private static final int WARMUP = 5;
    private static final int SINGLE_SAMPLES = 30;
    private static final int CONCURRENCY = 4;
    private static final int CONCURRENT_TASKS = 32;

    private ContractPdfService service;
    private ContractBaseReqDto dto;

    @BeforeEach
    void setUp() throws Exception {
        Files.createDirectories(JFR_DIR);
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
    @DisplayName("단일 + 동시 PDF 생성을 JFR profile 프리셋으로 캡처")
    void captureJfrProfile() throws Exception {
        // 워밍업은 JFR 밖에서 — JIT 컴파일 노이즈 분리
        for (int i = 0; i < WARMUP; i++) {
            service.generatePdf(dto);
        }

        Configuration profile = Configuration.getConfiguration("profile");
        try (Recording recording = new Recording(profile)) {
            recording.setName("pdf-profile");
            recording.setDestination(JFR_FILE);
            recording.start();
            log.info("[JFR] 녹화 시작 (profile preset) → {}", JFR_FILE.toAbsolutePath());

            // 1) 단일 스레드 N회 — 단일 PDF의 할당/CPU 패턴
            long t0 = System.nanoTime();
            for (int i = 0; i < SINGLE_SAMPLES; i++) {
                service.generatePdf(dto);
            }
            long singleMs = (System.nanoTime() - t0) / 1_000_000;
            log.info("[JFR] 단일 스레드 {}회 완료: {}ms", SINGLE_SAMPLES, singleMs);

            // 2) 동시 c=4로 32 작업 — 풀 상황 시뮬레이션
            ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY);
            List<Future<?>> futures = new ArrayList<>();
            long t1 = System.nanoTime();
            for (int i = 0; i < CONCURRENT_TASKS; i++) {
                futures.add(pool.submit(() -> {
                    try {
                        service.generatePdf(dto);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }));
            }
            for (Future<?> f : futures) f.get(2, TimeUnit.MINUTES);
            long concurrentMs = (System.nanoTime() - t1) / 1_000_000;
            pool.shutdownNow();
            log.info("[JFR] 동시 c={} × {}작업 완료: {}ms", CONCURRENCY, CONCURRENT_TASKS, concurrentMs);

            recording.stop();
        }

        log.info("[JFR] 녹화 완료. JMC에서 열기:");
        log.info("[JFR]   {}", JFR_FILE.toAbsolutePath());
        log.info("[JFR] 빠른 CLI 확인:  jfr summary {}", JFR_FILE);
    }
}
