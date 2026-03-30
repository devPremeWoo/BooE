package org.hyeong.booe.contract.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "contract_form_data")
public class ContractFormData {

    @Id
    private Long contractId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @Column(name = "form_json", columnDefinition = "json", nullable = false)
    private String formJson;

    @Builder
    private ContractFormData(Contract contract, String formJson) {
        this.contract = contract;
        this.formJson = formJson;
    }

    public static ContractFormData create(Contract contract, String formJson) {
        return ContractFormData.builder()
                .contract(contract)
                .formJson(formJson)
                .build();
    }

    public void update(String formJson) {
        this.formJson = formJson;
    }
}
