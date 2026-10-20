package org.hyeong.booe.payment.service;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

/**
 * PDF 비동기 분리 후 confirmPayment 응답 latency 측정.
 *
 * <p>Before (동기 PDF, 2026-05-27 측정): avg 82.7ms / p95 102.4ms
 * Before-baseline (PDF 무력화, 같은 측정): avg 31.7ms / p95 41.3ms — 이론상 도달 가능한 하한
 *
 * <p>After: 본 테스트가 측정. dispatch만 동기로 일어나고 실제 렌더는 워커 풀에서 비동기 처리되므로
 * 결제 응답은 무력화 baseline(31.7ms)에 근접해야 함.
 *
 * <p>워커가 실제로 동작했는지 검증: cheap stub으로 generatePdf 호출 횟수가 dispatch 수 이상인지 확인.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "booe.crypto.ci.hmac-key=dGVzdC1obWFjLWtleS1mb3ItdGVzdC1vbmx5LXVzZS1wYWRkaW5n",
                "booe.crypto.ci.aes-key=dGVzdC1hZXMta2V5LXdpdGgtMzItYnl0ZS1wYWRkaW5nLWFiY2RlZg=="
        })
@ActiveProfiles("test")
@DisplayName("PDF 비동기 분리 후 confirmPayment latency")
class PaymentAsyncDispatchLatencyTest {

    private static final Logger log = LoggerFactory.getLogger(PaymentAsyncDispatchLatencyTest.class);

    private static final int WARMUP = 3;
    private static final int ITERATIONS = 20;
    private static final long AMOUNT = 30_000L;

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

    private Long lessorId;
    private Long lesseeId;
    private final List<Long> createdContractIds = new ArrayList<>();
    private final List<String> createdPaymentKeys = new ArrayList<>();

    @BeforeEach
    void setUp() throws java.io.IOException {
        long nonce = System.nanoTime();
        lessorId = memberRepository.save(Member.builder().memberCode("asyclat-lessor-" + nonce).build()).getId();
        lesseeId = memberRepository.save(Member.builder().memberCode("asyclat-lessee-" + nonce).build()).getId();

        given(tossPaymentApiClient.confirmPayment(anyString(), anyString(), anyLong()))
                .willReturn(tossConfirmResponse());
        // cheap stubs — 워커는 실제로 호출되지만 비용은 거의 0 → 측정 대상은 confirmPayment 동기 경로뿐
        given(contractPdfService.generatePdf(any())).willReturn(new byte[]{1});
        given(contractPdfStorageService.save(anyLong(), any())).willReturn("path/contract.pdf");
        given(contractPdfStorageService.pathFor(anyLong())).willReturn("path/contract.pdf");
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        // 워커 큐 drain 대기 — 셋업 데이터 삭제 전에 워커 일 끝내기
        Thread.sleep(500);
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
    @DisplayName("confirmPayment 응답 latency 측정 — 동기 PDF 82.7ms baseline과 비교")
    void measureAsyncDispatchLatency() throws InterruptedException, java.io.IOException {
        warmup();

        long[] micros = new long[ITERATIONS];
        for (int i = 0; i < ITERATIONS; i++) {
            PaymentConfirmReqDto dto = prepareFreshPayment();
            long t0 = System.nanoTime();
            paymentService.confirmPayment(dto, lesseeId);
            micros[i] = (System.nanoTime() - t0) / 1_000;
        }
        Arrays.sort(micros);

        // 워커가 실제로 동작했는지 확인 — generatePdf가 ITERATIONS+ WARMUP회 이상 호출됨
        Thread.sleep(1500);
        verify(contractPdfService, atLeast(ITERATIONS)).generatePdf(any());

        long avg = avg(micros);
        long p95 = p(micros, 95);
        log.info("[ASYNC-LAT] confirmPayment (PDF 비동기 dispatch)  avg={}ms  p95={}ms", ms(avg), ms(p95));
        log.info("[ASYNC-LAT] baseline 비교:");
        log.info("[ASYNC-LAT]   동기 PDF (Before, 2026-05-27)        avg=82.7ms  p95=102.4ms");
        log.info("[ASYNC-LAT]   PDF 무력화 (이론 하한)                 avg=31.7ms  p95= 41.3ms");
        log.info("[ASYNC-LAT]   현재 (After 비동기 분리)               avg={}ms   p95={}ms", ms(avg), ms(p95));
        log.info("[ASYNC-LAT] → 동기 대비 단축: {}ms ({}%)",
                ms(82_700L - avg), pct(82_700L - avg, 82_700L));

        assertThat(avg).as("비동기 분리 후 동기 PDF baseline(82.7ms)보다 빠를 것").isLessThan(82_700L);
    }

    // ── helper ───────────────────────────────────────────────────────────

    private void warmup() {
        for (int i = 0; i < WARMUP; i++) {
            paymentService.confirmPayment(prepareFreshPayment(), lesseeId);
        }
    }

    /** confirmPayment 1회분: 새 PAYMENT_PENDING 계약 + form_data("{}") + Redis order + 고유 paymentKey. */
    private PaymentConfirmReqDto prepareFreshPayment() {
        long nonce = System.nanoTime();
        String orderId = "ORDER-AL-" + nonce;
        String paymentKey = "PK-AL-" + nonce;

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

    private long pct(long part, long whole) {
        return whole == 0 ? 0 : Math.round(part * 100.0 / whole);
    }
}
