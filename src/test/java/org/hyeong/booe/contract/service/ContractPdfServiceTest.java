package org.hyeong.booe.contract.service;

import org.hyeong.booe.contract.domain.type.RentPaymentType;
import org.hyeong.booe.contract.dto.req.ContractBaseReqDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContractPdfServiceTest {

    private ContractPdfService service;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        service = new ContractPdfService(engine);
    }

    @Test
    void renderContractHtml_파일출력() throws IOException {
        ContractBaseReqDto dto = buildSampleDto();

        // 공동임대인 추가
        ContractBaseReqDto.PersonInfo coLessor = new ContractBaseReqDto.PersonInfo();
        ReflectionTestUtils.setField(coLessor, "name", "이영희");
        ReflectionTestUtils.setField(coLessor, "address", "서울시 서초구 반포동");
        ReflectionTestUtils.setField(coLessor, "phone", "02-3333-4444");
        ReflectionTestUtils.setField(coLessor, "mobile", "010-1111-2222");
        ReflectionTestUtils.setField(dto, "lessors", List.of(dto.getLessors().get(0), coLessor));

        // 중도금 2회 추가
        ContractBaseReqDto.IntermediatePayment ip1 = new ContractBaseReqDto.IntermediatePayment();
        ReflectionTestUtils.setField(ip1, "degree", 1);
        ReflectionTestUtils.setField(ip1, "amount", 10_000_000L);
        ReflectionTestUtils.setField(ip1, "paymentDate", LocalDate.of(2026, 5, 15));

        ContractBaseReqDto.IntermediatePayment ip2 = new ContractBaseReqDto.IntermediatePayment();
        ReflectionTestUtils.setField(ip2, "degree", 2);
        ReflectionTestUtils.setField(ip2, "amount", 10_000_000L);
        ReflectionTestUtils.setField(ip2, "paymentDate", LocalDate.of(2026, 6, 15));

        ReflectionTestUtils.setField(dto.getPaymentInfo(), "intermediatePayments", List.of(ip1, ip2));

        String html = service.renderContractHtml(dto);

        Path output = Path.of("build/contract_preview.html");
        Files.writeString(output, html);
        System.out.println(">>> 크롬에서 열기: " + output.toAbsolutePath());
    }

    @Test
    void renderContractHtml_4인_파일출력() throws IOException {
        ContractBaseReqDto dto = buildSampleDto();

        // 임대인 공동명의인
        ContractBaseReqDto.PersonInfo coLessor = new ContractBaseReqDto.PersonInfo();
        ReflectionTestUtils.setField(coLessor, "name", "이영희");
        ReflectionTestUtils.setField(coLessor, "address", "서울특별시 서초구 반포동 456-78");
        ReflectionTestUtils.setField(coLessor, "phone", "02-3333-4444");
        ReflectionTestUtils.setField(coLessor, "mobile", "010-1111-2222");
        ReflectionTestUtils.setField(dto, "lessors", List.of(dto.getLessors().get(0), coLessor));

        // 임차인 공동명의인
        ContractBaseReqDto.PersonInfo coLessee = new ContractBaseReqDto.PersonInfo();
        ReflectionTestUtils.setField(coLessee, "name", "박민지");
        ReflectionTestUtils.setField(coLessee, "address", "경기도 성남시 분당구 정자동 111-22");
        ReflectionTestUtils.setField(coLessee, "phone", "031-555-6666");
        ReflectionTestUtils.setField(coLessee, "mobile", "010-5555-6666");
        ReflectionTestUtils.setField(dto, "lessees", List.of(dto.getLessees().get(0), coLessee));

        String html = service.renderContractHtml(dto);

        Path output = Path.of("build/contract_preview_4persons.html");
        Files.writeString(output, html);
        System.out.println(">>> 크롬에서 열기: " + output.toAbsolutePath());
    }

    @Test
    void renderContractHtml_6인_파일출력() throws IOException {
        ContractBaseReqDto dto = buildSampleDto();

        // 임대인 공동명의인 1
        ContractBaseReqDto.PersonInfo coLessor1 = new ContractBaseReqDto.PersonInfo();
        ReflectionTestUtils.setField(coLessor1, "name", "이영희");
        ReflectionTestUtils.setField(coLessor1, "address", "서울특별시 서초구 반포동 456-78");
        ReflectionTestUtils.setField(coLessor1, "phone", "02-3333-4444");
        ReflectionTestUtils.setField(coLessor1, "mobile", "010-1111-2222");

        // 임대인 공동명의인 2
        ContractBaseReqDto.PersonInfo coLessor2 = new ContractBaseReqDto.PersonInfo();
        ReflectionTestUtils.setField(coLessor2, "name", "최준혁");
        ReflectionTestUtils.setField(coLessor2, "address", "경기도 수원시 영통구 광교동 200-33");
        ReflectionTestUtils.setField(coLessor2, "phone", "031-777-8888");
        ReflectionTestUtils.setField(coLessor2, "mobile", "010-7777-8888");

        ReflectionTestUtils.setField(dto, "lessors", List.of(dto.getLessors().get(0), coLessor1, coLessor2));

        // 임차인 공동명의인 1
        ContractBaseReqDto.PersonInfo coLessee1 = new ContractBaseReqDto.PersonInfo();
        ReflectionTestUtils.setField(coLessee1, "name", "박민지");
        ReflectionTestUtils.setField(coLessee1, "address", "경기도 성남시 분당구 정자동 111-22");
        ReflectionTestUtils.setField(coLessee1, "phone", "031-555-6666");
        ReflectionTestUtils.setField(coLessee1, "mobile", "010-5555-6666");

        // 임차인 공동명의인 2
        ContractBaseReqDto.PersonInfo coLessee2 = new ContractBaseReqDto.PersonInfo();
        ReflectionTestUtils.setField(coLessee2, "name", "정수아");
        ReflectionTestUtils.setField(coLessee2, "address", "인천광역시 연수구 송도동 333-44");
        ReflectionTestUtils.setField(coLessee2, "phone", "032-333-4444");
        ReflectionTestUtils.setField(coLessee2, "mobile", "010-3333-4444");

        ReflectionTestUtils.setField(dto, "lessees", List.of(dto.getLessees().get(0), coLessee1, coLessee2));

        String html = service.renderContractHtml(dto);

        Path output = Path.of("build/contract_preview_6persons.html");
        Files.writeString(output, html);
        System.out.println(">>> 크롬에서 열기: " + output.toAbsolutePath());
    }

    @Test
    void renderContractHtml_정상_렌더링() {
        ContractBaseReqDto dto = buildSampleDto();

        String html = service.renderContractHtml(dto);

        // 주소
        assertThat(html).contains("서울특별시 강남구 테헤란로 123");
        assertThat(html).contains("101동");
        assertThat(html).contains("1501호");

        // 토지
        assertThat(html).contains("대");
        assertThat(html).contains("12345분의 33");
        assertThat(html).contains("25.5㎡");

        // 건물
        assertThat(html).contains("철근콘크리트");
        assertThat(html).contains("오피스텔");
        assertThat(html).contains("84.5㎡");

        // 금액
        assertThat(html).contains("정 150,000,000원");  // 보증금
        assertThat(html).contains("정 15,000,000원");    // 계약금
        assertThat(html).contains("정 500,000원");        // 월세

        // 기간
        assertThat(html).contains("2026년 05월 01일");   // 시작일
        assertThat(html).contains("24");                 // 기간
        assertThat(html).contains("2028년 04월 30일");   // 종료일

        // 임대인
        assertThat(html).contains("홍길동");
        assertThat(html).contains("010-1234-5678");
        // 임차인
        assertThat(html).contains("김철수");
        assertThat(html).contains("010-9876-5432");

        // 특약
        assertThat(html).contains("시설물 파손 시 원상복구");

        // 월세 지불 방식
        assertThat(html).contains("후불");
        assertThat(html).contains("25");
    }

    @Test
    void renderContractHtml_공동명의인_포함() {
        ContractBaseReqDto dto = buildSampleDto();

        // 공동임대인 추가
        ContractBaseReqDto.PersonInfo coLessor = new ContractBaseReqDto.PersonInfo();
        ReflectionTestUtils.setField(coLessor, "name", "이영희");
        ReflectionTestUtils.setField(coLessor, "address", "서울시 서초구");
        ReflectionTestUtils.setField(coLessor, "mobile", "010-1111-2222");

        List<ContractBaseReqDto.PersonInfo> lessors = List.of(dto.getLessors().get(0), coLessor);
        ReflectionTestUtils.setField(dto, "lessors", lessors);

        String html = service.renderContractHtml(dto);

        assertThat(html).contains("홍길동");
        assertThat(html).contains("이영희");
        // "공동명의인" 텍스트가 포함되어야 함
        assertThat(html).contains("공동");
    }

    @Test
    void renderContractHtml_특약없음() {
        ContractBaseReqDto dto = buildSampleDto();
        ReflectionTestUtils.setField(dto, "specialTerms", null);

        String html = service.renderContractHtml(dto);

        assertThat(html).contains("해당 사항 없음");
    }

    @Test
    void renderContractHtml_중도금_1회() {
        ContractBaseReqDto dto = buildSampleDto();

        ContractBaseReqDto.IntermediatePayment ip = new ContractBaseReqDto.IntermediatePayment();
        ReflectionTestUtils.setField(ip, "degree", 1);
        ReflectionTestUtils.setField(ip, "amount", 10_000_000L);
        ReflectionTestUtils.setField(ip, "paymentDate", LocalDate.of(2026, 6, 1));

        ContractBaseReqDto.PaymentInfo payment = dto.getPaymentInfo();
        ReflectionTestUtils.setField(payment, "intermediatePayments", List.of(ip));

        String html = service.renderContractHtml(dto);

        assertThat(html).contains("정 10,000,000원");
        assertThat(html).contains("2026년 06월 01일");
    }

    @Test
    void generatePdf_파일출력() throws IOException {
        ContractBaseReqDto dto = buildSampleDto();

        byte[] pdf = service.generatePdf(dto);

        Path output = Path.of("build/contract_test.pdf");
        Files.write(output, pdf);
        System.out.println(">>> PDF 파일 열기: " + output.toAbsolutePath());
        assertThat(pdf).isNotEmpty();
    }

    private ContractBaseReqDto buildSampleDto() {
        ContractBaseReqDto dto = new ContractBaseReqDto();

        // AddressInfo
        ContractBaseReqDto.AddressInfo addr = new ContractBaseReqDto.AddressInfo();
        ReflectionTestUtils.setField(addr, "address", "서울특별시 강남구 테헤란로 123");
        ReflectionTestUtils.setField(addr, "dongNm", "101동");
        ReflectionTestUtils.setField(addr, "hoNm", "1501호");
        ReflectionTestUtils.setField(dto, "addressInfo", addr);

        // LandInfo
        ContractBaseReqDto.LandInfo land = new ContractBaseReqDto.LandInfo();
        ReflectionTestUtils.setField(land, "jimok", "대");
        ReflectionTestUtils.setField(land, "landRatio", "12345분의 33");
        ReflectionTestUtils.setField(land, "landArea", "25.5㎡");
        ReflectionTestUtils.setField(dto, "landInfo", land);

        // BuildingInfo
        ContractBaseReqDto.BuildingInfo building = new ContractBaseReqDto.BuildingInfo();
        ReflectionTestUtils.setField(building, "structure", "철근콘크리트");
        ReflectionTestUtils.setField(building, "usage", "오피스텔");
        ReflectionTestUtils.setField(building, "exclusiveArea", "84.5㎡");
        ReflectionTestUtils.setField(dto, "buildingInfo", building);

        ReflectionTestUtils.setField(dto, "leasePart", "해당 층 전체");

        // PaymentInfo
        ContractBaseReqDto.PaymentInfo payment = new ContractBaseReqDto.PaymentInfo();
        ReflectionTestUtils.setField(payment, "deposit", 150_000_000L);
        ReflectionTestUtils.setField(payment, "downPayment", 15_000_000L);
        ReflectionTestUtils.setField(payment, "downPaymentDate", LocalDate.of(2026, 4, 3));
        ReflectionTestUtils.setField(payment, "receiverName", "홍길동");
        ReflectionTestUtils.setField(payment, "intermediatePayments", List.of());
        ReflectionTestUtils.setField(payment, "balance", 135_000_000L);
        ReflectionTestUtils.setField(payment, "balanceDate", LocalDate.of(2026, 7, 1));
        ReflectionTestUtils.setField(payment, "monthlyRent", 500_000L);
        ReflectionTestUtils.setField(payment, "rentPaymentType", RentPaymentType.POST);
        ReflectionTestUtils.setField(payment, "rentPaymentDay", 25);
        ReflectionTestUtils.setField(dto, "paymentInfo", payment);

        // LeaseTerm
        ContractBaseReqDto.LeaseTerm term = new ContractBaseReqDto.LeaseTerm();
        ReflectionTestUtils.setField(term, "moveInDate", LocalDate.of(2026, 5, 1));
        ReflectionTestUtils.setField(term, "leaseMonths", 24);
        ReflectionTestUtils.setField(term, "leaseEndDate", LocalDate.of(2028, 4, 30));
        ReflectionTestUtils.setField(dto, "leaseTerm", term);

        // 특약
        ReflectionTestUtils.setField(dto, "specialTerms", "시설물 파손 시 원상복구\n퇴거 시 청소비 20만원");

        // 임대인
        ContractBaseReqDto.PersonInfo lessor = new ContractBaseReqDto.PersonInfo();
        ReflectionTestUtils.setField(lessor, "name", "홍길동");
        ReflectionTestUtils.setField(lessor, "address", "서울시 강남구 역삼동");
        ReflectionTestUtils.setField(lessor, "phone", "02-1234-5678");
        ReflectionTestUtils.setField(lessor, "mobile", "010-1234-5678");

        ReflectionTestUtils.setField(dto, "lessors", List.of(lessor));

        // 임차인
        ContractBaseReqDto.PersonInfo lessee = new ContractBaseReqDto.PersonInfo();
        ReflectionTestUtils.setField(lessee, "name", "김철수");
        ReflectionTestUtils.setField(lessee, "address", "서울시 마포구 합정동");
        ReflectionTestUtils.setField(lessee, "phone", "02-9876-5432");
        ReflectionTestUtils.setField(lessee, "mobile", "010-9876-5432");

        ReflectionTestUtils.setField(dto, "lessees", List.of(lessee));

        return dto;
    }
}
