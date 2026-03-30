package org.hyeong.booe.contract.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.contract.domain.Contract;
import org.hyeong.booe.contract.domain.ContractFormData;
import org.hyeong.booe.contract.domain.ContractParty;
import org.hyeong.booe.contract.domain.type.PartyRole;
import org.hyeong.booe.contract.dto.req.ContractSaveReqDto;
import org.hyeong.booe.contract.dto.req.ReviewRequestDto;
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

    @Transactional
    public Long save(ContractSaveReqDto dto, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        Contract contract = resolveContract(dto, member, memberId);
        saveFormData(contract, dto);
        replaceParties(contract, dto);

        return contract.getId();
    }

    private Contract resolveContract(ContractSaveReqDto dto, Member member, Long memberId) {
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

    private void saveFormData(Contract contract, ContractSaveReqDto dto) {
        String formJson = serializeToJson(dto);
        contractFormDataRepository.findById(contract.getId())
                .ifPresentOrElse(
                        formData -> formData.update(formJson),
                        () -> contractFormDataRepository.save(ContractFormData.create(contract, formJson))
                );
    }

    private void replaceParties(Contract contract, ContractSaveReqDto dto) {
        contractPartyRepository.deleteAllByContract(contract);
        contractPartyRepository.saveAll(createParties(contract, dto));
    }

    private List<ContractParty> createParties(Contract contract, ContractSaveReqDto dto) {
        List<ContractParty> parties = new ArrayList<>();
        parties.addAll(createLessorParties(contract, dto.getLessors()));
        parties.addAll(createLesseeParties(contract, dto.getLessees()));
        return parties;
    }

    private List<ContractParty> createLessorParties(Contract contract, List<ContractSaveReqDto.PersonInfo> people) {
        return createPartiesWithRole(contract, people, PartyRole.LESSOR, PartyRole.CO_LESSOR);
    }

    private List<ContractParty> createLesseeParties(Contract contract, List<ContractSaveReqDto.PersonInfo> people) {
        return createPartiesWithRole(contract, people, PartyRole.LESSEE, PartyRole.CO_LESSEE);
    }

    private List<ContractParty> createPartiesWithRole(Contract contract, List<ContractSaveReqDto.PersonInfo> people,
                                                       PartyRole mainRole, PartyRole coRole) {
        List<ContractParty> parties = new ArrayList<>();
        if (people == null || people.isEmpty()) return parties;

        parties.add(ContractParty.createContractParty(contract, people.get(0), mainRole));
        for (int i = 1; i < people.size(); i++) {
            parties.add(ContractParty.createContractParty(contract, people.get(i), coRole));
        }
        return parties;
    }

    @Transactional
    public void requestReview(ReviewRequestDto dto, Long lessorMemberId) {
        save(dto.getContract(), lessorMemberId);

        Member lesseeMember = findMemberByPhone(dto.getLesseePhoneNumber());
        Contract contract = contractRepository.findById(dto.getContract().getContractId())
                .orElseThrow(ContractNotFoundException::new);
        contract.requestReview(lesseeMember);

        List<String> lesseeFcmTokens = findFcmTokensByMember(lesseeMember);
        fcmService.sendToAll(lesseeFcmTokens, "계약서 확인 요청",
                "임대인이 계약서 확인을 요청했습니다.", String.valueOf(contract.getId()));
    }

    @Transactional
    public void processLesseeConfirm(Long contractId, Long lesseeMemberId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(ContractNotFoundException::new);
        validateLesseeAccess(contract, lesseeMemberId);
        contract.confirmByLessee();

        List<String> lessorFcmTokens = findFcmTokensByMember(contract.getMember());
        fcmService.sendToAll(lessorFcmTokens, "임차인 확인 완료",
                "임차인이 계약서를 확인했습니다. 개인정보를 입력해주세요.", String.valueOf(contractId));
    }

    private void validateLesseeAccess(Contract contract, Long memberId) {
        if (!contract.getLesseeMember().getId().equals(memberId)) {
            throw new ContractAccessDeniedException();
        }
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

    private String serializeToJson(ContractSaveReqDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new JsonParsingException();
        }
    }
}
