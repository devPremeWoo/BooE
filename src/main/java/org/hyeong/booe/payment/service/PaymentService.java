package org.hyeong.booe.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.contract.domain.Contract;
import org.hyeong.booe.contract.repository.ContractRepository;
import org.hyeong.booe.contract.domain.ContractFormData;
import org.hyeong.booe.contract.dto.req.ContractBaseReqDto;
import org.hyeong.booe.contract.repository.ContractFormDataRepository;
import org.hyeong.booe.contract.service.ContractPdfService;
import org.hyeong.booe.contract.service.ContractPdfStorageService;
import org.hyeong.booe.exception.ContractAccessDeniedException;
import org.hyeong.booe.exception.ContractNotFoundException;
import org.hyeong.booe.exception.MemberNotFoundException;
import org.hyeong.booe.exception.PaymentAmountMismatchException;
import org.hyeong.booe.exception.PaymentNotFoundException;
import org.hyeong.booe.global.fcm.FcmService;
import org.hyeong.booe.member.repository.MemberRepository;
import org.hyeong.booe.member.domain.Member;
import org.hyeong.booe.member.domain.MemberDevice;
import org.hyeong.booe.member.repository.MemberDeviceRepository;
import org.hyeong.booe.payment.api.TossPaymentApiClient;
import org.hyeong.booe.payment.domain.Payment;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final ContractRepository contractRepository;
    private final ContractFormDataRepository contractFormDataRepository;
    private final MemberRepository memberRepository;
    private final MemberDeviceRepository memberDeviceRepository;
    private final PaymentRepository paymentRepository;
    private final TossPaymentApiClient tossPaymentApiClient;
    private final ContractPdfService contractPdfService;
    private final ContractPdfStorageService contractPdfStorageService;
    private final FcmService fcmService;
    private final PaymentProperties paymentProperties;
    private final ObjectMapper objectMapper;
    private final OrderIdGenerator orderIdGenerator;
    private final PaymentOrderRedisService paymentOrderRedisService;

    public PaymentOrderResDto createOrder(PaymentOrderReqDto dto, Long memberId) {
        Contract contract = findContract(dto.getContractId());
        validateLesseeAccess(contract, memberId);
        String orderId = orderIdGenerator.generate(contract.getId());

        PaymentOrderInfo orderInfo = new PaymentOrderInfo(orderId, paymentProperties.getServiceFee());
        paymentOrderRedisService.save(contract.getId(), orderInfo);

        return new PaymentOrderResDto(orderId, paymentProperties.getServiceFee(), paymentProperties.getOrderName());
    }


    @Transactional
    public String confirmPayment(PaymentConfirmReqDto dto, Long memberId) {
        Contract contract = findContract(dto.getContractId());
        Member member = findMember(memberId);
        validateLesseeAccess(contract, memberId);
        validateAmount(dto.getAmount());

        TossPaymentConfirmResDto response = requestTossConfirm(dto);
        validateConfirmedAmount(response);

        savePayment(contract, member, response);
        contract.completePayment();

        String pdfPath = generateAndStorePdf(contract);
        notifyPaymentCompleted(contract);
        return pdfPath;
    }

    private Contract findContract(Long contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(ContractNotFoundException::new);
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }

    private void savePayment(Contract contract, Member member, TossPaymentConfirmResDto response) {
        paymentRepository.save(Payment.createPayment(
                contract, member,
                response.getPaymentKey(), response.getOrderId(),
                response.getTotalAmount(), response.getMethod(),
                LocalDateTime.parse(response.getApprovedAt().substring(0, 19)),
                serializeToJson(response)
        ));
    }

    private String serializeToJson(TossPaymentConfirmResDto response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.warn("[Payment] 응답 JSON 직렬화 실패", e);
            return null;
        }
    }

    private String generateAndStorePdf(Contract contract) {
        ContractBaseReqDto formDto = loadFormData(contract.getId());
        try {
            byte[] pdfBytes = contractPdfService.generatePdf(formDto);
            return contractPdfStorageService.save(contract.getId(), pdfBytes);
        } catch (Exception e) {
            log.error("[PDF] 생성/저장 실패 - contractId={}", contract.getId(), e);
            throw new RuntimeException("PDF 생성에 실패했습니다.", e);
        }
    }

    private ContractBaseReqDto loadFormData(Long contractId) {
        String formJson = contractFormDataRepository.findById(contractId)
                .map(ContractFormData::getFormJson)
                .orElseThrow(() -> new ContractNotFoundException());
        try {
            return objectMapper.readValue(formJson, ContractBaseReqDto.class);
        } catch (Exception e) {
            throw new RuntimeException("계약서 폼 데이터 파싱 실패", e);
        }
    }

    private void validateLesseeAccess(Contract contract, Long memberId) {
        if (!contract.getLesseeMember().getId().equals(memberId)) {
            throw new ContractAccessDeniedException();
        }
    }

    private void validateAmount(Long requestAmount) {
        if (!paymentProperties.getServiceFee().equals(requestAmount)) {
            throw new PaymentAmountMismatchException();
        }
    }

    private TossPaymentConfirmResDto requestTossConfirm(PaymentConfirmReqDto dto) {
        return tossPaymentApiClient.confirmPayment(
                dto.getPaymentKey(), dto.getOrderId(), dto.getAmount());
    }

    private void validateConfirmedAmount(TossPaymentConfirmResDto response) {
        if (!paymentProperties.getServiceFee().equals(response.getTotalAmount())) {
            throw new PaymentAmountMismatchException();
        }
    }

    @Transactional
    public void refundPayment(PaymentRefundReqDto dto, Long memberId) {
        Contract contract = findContract(dto.getContractId());
        validateLesseeAccess(contract, memberId);

        Payment payment = findDonePayment(dto.getContractId());
        tossPaymentApiClient.cancelPayment(payment.getPaymentKey(), dto.getCancelReason());

        payment.refund();
        contract.cancelPayment();
        notifyPaymentRefunded(contract);
    }

    private Payment findDonePayment(Long contractId) {
        return paymentRepository.findByContractIdAndStatus(contractId, PaymentStatus.DONE)
                .orElseThrow(PaymentNotFoundException::new);
    }

    private void notifyPaymentRefunded(Contract contract) {
        String contractId = String.valueOf(contract.getId());
        List<String> tokens = findFcmTokens(contract.getMember());
        fcmService.sendToAll(tokens, "결제 환불 완료",
                "결제가 환불 처리되었습니다.", contractId);
    }

    private void notifyPaymentCompleted(Contract contract) {
        String contractId = String.valueOf(contract.getId());
        List<String> tokens = findFcmTokens(contract.getMember());
        fcmService.sendToAll(tokens, "결제 완료",
                "임차인이 결제를 완료했습니다.", contractId);
    }

    private List<String> findFcmTokens(Member member) {
        return memberDeviceRepository.findAllByMember(member)
                .stream()
                .map(MemberDevice::getFcmToken)
                .toList();
    }
}
