package org.hyeong.booe.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.contract.domain.Contract;
import org.hyeong.booe.contract.repository.ContractRepository;
import org.hyeong.booe.contract.service.ContractPdfStorageService;
import org.hyeong.booe.contract.service.PdfGenerationWorker;
import org.hyeong.booe.exception.ContractAccessDeniedException;
import org.hyeong.booe.exception.ContractNotFoundException;
import org.hyeong.booe.exception.DuplicatePaymentConfirmException;
import org.hyeong.booe.exception.MemberNotFoundException;
import org.hyeong.booe.exception.PaymentNotFoundException;
import org.hyeong.booe.exception.PaymentOrderInvalidException;
import org.hyeong.booe.global.fcm.FcmService;
import org.hyeong.booe.member.repository.MemberRepository;
import org.hyeong.booe.member.domain.Member;
import org.hyeong.booe.member.domain.MemberDevice;
import org.hyeong.booe.member.repository.MemberDeviceRepository;
import org.hyeong.booe.payment.api.TossPaymentApiClient;
import org.hyeong.booe.payment.domain.Payment;
import org.hyeong.booe.payment.domain.type.PaymentEventActor;
import org.hyeong.booe.payment.domain.type.PaymentStatus;
import org.hyeong.booe.payment.dto.PaymentOrderInfo;
import org.hyeong.booe.payment.dto.reqeust.PaymentConfirmReqDto;
import org.hyeong.booe.payment.dto.reqeust.PaymentOrderReqDto;
import org.hyeong.booe.payment.dto.reqeust.PaymentRefundReqDto;
import org.hyeong.booe.payment.dto.response.PaymentOrderResDto;
import org.hyeong.booe.payment.dto.response.TossPaymentConfirmResDto;
import org.hyeong.booe.payment.properties.PaymentProperties;
import org.hyeong.booe.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final ContractRepository contractRepository;
    private final MemberRepository memberRepository;
    private final MemberDeviceRepository memberDeviceRepository;
    private final PaymentRepository paymentRepository;
    private final TossPaymentApiClient tossPaymentApiClient;
    private final ContractPdfStorageService contractPdfStorageService;
    private final FcmService fcmService;
    private final PaymentProperties paymentProperties;
    private final ObjectMapper objectMapper;
    private final OrderIdGenerator orderIdGenerator;
    private final PaymentOrderRedisService paymentOrderRedisService;
    private final PaymentRecordService paymentRecordService;
    private final PdfGenerationWorker pdfGenerationWorker;

    public PaymentOrderResDto createOrder(PaymentOrderReqDto dto, Long memberId) {
        Contract contract = findContract(dto.getContractId());
        validateLesseeAccess(contract, memberId);
        String orderId = orderIdGenerator.generate(contract.getId());

        PaymentOrderInfo orderInfo = new PaymentOrderInfo(orderId, paymentProperties.getServiceFee());
        paymentOrderRedisService.saveOrderInfo(contract.getId(), orderInfo);

        return new PaymentOrderResDto(orderId, paymentProperties.getServiceFee(), paymentProperties.getOrderName());
    }


    public String confirmPayment(PaymentConfirmReqDto dto, Long memberId) {
        Contract contract = findContract(dto.getContractId());
        Member member = findMember(memberId);
        validateLesseeAccess(contract, memberId);

        PaymentOrderInfo orderInfo = validateOrder(dto);

        if (!paymentOrderRedisService.savePaymentConfirm(dto.getPaymentKey())) {
            throw new DuplicatePaymentConfirmException();
        }

        Payment payment = paymentRecordService.createPayment(contract, member, dto);
        processTossPayment(payment.getId(), dto, orderInfo);

        dispatchPdfGeneration(contract.getId());
        return contractPdfStorageService.pathFor(contract.getId());
    }

    /**
     * PDF 생성·저장·완료 FCM을 전용 워커 풀에 위임. 풀+큐가 모두 가득이면 AbortPolicy가
     * RejectedExecutionException을 던지므로 잡아서 로그만 남긴다 — 결제 자체는 이미 APPROVE 커밋됨.
     * (후속 안전망: reconciliation 스케줄러 확장으로 PDF 누락 detect/재처리 — 별건 작업)
     */
    private void dispatchPdfGeneration(Long contractId) {
        try {
            pdfGenerationWorker.process(contractId);
        } catch (RejectedExecutionException e) {
            log.error("[Payment] PDF 워커 큐 가득 — 거절. contractId={}. 후속 reconciliation 처리 예정.",
                    contractId, e);
        }
    }

    private void processTossPayment(Long paymentId, PaymentConfirmReqDto dto, PaymentOrderInfo orderInfo) {
        try {
            TossPaymentConfirmResDto response = requestTossConfirm(dto, orderInfo);
            paymentRecordService.approve(paymentId, dto.getContractId(), response, serializeToJson(response));
        } catch (Exception e) {
            compensateOrphan(paymentId, dto.getPaymentKey(), e);
            throw e;
        }
    }

    private void compensateOrphan(Long paymentId, String paymentKey, Exception cause) {
        String reason = "post-confirm failure: " + cause.getMessage();
        try {
            tossPaymentApiClient.cancelPayment(paymentKey, reason);
            paymentRecordService.cancel(paymentId, LocalDateTime.now(),
                    PaymentEventActor.SYSTEM, reason);
        } catch (Exception cancelEx) {
            log.error("[Payment] orphan compensation failed - paymentId={}, paymentKey={}",
                    paymentId, paymentKey, cancelEx);
            paymentRecordService.markCancelFailed(paymentId,
                    PaymentEventActor.SYSTEM, cancelEx.getMessage());
        }
    }

    private Contract findContract(Long contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(ContractNotFoundException::new);
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }

    private String serializeToJson(TossPaymentConfirmResDto response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.warn("[Payment] 응답 JSON 직렬화 실패", e);
            return null;
        }
    }

    private PaymentOrderInfo validateOrder(PaymentConfirmReqDto dto) {
        PaymentOrderInfo orderInfo = paymentOrderRedisService.findOrderInfo(dto.getContractId());
        if (orderInfo == null) throw new PaymentOrderInvalidException();
        if (!orderInfo.getOrderId().equals(dto.getOrderId())) throw new PaymentOrderInvalidException();
        return orderInfo;
    }

    private void validateLesseeAccess(Contract contract, Long memberId) {
        if (!contract.getLesseeMember().getId().equals(memberId)) {
            throw new ContractAccessDeniedException();
        }
    }

    private TossPaymentConfirmResDto requestTossConfirm(PaymentConfirmReqDto dto, PaymentOrderInfo orderInfo) {
        return tossPaymentApiClient.confirmPayment(
                dto.getPaymentKey(), orderInfo.getOrderId(), orderInfo.getAmount());
    }

    @Transactional
    public void refundPayment(PaymentRefundReqDto dto, Long memberId) {
        Contract contract = findContract(dto.getContractId());
        validateLesseeAccess(contract, memberId);

        Payment payment = findDonePayment(dto.getContractId());
        tossPaymentApiClient.cancelPayment(payment.getPaymentKey(), dto.getCancelReason());

        paymentRecordService.cancel(payment.getId(), LocalDateTime.now(),
                PaymentEventActor.USER, dto.getCancelReason());
        contract.cancelPayment();
        notifyPaymentRefunded(contract);
    }

    private Payment findDonePayment(Long contractId) {
        return paymentRepository.findByContractIdAndStatus(contractId, PaymentStatus.APPROVE)
                .orElseThrow(PaymentNotFoundException::new);
    }

    private void notifyPaymentRefunded(Contract contract) {
        String contractId = String.valueOf(contract.getId());
        List<String> tokens = findFcmTokens(contract.getMember());
        fcmService.sendToAll(tokens, "결제 환불 완료",
                "결제가 환불 처리되었습니다.", contractId);
    }

    private List<String> findFcmTokens(Member member) {
        return memberDeviceRepository.findAllByMember(member)
                .stream()
                .map(MemberDevice::getFcmToken)
                .toList();
    }
}
