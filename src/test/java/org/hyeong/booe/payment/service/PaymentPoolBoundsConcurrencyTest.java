package org.hyeong.booe.payment.service;

import org.hyeong.booe.contract.ContractSampleDtoFixture;
import org.hyeong.booe.contract.domain.Contract;
import org.hyeong.booe.contract.dto.req.ContractBaseReqDto;
import org.hyeong.booe.contract.repository.ContractRepository;
import org.hyeong.booe.contract.service.ContractPdfService;
import org.hyeong.booe.contract.service.ContractPdfStorageService;
import org.hyeong.booe.global.fcm.FcmService;
import org.hyeong.booe.member.domain.Member;
import org.hyeong.booe.member.repository.MemberRepository;
import org.hyeong.booe.payment.api.TossPaymentApiClient;
import org.hyeong.booe.payment.dto.PaymentOrderInfo;
import org.hyeong.booe.payment.dto.reqeust.PaymentConfirmReqDto;
import org.hyeong.booe.payment.dto.response.TossPaymentConfirmResDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * PDF 비동기 풀(N=4, 큐 100, AbortPolicy)이 동시 결제 부하에서 실제로 동시성을 캡해
 * OOM을 방지하는지 검증.
 *
 * <p>같은 축의 baseline ({@code PdfGenerationLoadTest} c=32, 폰트 캐싱 후):
 * <ul>
 *   <li>직접 동기 호출(c=32 동시): {@code OutOfMemoryError} **7건**, 평균 latency 518ms
 * </ul>
 *
 * <p>본 테스트:
 * <ul>
 *   <li>동시 32건 {@code confirmPayment} 발사
 *   <li>PDF는 cheap stub 아닌 <b>실제 렌더</b>(realPdf delegate)가 풀 경유로 실행
 *   <li>기대: 풀이 N=4로 캡 → 0 OOM, 전건 PDF 완료, dispatch 지연 짧음
 * </ul>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "booe.crypto.ci.hmac-key=dGVzdC1obWFjLWtleS1mb3ItdGVzdC1vbmx5LXVzZS1wYWRkaW5n",
                "booe.crypto.ci.aes-key=dGVzdC1hZXMta2V5LXdpdGgtMzItYnl0ZS1wYWRkaW5nLWFiY2RlZg=="
        })
@ActiveProfiles("test")
@DisplayName("풀 경유 동시 32 confirmPayment — OOM 방지·전건 PDF 완료")
class PaymentPoolBoundsConcurrencyTest {

    private static final Logger log = LoggerFactory.getLogger(PaymentPoolBoundsConcurrencyTest.class);

    private static final int CONCURRENT = 32;
    private static final long AMOUNT = 30_000L;
    private static final long DRAIN_TIMEOUT_MS = 60_000;

    @Autowired PaymentService paymentService;
    @Autowired PaymentOrderRedisService paymentOrderRedisService;
    @Autowired MemberRepository memberRepository;
    @Autowired ContractRepository contractRepository;
    @Autowired RedisTemplate<String, Object> redisTemplate;
    @Autowired JdbcTemplate jdbcTemplate;

    @MockBean TossPaymentApiClient tossPaymentApiClient;
    @MockBean ContractPdfService contractPdfService;
    @MockBean ContractPdfStorageService contractPdfStorageService;
    @MockBean FcmService fcmService;

    /** 실제 렌더링용. mock generatePdf가 이쪽으로 위임 → 풀이 진짜 PDF 부하를 받음. */
    private final ContractPdfService realPdf = new ContractPdfService(buildEngine());
    private final ContractBaseReqDto sampleDto = ContractSampleDtoFixture.build();

    private Long lessorId;
    private Long lesseeId;
    private final List<Long> createdContractIds = new ArrayList<>();
    private final List<String> createdPaymentKeys = new ArrayList<>();
    private final AtomicInteger pdfSaveCount = new AtomicInteger();
    private final AtomicInteger workerFailures = new AtomicInteger();
    private List<PaymentConfirmReqDto> dtos;

