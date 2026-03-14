package org.hyeong.booe.contract.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hyeong.booe.contract.dto.req.ContractSaveReqDto;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "contract_detail")
public class ContractDetail {

    @Id
    private Long contractId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @Column(name = "lease_part")
    private String leasePart;

    @Column(name ="property_info", columnDefinition = "json", nullable = false)
    private String propertyInfo;    // 부동산 정보

//    @Column(columnDefinition = "json", nullable = false)
//    private String contractClauses; // 계약 조항 문구

    @Column(name = "special_terms", columnDefinition = "text")
    private String specialTerms;    // 특약

    @Column(name = "receiver_name", length = 50)
    private String receiverName; // 영수자 성명 (임대인 혹은 대리인)

    @Column(name = "is_down_payment_received", nullable = false)
    private boolean isDownPaymentReceived = false; // 계약금 영수 여부 플래그. 계약서 상태가 완료되면 true로 변경

    @Builder
    private ContractDetail(Contract contract, String propertyInfo, String leasePart, String specialTerms, String receiverName) {
        this.contract = contract;
        this.propertyInfo = propertyInfo;
        this.leasePart = leasePart;
        this.specialTerms = specialTerms;
        this.receiverName = receiverName;
        this.isDownPaymentReceived = false;
    }

    public static ContractDetail createContractDetail(Contract contract, ContractSaveReqDto dto) {
        return ContractDetail.builder()
                .contract(contract)
                .propertyInfo("")
                .leasePart(dto.getLeasePart())
                .specialTerms(dto.getSpecialTerms())
                .receiverName(dto.getPaymentInfo().getReceiverName())
                .build();
    }
}
