package org.hyeong.booe.contract.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * PDF 생성 전용 워커 풀.
 *
 * <p>측정 근거(로컬, -Xmx 512MB 기준):
 * <ul>
 *   <li>throughput은 c=4에서 정점(62 PDF/s) — CPU 바운드, 코어 수에서 포화
 *   <li>peak heap working set은 c=4에서 ~390MB, c=8부터 천장 부근(OOM 임계)
 *   <li>두 천장(CPU·메모리)이 동일 지점에서 만나는 환경 — 운영에서는 사양 따라 재산출 필요
 * </ul>
 *
 * <p>풀 구성:
 * <ul>
 *   <li>워커 N=4 — 동시 PDF 작업 수 캡
 *   <li>큐 Q=100 — 짧은 트래픽 스파이크 흡수, 무제한 큐가 만들 새로운 형태의 OOM 차단
 *   <li>거절 정책 AbortPolicy — 풀+큐 모두 가득이면 RejectedExecutionException 던짐.
 *       호출자(PaymentService)가 로그+알람 후 reconciliation 스케줄러가 PDF 누락 detect/재처리
 *       (스케줄러 확장은 별건 작업)
 * </ul>
 */
@Configuration
@EnableAsync
public class PdfExecutorConfig {

    public static final String PDF_EXECUTOR = "pdfTaskExecutor";

    @Bean(name = PDF_EXECUTOR)
    public Executor pdfTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("pdf-worker-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
