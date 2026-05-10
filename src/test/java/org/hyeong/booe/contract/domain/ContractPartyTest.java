package org.hyeong.booe.contract.domain;

import org.hyeong.booe.contract.domain.type.PartyRole;
import org.hyeong.booe.contract.dto.req.ContractBaseReqDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ContractPartyTest {

    private ContractBaseReqDto.PersonInfo person(String name, String address, String phone, String mobile) {
        ContractBaseReqDto.PersonInfo info = new ContractBaseReqDto.PersonInfo();
        ReflectionTestUtils.setField(info, "name", name);
        ReflectionTestUtils.setField(info, "address", address);
        ReflectionTestUtils.setField(info, "phone", phone);
        ReflectionTestUtils.setField(info, "mobile", mobile);
        return info;
    }

    @Test
    @DisplayName("createContractParty: PersonInfo + role로 정상 생성")
    void create() {
        Contract contract = mockContractWithoutFields();
        ContractBaseReqDto.PersonInfo personInfo = person("홍길동", "서울시 강남구", "021234567", "01012345678");

        ContractParty party = ContractParty.createContractParty(contract, personInfo, PartyRole.LESSOR);

        assertThat(party.getContract()).isEqualTo(contract);
        assertThat(party.getRole()).isEqualTo(PartyRole.LESSOR);
        assertThat(party.getName()).isEqualTo("홍길동");
        assertThat(party.getAddress()).isEqualTo("서울시 강남구");
        assertThat(party.getPhone()).isEqualTo("021234567");
        assertThat(party.getMobile()).isEqualTo("01012345678");
    }

    @Test
    @DisplayName("createContractParty: 4가지 PartyRole 모두 생성 가능")
    void all_roles() {
        Contract contract = mockContractWithoutFields();
        ContractBaseReqDto.PersonInfo info = person("X", "주소", null, "01000000000");

        for (PartyRole role : PartyRole.values()) {
            ContractParty party = ContractParty.createContractParty(contract, info, role);
            assertThat(party.getRole()).isEqualTo(role);
        }
    }

    private Contract mockContractWithoutFields() {
        // Contract 객체만 필요 (id 없어도 됨)
        return new Contract();
    }
}
