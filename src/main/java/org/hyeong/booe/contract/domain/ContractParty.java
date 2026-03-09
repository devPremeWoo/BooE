package org.hyeong.booe.contract.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hyeong.booe.contract.domain.type.PartyRole;

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

    @Column(name = "phone1")
    private String phone1;

    @Column(name = "phone2")
    private String phone2;

    @Column(name = "address", length = 255)
    private String address;

//    @Builder
//    private ContractParty(Contract contract, PartyRole role, String name,
//                          String phone1, String phone2, String address, String residentNoMasked) {
//        this.contract = contract;
//        this.role = role;
//        this.name = name;
//        this.phone1 = phone1;
//        this.phone2 = phone2;
//        this.address = address;
//        this.residentNoMasked = residentNoMasked;
//    }
//
//    public void updateInfo(String name, String phone1, String phone2, String address, String residentNoMasked) {
//        this.name = name;
//        this.phone1 = phone1;
//        this.phone2 = phone2;
//        this.address = address;
//        this.residentNoMasked = residentNoMasked;
//    }
}
