package org.hyeong.booe.contract.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.contract.dto.req.ContractBaseReqDto;
import org.hyeong.booe.contract.dto.req.ContractBaseReqDto.*;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 계약서 HTML을 Thymeleaf 템플릿으로 렌더링합니다.
 * 추후 OpenHTMLtoPDF로 HTML → PDF 변환을 추가합니다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContractPdfService {

    private final SpringTemplateEngine templateEngine;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

    /**
     * ContractBaseReqDto를 기반으로 계약서 HTML을 렌더링합니다.
     */
    public String renderContractHtml(ContractBaseReqDto dto) {
        Context context = new Context();
        setVariables(context, dto);
        return templateEngine.process("contracts/monthlyContractDoc", context);
    }

    private void setVariables(Context context, ContractBaseReqDto dto) {
        // 1. 부동산 표시
        AddressInfo addr = dto.getAddressInfo();
        if (addr != null) {
            String fullAddress = addr.getAddress();
            if (addr.getDongNm() != null && !addr.getDongNm().isBlank()) {
                fullAddress += " " + addr.getDongNm();
            }
            if (addr.getHoNm() != null && !addr.getHoNm().isBlank()) {
                fullAddress += " " + addr.getHoNm();
            }
            context.setVariable("address", fullAddress);
        }

        LandInfo land = dto.getLandInfo();
        if (land != null) {
            context.setVariable("landUsage", land.getJimok());
            context.setVariable("landRatio", land.getLandRatio());
            context.setVariable("landArea", land.getLandArea());
        }

        BuildingInfo building = dto.getBuildingInfo();
        if (building != null) {
            context.setVariable("buildingStructure", building.getStructure());
            context.setVariable("buildingUsage", building.getUsage());
            context.setVariable("buildingArea", building.getExclusiveArea());
        }

        context.setVariable("rentPart", dto.getLeasePart());

        // 2. 계약 내용 - 금액
        PaymentInfo payment = dto.getPaymentInfo();
        if (payment != null) {
            context.setVariable("deposit", formatMoney(payment.getDeposit()));
            context.setVariable("downPayment", formatMoney(payment.getDownPayment()));

            // 중도금
            List<Map<String, String>> middlePayments = new ArrayList<>();
            if (payment.getIntermediatePayments() != null) {
                for (IntermediatePayment ip : payment.getIntermediatePayments()) {
                    Map<String, String> mp = new HashMap<>();
                    mp.put("amount", formatMoney(ip.getAmount()));
                    mp.put("date", ip.getPaymentDate() != null ? ip.getPaymentDate().format(DATE_FMT) : "");
                    middlePayments.add(mp);
                }
            }
            context.setVariable("middlePayments", middlePayments);

            // 잔금
            Map<String, String> balance = new HashMap<>();
            balance.put("amount", formatMoney(payment.getBalance()));
            balance.put("date", payment.getBalanceDate() != null ? payment.getBalanceDate().format(DATE_FMT) : "");
            context.setVariable("balance", balance);

            // 월세
            Map<String, Object> rent = new HashMap<>();
            rent.put("amount", formatMoney(payment.getMonthlyRent()));
            rent.put("paymentType", payment.getRentPaymentType() != null
                    ? payment.getRentPaymentType().getLabel() : "");
            rent.put("payDay", payment.getRentPaymentDay() != null
                    ? String.valueOf(payment.getRentPaymentDay()) : "");
            context.setVariable("rent", rent);
        }

        // 3. 임대차 기간
        LeaseTerm term = dto.getLeaseTerm();
        if (term != null) {
            context.setVariable("startDate",
                    term.getMoveInDate() != null ? term.getMoveInDate().format(DATE_FMT) : "");
            context.setVariable("duration",
                    term.getLeaseMonths() != null ? String.valueOf(term.getLeaseMonths()) : "");
            context.setVariable("endDate",
                    term.getLeaseEndDate() != null ? term.getLeaseEndDate().format(DATE_FMT) : "");
        }

        // 4. 특약사항
        List<String> specialTerms = new ArrayList<>();
        if (dto.getSpecialTerms() != null && !dto.getSpecialTerms().isBlank()) {
            String[] lines = dto.getSpecialTerms().split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    specialTerms.add(trimmed);
                }
            }
        }
        context.setVariable("specialTerms", specialTerms);

        // 5. 날짜
        context.setVariable("currentDate", LocalDate.now());

        // 6. 임대인(landlords)
        List<Map<String, String>> landlords = toPartyList(dto.getLessors());
        context.setVariable("landlords", landlords);

        // 7. 임차인(tenants)
        List<Map<String, String>> tenants = toPartyList(dto.getLessees());
        context.setVariable("tenants", tenants);
    }

    private List<Map<String, String>> toPartyList(List<PersonInfo> persons) {
        if (persons == null || persons.isEmpty()) {
            return List.of();
        }
        List<Map<String, String>> result = new ArrayList<>();
        for (PersonInfo p : persons) {
            Map<String, String> map = new HashMap<>();
            map.put("name", p.getName());
            map.put("address", p.getAddress());
            map.put("phone", p.getPhone());
            map.put("mobile", p.getMobile());
            result.add(map);
        }
        return result;
    }

    private String formatMoney(Long amount) {
        if (amount == null) return "";
        return String.format("정 %,d원", amount);
    }
}
