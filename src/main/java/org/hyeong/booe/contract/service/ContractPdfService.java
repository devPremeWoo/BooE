package org.hyeong.booe.contract.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.util.XRLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.contract.dto.req.ContractBaseReqDto;
import org.hyeong.booe.contract.dto.req.ContractBaseReqDto.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 계약서 PDF를 생성합니다.
 * Thymeleaf로 HTML 렌더링 후 OpenHTMLtoPDF로 PDF로 변환합니다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContractPdfService {

    private final SpringTemplateEngine templateEngine;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");
    private static final String PDF_STYLE_OVERRIDE = loadPdfOverrideCss();

    private static String loadPdfOverrideCss() {
        try (InputStream is = ContractPdfService.class.getResourceAsStream(
                "/templates/contracts/pdf-override.css")) {
            String css = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return "<style>" + css + "</style>";
        } catch (IOException e) {
            throw new RuntimeException("pdf-override.css 로드 실패", e);
        }
    }

    public byte[] generatePdf(ContractBaseReqDto dto) throws IOException {
        String html = renderContractHtml(dto);
        html = html.replace("</head>", PDF_STYLE_OVERRIDE + "</head>");
        String baseUri = new ClassPathResource("/templates/").getURL().toString();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFont(
                () -> getClass().getResourceAsStream("/fonts/malgun.ttf"),
                "MalgunGothic", 400, PdfRendererBuilder.FontStyle.NORMAL, true
            );
            builder.useFont(
                () -> getClass().getResourceAsStream("/fonts/malgun.ttf"),
                "sans-serif", 400, PdfRendererBuilder.FontStyle.NORMAL, true
            );
            builder.withHtmlContent(html, baseUri);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        }
    }


    String renderContractHtml(ContractBaseReqDto dto) {
        Context context = new Context();
        setVariables(context, dto);
        return templateEngine.process("contracts/monthlyContractDoc", context);
    }

    private void setVariables(Context context, ContractBaseReqDto dto) {
        setAddressInfo(context, dto.getAddressInfo());
        setLandInfo(context, dto.getLandInfo());
        setBuildingInfo(context, dto.getBuildingInfo());
        context.setVariable("rentPart", dto.getLeasePart());
        setPaymentInfo(context, dto.getPaymentInfo());
        setLeaseTerm(context, dto.getLeaseTerm());
        setSpecialTerms(context, dto.getSpecialTerms());
        context.setVariable("currentDate", LocalDate.now());
        context.setVariable("landlords", toPartyList(dto.getLessors()));
        context.setVariable("tenants", toPartyList(dto.getLessees()));
    }

    private void setAddressInfo(Context context, AddressInfo addr) {
        if (addr == null) return;
        String fullAddress = addr.getAddress();
        if (addr.getDongNm() != null && !addr.getDongNm().isBlank()) fullAddress += " " + addr.getDongNm();
        if (addr.getHoNm() != null && !addr.getHoNm().isBlank()) fullAddress += " " + addr.getHoNm();
        context.setVariable("address", fullAddress);
    }

    private void setLandInfo(Context context, LandInfo land) {
        if (land == null) return;
        context.setVariable("landUsage", land.getJimok());
        context.setVariable("landRatio", land.getLandRatio());
        context.setVariable("landArea", land.getLandArea());
    }

    private void setBuildingInfo(Context context, BuildingInfo building) {
        if (building == null) return;
        context.setVariable("buildingStructure", building.getStructure());
        context.setVariable("buildingUsage", building.getUsage());
        context.setVariable("buildingArea", building.getExclusiveArea());
    }

    private void setPaymentInfo(Context context, PaymentInfo payment) {
        if (payment == null) return;
        context.setVariable("deposit", formatMoney(payment.getDeposit()));
        context.setVariable("downPayment", formatMoney(payment.getDownPayment()));
        context.setVariable("middlePayments", buildMiddlePayments(payment.getIntermediatePayments()));
        context.setVariable("balance", buildBalance(payment));
        context.setVariable("rent", buildRent(payment));
    }

    private List<Map<String, String>> buildMiddlePayments(List<IntermediatePayment> payments) {
        if (payments == null) return List.of();
        return payments.stream()
                .map(ip -> Map.of(
                        "amount", formatMoney(ip.getAmount()),
                        "date", ip.getPaymentDate() != null ? ip.getPaymentDate().format(DATE_FMT) : ""))
                .toList();
    }

    private Map<String, String> buildBalance(PaymentInfo payment) {
        return Map.of(
                "amount", formatMoney(payment.getBalance()),
                "date", payment.getBalanceDate() != null ? payment.getBalanceDate().format(DATE_FMT) : "");
    }

    private Map<String, Object> buildRent(PaymentInfo payment) {
        Map<String, Object> rent = new HashMap<>();
        rent.put("amount", formatMoney(payment.getMonthlyRent()));
        rent.put("paymentType", payment.getRentPaymentType() != null ? payment.getRentPaymentType().getLabel() : "");
        rent.put("payDay", payment.getRentPaymentDay() != null ? String.valueOf(payment.getRentPaymentDay()) : "");
        return rent;
    }

    private void setLeaseTerm(Context context, LeaseTerm term) {
        if (term == null) return;
        context.setVariable("startDate", term.getMoveInDate() != null ? term.getMoveInDate().format(DATE_FMT) : "");
        context.setVariable("duration", term.getLeaseMonths() != null ? String.valueOf(term.getLeaseMonths()) : "");
        context.setVariable("endDate", term.getLeaseEndDate() != null ? term.getLeaseEndDate().format(DATE_FMT) : "");
    }

    private void setSpecialTerms(Context context, String specialTerms) {
        if (specialTerms == null || specialTerms.isBlank()) {
            context.setVariable("specialTerms", List.of());
            return;
        }
        List<String> lines = Arrays.stream(specialTerms.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        context.setVariable("specialTerms", lines);
    }

    private List<Map<String, String>> toPartyList(List<PersonInfo> persons) {
        if (persons == null || persons.isEmpty()) return List.of();
        return persons.stream()
                .map(p -> Map.of(
                        "name", Objects.toString(p.getName(), ""),
                        "address", Objects.toString(p.getAddress(), ""),
                        "phone", Objects.toString(p.getPhone(), ""),
                        "mobile", Objects.toString(p.getMobile(), "")))
                .toList();
    }

    private String formatMoney(Long amount) {
        if (amount == null) return "";
        return String.format("정 %,d원", amount);
    }
}
