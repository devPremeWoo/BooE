package org.hyeong.booe.contract.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.hyeong.booe.contract.domain.Contract;
import org.hyeong.booe.contract.domain.ContractFormData;
import org.hyeong.booe.contract.dto.req.ContractBaseReqDto;
import org.hyeong.booe.contract.repository.ContractFormDataRepository;
import org.hyeong.booe.contract.repository.ContractRepository;
import org.hyeong.booe.exception.ContractNotFoundException;
import org.hyeong.booe.member.domain.MemberDevice;
import org.hyeong.booe.member.repository.MemberDeviceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * PDF 워커가 필요한 데이터를 한 트랜잭션 안에서 모아 온다.
 *
 * <p>워커({@link PdfGenerationWorker})에서 {@code @Transactional}을 직접 붙이면
 * (1) 자기 자신 호출이 아니라 외부 호출이라 적용은 되지만 PDF 렌더 중에도 커넥션을 잡고 있어 낭비,
 * (2) 같은 클래스 내 self-invocation 시 AOP가 동작하지 않음.
 * → 별도 빈으로 분리해 짧은 트랜잭션 안에서 lazy 필드까지 다 로드한 뒤 detach.
 */
@Component
@RequiredArgsConstructor
public class PdfJobDataLoader {

    private final ContractRepository contractRepository;
    private final ContractFormDataRepository contractFormDataRepository;
    private final MemberDeviceRepository memberDeviceRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public JobData load(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(ContractNotFoundException::new);
        String formJson = contractFormDataRepository.findById(contractId)
                .map(ContractFormData::getFormJson)
                .orElseThrow(ContractNotFoundException::new);
        ContractBaseReqDto formDto = parseFormJson(formJson);
        List<String> tokens = memberDeviceRepository.findAllByMember(contract.getMember())
                .stream()
                .map(MemberDevice::getFcmToken)
                .toList();
        return new JobData(formDto, tokens);
    }

    private ContractBaseReqDto parseFormJson(String formJson) {
        try {
            return objectMapper.readValue(formJson, ContractBaseReqDto.class);
        } catch (Exception e) {
            throw new RuntimeException("계약서 폼 데이터 파싱 실패", e);
        }
    }

    public record JobData(ContractBaseReqDto formDto, List<String> tokens) {}
}