    @BeforeEach
    void setUp() throws IOException {
        long nonce = System.nanoTime();
        lessorId = memberRepository.save(Member.builder().memberCode("pool-lessor-" + nonce).build()).getId();
        lesseeId = memberRepository.save(Member.builder().memberCode("pool-lessee-" + nonce).build()).getId();

        given(tossPaymentApiClient.confirmPayment(anyString(), anyString(), anyLong()))
                .willReturn(tossConfirmResponse());

        // 실제 PDF 렌더링: form_json 라운드트립 이슈 회피 위해 sampleDto 사용. 워커 OOM/실패 카운트.
        given(contractPdfService.generatePdf(any())).willAnswer(inv -> {
            try {
                return realPdf.generatePdf(sampleDto);
            } catch (Throwable t) {
                workerFailures.incrementAndGet();
                throw t;
            }
        });
        // 저장은 디스크 I/O 제외, 완료 카운터만
        given(contractPdfStorageService.save(anyLong(), any())).willAnswer(inv -> {
            pdfSaveCount.incrementAndGet();
            return "path/contract.pdf";
        });
        given(contractPdfStorageService.pathFor(anyLong())).willReturn("path/contract.pdf");

        // 32건 사전 셋업 (계약 + form_data + Redis order + 고유 paymentKey)
        dtos = new ArrayList<>(CONCURRENT);
        for (int i = 0; i < CONCURRENT; i++) {
            dtos.add(prepareFreshPayment());
        }
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        Thread.sleep(500); // 워커 완전 drain
        for (String pk : createdPaymentKeys) {
            jdbcTemplate.update(
                    "DELETE FROM payment_event WHERE payment_id IN (SELECT id FROM payment WHERE payment_key = ?)", pk);
            jdbcTemplate.update("DELETE FROM payment WHERE payment_key = ?", pk);
            redisTemplate.delete("payment:confirm:" + pk);
        }
        for (Long cid : createdContractIds) {
            jdbcTemplate.update("DELETE FROM contract_form_data WHERE contract_id = ?", cid);
            jdbcTemplate.update("DELETE FROM contract WHERE id = ?", cid);
            redisTemplate.delete("payment:order:" + cid);
        }
        jdbcTemplate.update("DELETE FROM member WHERE id IN (?, ?)", lessorId, lesseeId);
    }

    @Test
    @DisplayName("동시 32건 confirmPayment → 풀 N=4 캡 → 0 OOM, 전건 PDF 저장, dispatch 짧음")
    void poolCapsPdfConcurrencyAndPreventsOom() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(CONCURRENT);
        long[] latencyMicros = new long[CONCURRENT];
        AtomicInteger callerErrors = new AtomicInteger();

