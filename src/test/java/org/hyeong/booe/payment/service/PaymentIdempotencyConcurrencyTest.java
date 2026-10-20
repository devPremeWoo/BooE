package org.hyeong.booe.payment.service;

import org.hyeong.booe.contract.domain.Contract;
import org.hyeong.booe.contract.domain.type.ContractStatus;
import org.hyeong.booe.contract.dto.req.ContractBaseReqDto;
import org.hyeong.booe.contract.repository.ContractRepository;
import org.hyeong.booe.contract.service.ContractPdfService;
import org.hyeong.booe.contract.service.ContractPdfStorageService;
import org.hyeong.booe.exception.DuplicatePaymentConfirmException;
import org.hyeong.booe.exception.PaymentOrderInvalidException;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 결제 멱등성 이중 안전망 설계 검증.
 *
 * <p>주장: 동일 paymentKey로 동시에 N개 confirmPayment 요청이 들어와도
 * (1) Redis SETNX 게이트가 1건만 통과시키고, (2) Redis가 뚫려도 DB UNIQUE(uk_payment_key)가
 * 막아, 결과적으로 토스 confirm은 정확히 1회 호출되고 Payment는 1건만 APPROVE로 남는다.
 *
 * <p>throughput이 아니라 <b>불변식(invariant)</b>을 카운터로 증명하는 테스트.
 * 실제 MySQL(127.0.0.1:3306/booe) + Redis(localhost:6379)가 떠 있어야 실행된다.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "booe.crypto.ci.hmac-key=dGVzdC1obWFjLWtleS1mb3ItdGVzdC1vbmx5LXVzZS1wYWRkaW5n",
                "booe.crypto.ci.aes-key=dGVzdC1hZXMta2V5LXdpdGgtMzItYnl0ZS1wYWRkaW5nLWFiY2RlZg=="
        })
@ActiveProfiles("test")
@DisplayName("결제 멱등성 동시성 검증")
class PaymentIdempotencyConcurrencyTest {

    private static final Logger log = LoggerFactory.getLogger(PaymentIdempotencyConcurrencyTest.class);

    private static final int THREADS = 50;           // 동시 요청 수 (불변식 증명엔 값 무관, 튜닝 가능)
    private static final long AMOUNT = 30_000L;

    @Autowired PaymentService paymentService;
    @Autowired PaymentRecordService paymentRecordService;
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
    private Long contractId;
    private String orderId;
    private String paymentKey;

    @BeforeEach
    void setUp() throws IOException {
        long nonce = System.nanoTime();
        orderId = "ORDER-IDEM-" + nonce;
        paymentKey = "PK-IDEM-" + nonce;

        Member lessor = memberRepository.save(Member.builder().memberCode("idem-lessor-" + nonce).build());
        Member lessee = memberRepository.save(Member.builder().memberCode("idem-lessee-" + nonce).build());
        lessorId = lessor.getId();
        lesseeId = lessee.getId();

        Contract contract = contractRepository.save(buildPaymentPendingContract(lessor, lessee));
        contractId = contract.getId();
        // @MapsId OneToOne이라 detached contract로 JPA persist 시 충돌 → JDBC로 직접 삽입
        jdbcTemplate.update(
                "INSERT INTO contract_form_data (contract_id, form_json) VALUES (?, ?)", contractId, "{}");

        // 토스 호출 *전* 클라가 받은 orderId가 Redis에 기억돼 있는 상태를 재현
        paymentOrderRedisService.saveOrderInfo(contractId, new PaymentOrderInfo(orderId, AMOUNT));

        given(tossPaymentApiClient.confirmPayment(anyString(), anyString(), anyLong()))
                .willReturn(tossConfirmResponse());
        given(contractPdfService.generatePdf(any())).willReturn(new byte[]{1, 2, 3});
        given(contractPdfStorageService.save(anyLong(), any())).willReturn("path/contract-" + contractId + ".pdf");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update(
                "DELETE FROM payment_event WHERE payment_id IN (SELECT id FROM payment WHERE payment_key = ?)", paymentKey);
        jdbcTemplate.update("DELETE FROM payment WHERE payment_key = ?", paymentKey);
        if (contractId != null) {
            jdbcTemplate.update("DELETE FROM contract_form_data WHERE contract_id = ?", contractId);
            jdbcTemplate.update("DELETE FROM contract WHERE id = ?", contractId);
        }
        jdbcTemplate.update("DELETE FROM member WHERE id IN (?, ?)", lessorId, lesseeId);
        redisTemplate.delete("payment:order:" + contractId);
        redisTemplate.delete("payment:confirm:" + paymentKey);
    }

