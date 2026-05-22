package org.hyeong.booe.payment.service;

import lombok.RequiredArgsConstructor;
import org.hyeong.booe.contract.domain.Contract;
import org.hyeong.booe.contract.repository.ContractRepository;
import org.hyeong.booe.exception.ContractNotFoundException;
import org.hyeong.booe.exception.PaymentNotFoundException;
import org.hyeong.booe.member.domain.Member;
import org.hyeong.booe.payment.domain.Payment;
import org.hyeong.booe.payment.domain.PaymentEvent;
import org.hyeong.booe.payment.domain.type.PaymentEventActor;
import org.hyeong.booe.payment.domain.type.PaymentEventType;
import org.hyeong.booe.payment.dto.reqeust.PaymentConfirmReqDto;
import org.hyeong.booe.payment.dto.response.TossPaymentConfirmResDto;
import org.hyeong.booe.payment.repository.PaymentEventRepository;
import org.hyeong.booe.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentRecordService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final ContractRepository contractRepository;
    private final PaymentOrderRedisService paymentOrderRedisService;

    @Transactional
    public Payment createPayment(Contract contract, Member member, PaymentConfirmReqDto reqDto) {
        Payment payment = paymentRepository.save(Payment.createPayment(
                contract, member,
                reqDto.getPaymentKey(), reqDto.getOrderId(), reqDto.getAmount()));
        recordEvent(payment, PaymentEventType.CREATED, PaymentEventActor.SYSTEM, null, null);
        return payment;
    }

    @Transactional
    public void approve(Long paymentId, Long contractId,
                        TossPaymentConfirmResDto response, String rawResponse) {
        Payment payment = findPayment(paymentId);
        payment.approve(
                response.getMethod(),
                LocalDateTime.parse(response.getApprovedAt().substring(0, 19)),
                rawResponse);

        Contract contract = findContract(contractId);
        contract.completePayment();

        paymentOrderRedisService.delete(contractId);

        recordEvent(payment, PaymentEventType.APPROVED, PaymentEventActor.SYSTEM, null, rawResponse);
    }

    @Transactional
    public void cancel(Long paymentId, LocalDateTime canceledAt,
                       PaymentEventActor actor, String reason) {
        Payment payment = findPayment(paymentId);
        payment.cancel(canceledAt);
        recordEvent(payment, PaymentEventType.CANCEL_SUCCEEDED, actor, reason, null);
    }

    @Transactional
    public void markCancelFailed(Long paymentId, PaymentEventActor actor, String reason) {
        Payment payment = findPayment(paymentId);
        payment.markFailed();
        recordEvent(payment, PaymentEventType.CANCEL_FAILED, actor, reason, null);
    }

    private void recordEvent(Payment payment, PaymentEventType eventType,
                             PaymentEventActor actor, String reason, String payload) {
        paymentEventRepository.save(
                PaymentEvent.of(payment, eventType, actor, reason, payload));
    }

    private Payment findPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(PaymentNotFoundException::new);
    }

    private Contract findContract(Long contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(ContractNotFoundException::new);
    }
}
