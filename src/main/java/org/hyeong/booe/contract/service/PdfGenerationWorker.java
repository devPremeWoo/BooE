package org.hyeong.booe.contract.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.contract.config.PdfExecutorConfig;
import org.hyeong.booe.contract.dto.req.ContractBaseReqDto;
import org.hyeong.booe.global.fcm.FcmService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * 결제 승인 후 PDF 생성 + 저장 + 완료 FCM 알림을 비동기로 처리.
 *
 * <p>도입 근거: 측정상 PDF 생성이 confirmPayment 응답의 62%를 차지(51ms / 82ms). 동기 경로에서 제거하면
 * 결제 응답 latency 대폭 단축 + 동시 PDF 작업이 톰캣 요청 스레드를 점유하지 않음.
 *
 * <p>전용 풀({@link PdfExecutorConfig#PDF_EXECUTOR}, N=4 + 큐 100 + AbortPolicy)에서 실행.
 * 풀+큐 모두 가득이면 호출자에 RejectedExecutionException 전파 → PaymentService에서 로그 처리.
 *
 * <p>워커 내부 실패(폼 파싱, PDF 렌더, 저장, FCM)는 로그만 남긴다. 후속 안전망은 reconciliation
 * 스케줄러 확장으로 PDF 누락을 detect/재처리하는 것 — 별건 작업으로 분리.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PdfGenerationWorker {

    private final PdfJobDataLoader dataLoader;
    private final ContractPdfService contractPdfService;
    private final ContractPdfStorageService contractPdfStorageService;
    private final FcmService fcmService;

    @Async(PdfExecutorConfig.PDF_EXECUTOR)
    public void process(Long contractId) {
        try {
            PdfJobDataLoader.JobData data = dataLoader.load(contractId);
            byte[] pdfBytes = render(data.formDto());
            contractPdfStorageService.save(contractId, pdfBytes);
            notifyCompletion(contractId, data.tokens());
        } catch (Throwable t) {
            log.error("[PDF-Worker] 처리 실패 contractId={}", contractId, t);
        }
    }

    private byte[] render(ContractBaseReqDto formDto) {
        try {
            return contractPdfService.generatePdf(formDto);
        } catch (IOException e) {
            throw new RuntimeException("PDF 렌더 실패", e);
        }
    }

    private void notifyCompletion(Long contractId, List<String> tokens) {
        fcmService.sendToAll(tokens, "결제 완료",
                "임차인이 결제를 완료했습니다.", String.valueOf(contractId));
    }
}
