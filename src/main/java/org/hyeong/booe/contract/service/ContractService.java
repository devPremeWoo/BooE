package org.hyeong.booe.contract.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.contract.domain.Contract;
import org.hyeong.booe.contract.domain.ContractDetail;
import org.hyeong.booe.contract.domain.ContractParty;
import org.hyeong.booe.contract.domain.ContractPaymentSchedule;
import org.hyeong.booe.contract.domain.type.PartyRole;
import org.hyeong.booe.contract.domain.type.PaymentType;
import org.hyeong.booe.contract.domain.type.RentPaymentType;
import org.hyeong.booe.contract.dto.req.ContractSaveReqDto;
import org.hyeong.booe.contract.repository.ContractDetailRepository;
import org.hyeong.booe.contract.repository.ContractPartyRepository;
import org.hyeong.booe.contract.repository.ContractPaymentScheduleRepository;
import org.hyeong.booe.contract.repository.ContractRepository;
import org.hyeong.booe.exception.JsonParsingException;
import org.hyeong.booe.member.domain.Member;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractPartyRepository contractPartyRepository;
    private final ContractDetailRepository contractDetailRepository;
    private final ContractPaymentScheduleRepository paymentScheduleRepository;
    private final ObjectMapper objectMapper;

    // 월세 요청이라고 가정. 전세 요청은 따로 만들자
    public void save(Member member, ContractSaveReqDto dto) {

        Contract contract = contractRepository.save(Contract.createContract(member, dto));

        String propertyJson = convertPropertyInfoToJson(dto.getLandInfo(), dto.getBuildingInfo());
        contractDetailRepository.save(ContractDetail.createContractDetail(contract, dto, propertyJson));
        contractPartyRepository.saveAll(createParties(contract, dto));
        paymentScheduleRepository.saveAll(createMonthlyRentSchedule(contract, dto));
    }

    private String convertPropertyInfoToJson(ContractSaveReqDto.LandInfo land, ContractSaveReqDto.BuildingInfo building) {
        try {
            Map<String, Object> propertyMap = Map.of("land", land, "building", building);
            return objectMapper.writeValueAsString(propertyMap);
        } catch (JsonProcessingException e) {
            throw new JsonParsingException();
        }
    }

    // 우선 월세 저장이라고 가정하고 설계
    private List<ContractPaymentSchedule> createMonthlyRentSchedule(Contract contract, ContractSaveReqDto dto) {
        List<ContractPaymentSchedule> schedules = new ArrayList<>();

        var paymentInfo = dto.getPaymentInfo();
        var leaseTerm = dto.getLeaseTerm();

        LocalDate startDate = leaseTerm.getMoveInDate();
        int totalMonths = leaseTerm.getLeaseMonths();
        int paymentDay = paymentInfo.getRentPaymentDay();

        for (int i = 1; i <= totalMonths; i++) {
            int monthOffset = (paymentInfo.getRentPaymentType() == RentPaymentType.PRE) ? (i - 1) : i;
            LocalDate targetMonth = startDate.plusMonths(monthOffset);

            int actualDay = Math.min(paymentDay, targetMonth.lengthOfMonth());
            LocalDate dueDate = targetMonth.withDayOfMonth(actualDay);

            schedules.add(ContractPaymentSchedule.createPaymentSchedule(
                    contract, PaymentType.MONTHLY_RENT, paymentInfo.getMonthlyRent()
                    , dueDate, i
            ));
        }
        return schedules;
    }

    private List<ContractParty> createParties(Contract contract, ContractSaveReqDto dto) {
        List<ContractParty> parties = new ArrayList<>();

        parties.addAll(createLessorParty(contract, dto.getLessors()));
        parties.addAll(createLesseeParty(contract, dto.getLessees()));

        return parties;
    }

    private List<ContractParty> createLessorParty(Contract contract, List<ContractSaveReqDto.PersonInfo> people) {
        List<ContractParty> parties = new ArrayList<>();

        if (people == null || people.isEmpty()) return parties;

        for (int i = 0; i < people.size(); i++) {
            if (i == 0) {
                parties.add(ContractParty.createContractParty(contract, people.get(i), PartyRole.LESSOR));
                continue;
            }
            parties.add(ContractParty.createContractParty(contract, people.get(i), PartyRole.CO_LESSOR));

        }
        return parties;
    }

    private List<ContractParty> createLesseeParty(Contract contract, List<ContractSaveReqDto.PersonInfo> people) {
        List<ContractParty> parties = new ArrayList<>();

        if (people == null || people.isEmpty()) return parties;

        for (int i = 0; i < people.size(); i++) {
            if (i == 0) {
                parties.add(ContractParty.createContractParty(contract, people.get(i), PartyRole.LESSEE));
                continue;
            }
            parties.add(ContractParty.createContractParty(contract, people.get(i), PartyRole.CO_LESSEE));

        }
        return parties;
    }
}
