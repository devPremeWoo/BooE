package org.hyeong.booe.contract.dto.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hyeong.booe.contract.domain.type.RentPaymentType;

import java.time.LocalDate;
import java.util.List;

@Getter
@ToString
@NoArgsConstructor
public class ContractBaseReqDto {

    public interface TempSave {}
    public interface ReviewRequest {}
    public interface LesseeSubmit {}

    private Long contractId;  // null: 신규 생성 / non-null: 기존 계약 수정

    @NotBlank(groups = {TempSave.class, ReviewRequest.class, LesseeSubmit.class})
    private String title;

    @NotNull(groups = {TempSave.class, ReviewRequest.class, LesseeSubmit.class})
    @Valid
    private AddressInfo addressInfo;

    @NotNull(groups = {ReviewRequest.class, LesseeSubmit.class})
    @Valid
    private LandInfo landInfo;

    @NotNull(groups = {ReviewRequest.class, LesseeSubmit.class})
    @Valid
    private BuildingInfo buildingInfo;

    @NotBlank(groups = {ReviewRequest.class, LesseeSubmit.class})
    private String leasePart;

    @NotNull(groups = {ReviewRequest.class, LesseeSubmit.class})
    @Valid
    private PaymentInfo paymentInfo;

    @NotNull(groups = {ReviewRequest.class, LesseeSubmit.class})
    @Valid
    private LeaseTerm leaseTerm;

    private String specialTerms;

    @NotEmpty(groups = {ReviewRequest.class, LesseeSubmit.class})
    @Valid
    private List<PersonInfo> lessors;

    @NotEmpty(groups = LesseeSubmit.class)
    @Valid
    private List<PersonInfo> lessees;

    @Getter @NoArgsConstructor @ToString
    public static class AddressInfo {
        @NotBlank(groups = {TempSave.class, ReviewRequest.class, LesseeSubmit.class})
        private String address;
        private String dongNm;
        private String hoNm;
    }

    @Getter @NoArgsConstructor @ToString
    public static class LandInfo {
        @NotBlank(groups = {ReviewRequest.class, LesseeSubmit.class})
        private String jimok;
        private String landRatio;
        private String landArea;
    }

    @Getter @NoArgsConstructor @ToString
    public static class BuildingInfo {
        @NotBlank(groups = {ReviewRequest.class, LesseeSubmit.class})
        private String structure;
        private String usage;
        private String exclusiveArea;
    }

    @Getter @NoArgsConstructor @ToString
    public static class PaymentInfo {
        @NotNull(groups = {ReviewRequest.class, LesseeSubmit.class})
        private Long deposit;
        @NotNull(groups = {ReviewRequest.class, LesseeSubmit.class})
        private Long downPayment;
        @NotNull(groups = {ReviewRequest.class, LesseeSubmit.class})
        private LocalDate downPaymentDate;
        private String receiverName;

        private List<IntermediatePayment> intermediatePayments;

        @NotNull(groups = {ReviewRequest.class, LesseeSubmit.class})
        private Long balance;
        @NotNull(groups = {ReviewRequest.class, LesseeSubmit.class})
        private LocalDate balanceDate;
        private Long monthlyRent;
        private RentPaymentType rentPaymentType;
        private Integer rentPaymentDay;
    }

    @Getter @NoArgsConstructor @ToString
    public static class IntermediatePayment {
        private Integer degree;
        private Long amount;
        private LocalDate paymentDate;
    }

    @Getter @NoArgsConstructor @ToString
    public static class LeaseTerm {
        @NotNull(groups = {ReviewRequest.class, LesseeSubmit.class})
        private LocalDate moveInDate;
        private Integer leaseMonths;
        @NotNull(groups = {ReviewRequest.class, LesseeSubmit.class})
        private LocalDate leaseEndDate;
    }

    @Getter @NoArgsConstructor @ToString
    public static class PersonInfo {
        @NotBlank(groups = {ReviewRequest.class, LesseeSubmit.class})
        private String name;
        @NotBlank(groups = {ReviewRequest.class, LesseeSubmit.class})
        private String address;
        private String phone;
        private String mobile;
        private String ssn;  // 주민번호 앞 6자리
    }
}
