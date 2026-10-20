package org.hyeong.booe.contract;

import org.hyeong.booe.contract.domain.type.RentPaymentType;
import org.hyeong.booe.contract.dto.req.ContractBaseReqDto;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * 현실적인 계약서 PDF 렌더 비용을 재기 위한, 모든 섹션이 채워진 샘플 DTO.
 * (ContractPdfServiceTest.buildSampleDto와 동등 — 부하 테스트 공용으로 분리)
 */
public final class ContractSampleDtoFixture {

    private ContractSampleDtoFixture() {}

    public static ContractBaseReqDto build() {
        ContractBaseReqDto dto = new ContractBaseReqDto();

        ContractBaseReqDto.AddressInfo addr = new ContractBaseReqDto.AddressInfo();
        ReflectionTestUtils.setField(addr, "address", "서울특별시 강남구 테헤란로 123");
        ReflectionTestUtils.setField(addr, "dongNm", "101동");
        ReflectionTestUtils.setField(addr, "hoNm", "1501호");
        ReflectionTestUtils.setField(dto, "addressInfo", addr);

        ContractBaseReqDto.LandInfo land = new ContractBaseReqDto.LandInfo();
        ReflectionTestUtils.setField(land, "jimok", "대");
        ReflectionTestUtils.setField(land, "landRatio", "12345분의 33");
        ReflectionTestUtils.setField(land, "landArea", "25.5㎡");
        ReflectionTestUtils.setField(dto, "landInfo", land);

        ContractBaseReqDto.BuildingInfo building = new ContractBaseReqDto.BuildingInfo();
        ReflectionTestUtils.setField(building, "structure", "철근콘크리트");
        ReflectionTestUtils.setField(building, "usage", "오피스텔");
        ReflectionTestUtils.setField(building, "exclusiveArea", "84.5㎡");
        ReflectionTestUtils.setField(dto, "buildingInfo", building);

        ReflectionTestUtils.setField(dto, "leasePart", "해당 층 전체");

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

        ContractBaseReqDto.LeaseTerm term = new ContractBaseReqDto.LeaseTerm();
        ReflectionTestUtils.setField(term, "moveInDate", LocalDate.of(2026, 5, 1));
        ReflectionTestUtils.setField(term, "leaseMonths", 24);
        ReflectionTestUtils.setField(term, "leaseEndDate", LocalDate.of(2028, 4, 30));
        ReflectionTestUtils.setField(dto, "leaseTerm", term);

        ReflectionTestUtils.setField(dto, "specialTerms", "시설물 파손 시 원상복구\n퇴거 시 청소비 20만원");

        ContractBaseReqDto.PersonInfo lessor = new ContractBaseReqDto.PersonInfo();
        ReflectionTestUtils.setField(lessor, "name", "홍길동");
        ReflectionTestUtils.setField(lessor, "address", "서울시 강남구 역삼동");
        ReflectionTestUtils.setField(lessor, "phone", "02-1234-5678");
        ReflectionTestUtils.setField(lessor, "mobile", "010-1234-5678");
        ReflectionTestUtils.setField(dto, "lessors", List.of(lessor));

        ContractBaseReqDto.PersonInfo lessee = new ContractBaseReqDto.PersonInfo();
        ReflectionTestUtils.setField(lessee, "name", "김철수");
        ReflectionTestUtils.setField(lessee, "address", "서울시 마포구 합정동");
        ReflectionTestUtils.setField(lessee, "phone", "02-9876-5432");
        ReflectionTestUtils.setField(lessee, "mobile", "010-9876-5432");
        ReflectionTestUtils.setField(dto, "lessees", List.of(lessee));

        return dto;
    }
}
