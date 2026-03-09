package org.hyeong.booe.contract.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

//    @Builder
//    private ContractDetail(Contract contract, String propertyInfo,
//                           String contractClauses, String specialTerms) {
//        this.contract = contract;
//        this.propertyInfo = propertyInfo;
//        this.contractClauses = contractClauses;
//        this.specialTerms = specialTerms;
//    }
}
