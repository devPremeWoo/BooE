package org.hyeong.booe.contract.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hyeong.booe.contract.domain.type.PartyRole;
import org.hyeong.booe.contract.dto.req.ContractBaseReqDto;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "contract_parties")
public class ContractParty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private PartyRole role;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone")
    private String phone;

    @Column(name = "mobile")
    private String mobile;

    @Column(name = "address", length = 255)
    private String address;

    @Builder
    private ContractParty(Contract contract, PartyRole role, String name, String phone, String mobile, String address) {
        this.contract = contract;
        this.role = role;
        this.name = name;
        this.phone = phone;
        this.mobile = mobile;
        this.address = address;
    }

    public static ContractParty createContractParty(Contract contract, ContractBaseReqDto.PersonInfo personInfo, PartyRole role) {
        return ContractParty.builder()
                .contract(contract)
                .role(role)
                .name(personInfo.getName())
                .phone(personInfo.getPhone())
                .mobile(personInfo.getMobile())
                .address(personInfo.getAddress())
                .build();
    }




}