    @Test
    @DisplayName("동일 paymentKey 동시 N요청 → 성공 1 / 차단 N-1 / 토스 confirm 1회 / Payment 1건 APPROVE")
    void concurrentConfirm_onlyOneSucceeds() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger duplicate = new AtomicInteger();     // Redis SETNX 게이트에서 차단
        AtomicInteger orderInvalid = new AtomicInteger();  // 승자가 order 키 삭제 후 도착해 차단
        AtomicInteger other = new AtomicInteger();

        for (int i = 0; i < THREADS; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    paymentService.confirmPayment(buildConfirmDto(), lesseeId);
                    success.incrementAndGet();
                } catch (DuplicatePaymentConfirmException e) {
                    duplicate.incrementAndGet();
                } catch (PaymentOrderInvalidException e) {
                    orderInvalid.incrementAndGet();
                } catch (Exception e) {
                    other.incrementAndGet();
                    log.warn("[IDEM] 예상치 못한 예외", e);
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();                       // 동시 발사
        boolean finished = done.await(60, TimeUnit.SECONDS);
        pool.shutdownNow();

        log.info("[IDEM] threads={} success={} duplicate={} orderInvalid={} other={}",
                THREADS, success.get(), duplicate.get(), orderInvalid.get(), other.get());

        assertThat(finished).as("모든 스레드 완료").isTrue();
        assertThat(success.get()).as("성공은 정확히 1건").isEqualTo(1);
        assertThat(other.get()).as("예상 밖 예외 없음").isZero();
        assertThat(duplicate.get() + orderInvalid.get()).as("나머지는 모두 차단").isEqualTo(THREADS - 1);

        // 핵심 불변식: 토스 confirm은 정확히 1회만 호출된다
        verify(tossPaymentApiClient, times(1)).confirmPayment(anyString(), anyString(), anyLong());

        // DB에는 paymentKey 1건, 상태는 APPROVE
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment WHERE payment_key = ?", Integer.class, paymentKey);
        assertThat(rows).isEqualTo(1);
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM payment WHERE payment_key = ?", String.class, paymentKey);
        assertThat(status).isEqualTo("APPROVE");

        Contract contract = contractRepository.findById(contractId).orElseThrow();
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.PAYMENT_COMPLETED);
    }

    @Test
    @DisplayName("Redis 게이트 우회(락 만료·장애) 시 DB UNIQUE(uk_payment_key)가 중복 INSERT를 차단한다")
    void dbUniqueConstraint_isSecondSafetyNet() {
        Contract contract = contractRepository.findById(contractId).orElseThrow();
        Member lessee = memberRepository.findById(lesseeId).orElseThrow();

        paymentRecordService.createPayment(contract, lessee, buildConfirmDto());

        assertThatThrownBy(() ->
                paymentRecordService.createPayment(contract, lessee, buildConfirmDto()))
                .isInstanceOf(DataIntegrityViolationException.class);

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment WHERE payment_key = ?", Integer.class, paymentKey);
        assertThat(rows).as("두 번째 INSERT는 롤백되어 1건만 남음").isEqualTo(1);
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────

    private Contract buildPaymentPendingContract(Member lessor, Member lessee) {
        Contract contract = Contract.createContract(lessor, minimalContractDto());
        contract.requestReview(lessee);                       // lesseeMember 지정
        contract.confirmByLessor("홍임대", "01011112222");      // status = PAYMENT_PENDING
        return contract;
    }

    /** createContract가 요구하는 최소 필드(address)만 채운 DTO. 공용 픽스처와 분리. */
    private ContractBaseReqDto minimalContractDto() {
        ContractBaseReqDto dto = new ContractBaseReqDto();
        ContractBaseReqDto.AddressInfo addressInfo = new ContractBaseReqDto.AddressInfo();
        ReflectionTestUtils.setField(addressInfo, "address", "서울시 강남구 테헤란로 123");
        ReflectionTestUtils.setField(dto, "addressInfo", addressInfo);
        return dto;
    }

    private PaymentConfirmReqDto buildConfirmDto() {
        PaymentConfirmReqDto dto = new PaymentConfirmReqDto();
        ReflectionTestUtils.setField(dto, "contractId", contractId);
        ReflectionTestUtils.setField(dto, "paymentKey", paymentKey);
        ReflectionTestUtils.setField(dto, "orderId", orderId);
        ReflectionTestUtils.setField(dto, "amount", AMOUNT);
        return dto;
    }

    private TossPaymentConfirmResDto tossConfirmResponse() {
        TossPaymentConfirmResDto res = new TossPaymentConfirmResDto();
        ReflectionTestUtils.setField(res, "paymentKey", paymentKey);
        ReflectionTestUtils.setField(res, "orderId", orderId);
        ReflectionTestUtils.setField(res, "status", "DONE");
        ReflectionTestUtils.setField(res, "totalAmount", AMOUNT);
        ReflectionTestUtils.setField(res, "method", "간편결제");
        ReflectionTestUtils.setField(res, "approvedAt", "2026-05-27T10:00:00+09:00");
        return res;
    }
}
