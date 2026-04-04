package org.hyeong.booe.contract.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.contract.domain.Contract;
import org.hyeong.booe.contract.domain.ContractFormData;
import org.hyeong.booe.contract.domain.ContractParty;
import org.hyeong.booe.contract.domain.type.PartyRole;
import org.hyeong.booe.contract.dto.req.ContractBaseReqDto;
import org.hyeong.booe.contract.dto.req.DownPaymentConfirmReqDto;
import org.hyeong.booe.contract.dto.req.ReviewRequestDto;
import org.hyeong.booe.contract.dto.res.ContractResDto;
import org.hyeong.booe.contract.repository.ContractFormDataRepository;
import org.hyeong.booe.contract.repository.ContractPartyRepository;
import org.hyeong.booe.contract.repository.ContractRepository;
import org.hyeong.booe.exception.ContractAccessDeniedException;
import org.hyeong.booe.exception.ContractNotFoundException;
import org.hyeong.booe.exception.JsonParsingException;
import org.hyeong.booe.exception.MemberNotFoundException;
import org.hyeong.booe.exception.ProfileNotFoundException;
import org.hyeong.booe.global.fcm.FcmService;
import org.hyeong.booe.member.domain.Member;
import org.hyeong.booe.member.domain.MemberDevice;
import org.hyeong.booe.member.domain.MemberProfile;
import org.hyeong.booe.member.domain.type.MemberStatus;
import org.hyeong.booe.member.repository.MemberDeviceRepository;
import org.hyeong.booe.member.repository.MemberProfileRepository;
import org.hyeong.booe.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractPartyRepository contractPartyRepository;
    private final ContractFormDataRepository contractFormDataRepository;
    private final ObjectMapper objectMapper;
    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final MemberDeviceRepository memberDeviceRepository;
    private final FcmService fcmService;

    // 임시저장 (임대인) - contractId 없으면 신규, 있으면 수정
    @Transactional
    public Long save(ContractBaseReqDto dto, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        Contract contract = resolveContract(dto, member, memberId);
        saveFormData(contract, dto);
        replaceParties(contract, dto);

        return contract.getId();
    }

    // 임대인 → 임차인 확인 요청
    @Transactional
    public void requestReview(ReviewRequestDto dto, Long lessorMemberId) {
        save(dto.getContract(), lessorMemberId);

        // TODO: 번호로 회원 조회 실패 시 예외 처리 추가 필요
        Member lesseeMember = findMemberByPhone(dto.getLesseePhoneNumber());
        Contract contract = contractRepository.findById(dto.getContract().getContractId())
                .orElseThrow(ContractNotFoundException::new);
        contract.requestReview(lesseeMember);

        List<String> lesseeFcmTokens = findFcmTokensByMember(lesseeMember);
        fcmService.sendToAll(lesseeFcmTokens, "계약서 확인 요청",
                "임대인이 계약서 확인을 요청했습니다.", String.valueOf(contract.getId()));
    }

    // 계약서 단건 조회 (임대인/임차인 공통)
    public ContractResDto getContract(Long contractId, Long memberId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(ContractNotFoundException::new);
        validateContractAccess(contract, memberId);

        String formJson = contractFormDataRepository.findById(contractId)
                .map(ContractFormData::getFormJson)
                .orElse(null);

        return ContractResDto.of(contract, formJson);
    }

    // 임차인 정보 입력 완료
    @Transactional
    public void submitLesseeInfo(Long contractId, ContractBaseReqDto dto, Long lesseeMemberId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(ContractNotFoundException::new);
        validateLesseeAccess(contract, lesseeMemberId);

        saveFormData(contract, dto);
        replaceLesseeParties(contract, dto.getLessees());
        contract.submitByLessee();

        List<String> lessorFcmTokens = findFcmTokensByMember(contract.getMember());
        fcmService.sendToAll(lessorFcmTokens, "임차인 정보 입력 완료",
                "임차인이 정보를 입력했습니다. 확인 후 결제를 진행해주세요.", String.valueOf(contractId));
    }

    // 임대인 계약금 수령 확인 + 영수자 정보 입력 → 결제 요청 단계
    @Transactional
    public void confirmDownPayment(Long contractId, DownPaymentConfirmReqDto dto, Long lessorMemberId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(ContractNotFoundException::new);
        validateOwnership(contract, lessorMemberId);

        contract.confirmByLessor(dto.getReceiverName(), dto.getReceiverPhone());
    }

    private Contract resolveContract(ContractBaseReqDto dto, Member member, Long memberId) {
        if (dto.getContractId() == null) {
            return contractRepository.save(Contract.createContract(member, dto));
        }

        Contract contract = contractRepository.findById(dto.getContractId())
                .orElseThrow(ContractNotFoundException::new);
        validateOwnership(contract, memberId);
        contract.update(dto);
        return contract;
    }

    private void validateOwnership(Contract contract, Long memberId) {
        if (!contract.getMember().getId().equals(memberId)) {
            throw new ContractAccessDeniedException();
        }
    }

    private void validateLesseeAccess(Contract contract, Long memberId) {
        if (!contract.getLesseeMember().getId().equals(memberId)) {
            throw new ContractAccessDeniedException();
        }
    }

    private void validateContractAccess(Contract contract, Long memberId) {
        boolean isLessor = contract.getMember().getId().equals(memberId);
        boolean isLessee = contract.getLesseeMember() != null
                && contract.getLesseeMember().getId().equals(memberId);

        if (!isLessor && !isLessee) {
            throw new ContractAccessDeniedException();
        }
    }

    private void saveFormData(Contract contract, ContractBaseReqDto dto) {
        String formJson = serializeToJson(dto);
        contractFormDataRepository.findById(contract.getId())
                .ifPresentOrElse(
                        formData -> formData.update(formJson),
                        () -> contractFormDataRepository.save(ContractFormData.create(contract, formJson))
                );
    }

    private void replaceParties(Contract contract, ContractBaseReqDto dto) {
        contractPartyRepository.deleteAllByContract(contract);
        contractPartyRepository.saveAll(createParties(contract, dto));
    }

    private void replaceLesseeParties(Contract contract, List<ContractBaseReqDto.PersonInfo> lessees) {
        contractPartyRepository.deleteAllByContractAndRoleIn(
                contract, List.of(PartyRole.LESSEE, PartyRole.CO_LESSEE));
        contractPartyRepository.saveAll(
                createPartiesWithRole(contract, lessees, PartyRole.LESSEE, PartyRole.CO_LESSEE));
    }

    private List<ContractParty> createParties(Contract contract, ContractBaseReqDto dto) {
        List<ContractParty> parties = new ArrayList<>();
        parties.addAll(createPartiesWithRole(contract, dto.getLessors(), PartyRole.LESSOR, PartyRole.CO_LESSOR));
        parties.addAll(createPartiesWithRole(contract, dto.getLessees(), PartyRole.LESSEE, PartyRole.CO_LESSEE));
        return parties;
    }

    private List<ContractParty> createPartiesWithRole(Contract contract, List<ContractBaseReqDto.PersonInfo> people,
                                                       PartyRole mainRole, PartyRole coRole) {
        List<ContractParty> parties = new ArrayList<>();
        if (people == null || people.isEmpty()) return parties;

        parties.add(ContractParty.createContractParty(contract, people.get(0), mainRole));
        for (int i = 1; i < people.size(); i++) {
            parties.add(ContractParty.createContractParty(contract, people.get(i), coRole));
        }
        return parties;
    }

    private Member findMemberByPhone(String phoneNumber) {
        MemberProfile profile = memberProfileRepository
                .findByPhoneNumberAndMemberStatus(phoneNumber, MemberStatus.ACTIVE)
                .orElseThrow(ProfileNotFoundException::new);
        return profile.getMember();
    }

    private List<String> findFcmTokensByMember(Member member) {
        return memberDeviceRepository.findAllByMember(member)
                .stream()
                .map(MemberDevice::getFcmToken)
                .toList();
    }

    private String serializeToJson(ContractBaseReqDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new JsonParsingException();
        }
    }
}