        long wallStart = System.nanoTime();
        for (int i = 0; i < CONCURRENT; i++) {
            int idx = i;
            pool.submit(() -> {
                try {
                    start.await();
                    long t0 = System.nanoTime();
                    paymentService.confirmPayment(dtos.get(idx), lesseeId);
                    latencyMicros[idx] = (System.nanoTime() - t0) / 1_000;
                } catch (Throwable t) {
                    callerErrors.incrementAndGet();
                    log.warn("[POOL] confirmPayment 호출자 예외 idx={}", idx, t);
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        boolean dispatched = done.await(30, TimeUnit.SECONDS);
        long dispatchWallMs = (System.nanoTime() - wallStart) / 1_000_000;
        pool.shutdownNow();

        // 워커 풀 drain — 모든 PDF가 실제로 저장 완료될 때까지 대기
        long drainDeadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(DRAIN_TIMEOUT_MS);
        long drainStart = System.nanoTime();
        while (pdfSaveCount.get() < CONCURRENT && System.nanoTime() < drainDeadline) {
            Thread.sleep(50);
        }
        long drainMs = (System.nanoTime() - drainStart) / 1_000_000;
        long totalWallMs = dispatchWallMs + drainMs;

        long[] sortedLat = latencyMicros.clone();
        Arrays.sort(sortedLat);

        log.info("[POOL] 동시 confirmPayment {}건 결과:", CONCURRENT);
        log.info("[POOL]   dispatch wall={}ms  drain wall={}ms  total={}ms",
                dispatchWallMs, drainMs, totalWallMs);
        log.info("[POOL]   confirmPayment per-call  avg={}ms  p95={}ms  max={}ms",
                ms(avg(sortedLat)), ms(p(sortedLat, 95)), ms(sortedLat[sortedLat.length - 1]));
        log.info("[POOL]   호출자 예외(거절 포함)={}  워커 내부 실패(OOM 등)={}  PDF 저장 완료={}/{}",
                callerErrors.get(), workerFailures.get(), pdfSaveCount.get(), CONCURRENT);
        log.info("[POOL] 같은 축 baseline (PdfGenerationLoadTest c=32, 폰트 캐싱 후):");
        log.info("[POOL]   직접 동기 호출           OOM 7건, latency avg=518ms");
        log.info("[POOL]   풀 경유 (이 테스트)      OOM {}건, dispatch avg={}ms",
                workerFailures.get(), ms(avg(sortedLat)));

        assertThat(dispatched).as("모든 dispatch 완료").isTrue();
        assertThat(callerErrors.get()).as("호출자 예외 0 (큐 100이라 거절 없음)").isZero();
        assertThat(workerFailures.get()).as("워커 OOM/실패 0 (풀이 동시성 캡)").isZero();
        assertThat(pdfSaveCount.get()).as("전건 PDF 저장").isEqualTo(CONCURRENT);
        verify(contractPdfStorageService, times(CONCURRENT)).save(anyLong(), any());
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private PaymentConfirmReqDto prepareFreshPayment() {
        long nonce = System.nanoTime();
        String orderId = "ORDER-POOL-" + nonce;
        String paymentKey = "PK-POOL-" + nonce;

        Member lessor = memberRepository.findById(lessorId).orElseThrow();
        Member lessee = memberRepository.findById(lesseeId).orElseThrow();
        Contract contract = contractRepository.save(buildPaymentPendingContract(lessor, lessee));
        Long contractId = contract.getId();
        jdbcTemplate.update(
                "INSERT INTO contract_form_data (contract_id, form_json) VALUES (?, ?)", contractId, "{}");
        paymentOrderRedisService.saveOrderInfo(contractId, new PaymentOrderInfo(orderId, AMOUNT));

        createdContractIds.add(contractId);
        createdPaymentKeys.add(paymentKey);
        return buildConfirmDto(contractId, orderId, paymentKey);
    }

    private Contract buildPaymentPendingContract(Member lessor, Member lessee) {
        ContractBaseReqDto dto = new ContractBaseReqDto();
        ContractBaseReqDto.AddressInfo addressInfo = new ContractBaseReqDto.AddressInfo();
        ReflectionTestUtils.setField(addressInfo, "address", "서울시 강남구 테헤란로 123");
        ReflectionTestUtils.setField(dto, "addressInfo", addressInfo);

        Contract contract = Contract.createContract(lessor, dto);
        contract.requestReview(lessee);
        contract.confirmByLessor("홍임대", "01011112222");
        return contract;
    }

    private PaymentConfirmReqDto buildConfirmDto(Long contractId, String orderId, String paymentKey) {
        PaymentConfirmReqDto dto = new PaymentConfirmReqDto();
        ReflectionTestUtils.setField(dto, "contractId", contractId);
        ReflectionTestUtils.setField(dto, "paymentKey", paymentKey);
        ReflectionTestUtils.setField(dto, "orderId", orderId);
        ReflectionTestUtils.setField(dto, "amount", AMOUNT);
        return dto;
    }

    private TossPaymentConfirmResDto tossConfirmResponse() {
        TossPaymentConfirmResDto res = new TossPaymentConfirmResDto();
        ReflectionTestUtils.setField(res, "status", "DONE");
        ReflectionTestUtils.setField(res, "method", "간편결제");
        ReflectionTestUtils.setField(res, "approvedAt", "2026-05-27T10:00:00+09:00");
        return res;
    }

    private static SpringTemplateEngine buildEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private long avg(long[] micros) {
        long sum = 0;
        for (long v : micros) sum += v;
        return micros.length == 0 ? 0 : sum / micros.length;
    }

    private long p(long[] sortedMicros, int percentile) {
        int idx = (int) Math.ceil(percentile / 100.0 * sortedMicros.length) - 1;
        return sortedMicros[Math.max(0, Math.min(idx, sortedMicros.length - 1))];
    }

    private String ms(long micros) {
        return String.format("%.1f", micros / 1000.0);
    }
}
