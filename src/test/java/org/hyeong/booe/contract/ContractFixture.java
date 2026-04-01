package org.hyeong.booe.contract;

import org.hyeong.booe.contract.domain.Contract;
import org.hyeong.booe.contract.dto.req.ContractSaveReqDto;
import org.hyeong.booe.member.domain.Member;
import org.hyeong.booe.member.domain.MemberProfile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

public class ContractFixture {

    public static Member member(Long id, String memberCode) {
        Member m = Member.builder().memberCode(memberCode).build();
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }

    public static MemberProfile profile(Member member, String name, String email, String phoneNumber) {
        return MemberProfile.builder()
                .member(member)
                .name(name)
                .email(email)
                .phoneNumber(phoneNumber)
                .birth(LocalDate.of(1990, 1, 1))
                .build();
    }

    public static ContractSaveReqDto saveDto(Long contractId) {
        ContractSaveReqDto dto = new ContractSaveReqDto();
        ReflectionTestUtils.setField(dto, "contractId", contractId);
        ReflectionTestUtils.setField(dto, "title", "테스트 월세 계약서");

        ContractSaveReqDto.AddressInfo addressInfo = new ContractSaveReqDto.AddressInfo();
        ReflectionTestUtils.setField(addressInfo, "address", "서울시 강남구 테헤란로 123");
        ReflectionTestUtils.setField(dto, "addressInfo", addressInfo);

        ContractSaveReqDto.PaymentInfo paymentInfo = new ContractSaveReqDto.PaymentInfo();
        ReflectionTestUtils.setField(paymentInfo, "deposit", 10_000_000L);
        ReflectionTestUtils.setField(paymentInfo, "monthlyRent", 500_000L);
        ReflectionTestUtils.setField(dto, "paymentInfo", paymentInfo);

        ContractSaveReqDto.LeaseTerm leaseTerm = new ContractSaveReqDto.LeaseTerm();
        ReflectionTestUtils.setField(leaseTerm, "moveInDate", LocalDate.of(2026, 6, 1));
        ReflectionTestUtils.setField(leaseTerm, "leaseMonths", 12);
        ReflectionTestUtils.setField(leaseTerm, "leaseEndDate", LocalDate.of(2027, 5, 31));
        ReflectionTestUtils.setField(dto, "leaseTerm", leaseTerm);

        ReflectionTestUtils.setField(dto, "lessors", List.of(personInfo("홍임대", "서울시 강남구", "01011112222")));
        ReflectionTestUtils.setField(dto, "lessees", List.of(personInfo("김임차", "서울시 서초구", "01033334444")));

        return dto;
    }

    public static ContractSaveReqDto.PersonInfo personInfo(String name, String address, String mobile) {
        ContractSaveReqDto.PersonInfo info = new ContractSaveReqDto.PersonInfo();
        ReflectionTestUtils.setField(info, "name", name);
        ReflectionTestUtils.setField(info, "address", address);
        ReflectionTestUtils.setField(info, "mobile", mobile);
        return info;
    }

    public static Contract contract(Long id, Member lessor) {
        Contract c = Contract.createContract(lessor, saveDto(null));
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }
}
