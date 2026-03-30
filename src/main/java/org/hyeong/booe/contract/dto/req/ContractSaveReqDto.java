package org.hyeong.booe.contract.dto.req;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hyeong.booe.contract.domain.type.RentPaymentType;

import java.time.LocalDate;
import java.util.List;

@Getter
@ToString
@NoArgsConstructor
public class ContractSaveReqDto {

    private Long contractId;  // null: 신규 생성 / non-null: 기존 계약 수정

    private String title;

    private AddressInfo addressInfo;
    private LandInfo landInfo;
    private BuildingInfo buildingInfo;
    private String leasePart;

    private PaymentInfo paymentInfo;

    private LeaseTerm leaseTerm;
    private String specialTerms;    // 특약

    private List<PersonInfo> lessors;
    private List<PersonInfo> lessees;

    @Getter @NoArgsConstructor @ToString
    public static class AddressInfo {
        private String address;
        private String dongNm;
        private String hoNm;
    }

    @Getter @NoArgsConstructor @ToString
    public static class LandInfo {
        private String jimok;
        private String landRatio;
        private String landArea;
    }

    @Getter @NoArgsConstructor @ToString
    public static class BuildingInfo {
        private String structure;
        private String usage;
        private String exclusiveArea;
    }

    @Getter @NoArgsConstructor @ToString
    public static class PaymentInfo {
        private Long deposit;
        private Long downPayment;
        private LocalDate downPaymentDate;
        private String receiverName;

        private List<IntermediatePayment> intermediatePayments;

        private Long balance;
        private LocalDate balanceDate;
        private Long monthlyRent;
        private RentPaymentType rentPaymentType; // 선/후불
        private Integer rentPaymentDay; // 지불일
    }

    @Getter @NoArgsConstructor @ToString
    public static class IntermediatePayment {
        private Integer degree;
        private Long amount;
        private LocalDate paymentDate;
    }

    @Getter @NoArgsConstructor @ToString
    public static class LeaseTerm {
        private LocalDate moveInDate;       // 인도일(입주일)
        private Integer leaseMonths;        // 임대 기간 (n개월)
        private LocalDate leaseEndDate;     // 종료일
    }

    @Getter @NoArgsConstructor @ToString
    public static class PersonInfo {
        private String name;
        private String address;
        private String phone;
        private String mobile;
    }
}
