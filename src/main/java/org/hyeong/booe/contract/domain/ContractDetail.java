package org.hyeong.booe.contract.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "contract_detail")
public class ContractDetail {

    @Id
    private Long contractId;

    @MapsId
    @OneToOne
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @Column(columnDefinition = "json", nullable = false)
    private String propertyInfo;

    @Column(columnDefinition = "json", nullable = false)
    private String contractClauses;

    @Column(columnDefinition = "text")
    private String specialTerms;

    @Column(length = 50)
    private String receiverName; // 영수자 성명 (임대인 혹은 대리인)

    @Column(nullable = false)
    private boolean isDownPaymentReceived = false; // 계약금 영수 여부 플래그

    private LocalDateTime receivedAt;

//    @Builder
//    private ContractDetail(Contract contract, String propertyInfo,
//                           String contractClauses, String specialTerms) {
//        this.contract = contract;
//        this.propertyInfo = propertyInfo;
//        this.contractClauses = contractClauses;
//        this.specialTerms = specialTerms;
//    }
}
