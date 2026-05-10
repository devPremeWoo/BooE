package org.hyeong.booe.contract.service;

import org.hyeong.booe.contract.domain.Contract;
import org.hyeong.booe.contract.domain.ContractPaymentSchedule;
import org.hyeong.booe.contract.domain.type.PaymentType;
import org.hyeong.booe.contract.dto.req.ContractBaseReqDto;
import org.hyeong.booe.contract.dto.res.ContractListResDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hyeong.booe.contract.repository.ContractPaymentScheduleRepository;
import org.hyeong.booe.contract.repository.ContractRepository;
import org.hyeong.booe.global.fcm.FcmService;
import org.hyeong.booe.member.domain.Member;
import org.hyeong.booe.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "jwt.access_token_time=3600000",
        "booe.crypto.ci.hmac-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "booe.crypto.ci.aes-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Transactional
class ContractServiceIntegrationTest {

    @Autowired
    private ContractService contractService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ContractPaymentScheduleRepository scheduleRepository;

    @MockBean
    private FcmService fcmService;

    @PersistenceContext
    private EntityManager entityManager;

    private Long testMemberId;

    @BeforeEach
    void setUp() {
        Member member = memberRepository.save(
                Member.builder()
                        .memberCode("itest_" + UUID.randomUUID())
                        .build()
        );
        testMemberId = member.getId();

        ContractBaseReqDto dto = baseDto();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < 5; i++) {
            Contract contract = contractRepository.save(Contract.createContract(member, dto));

            scheduleRepository.save(ContractPaymentSchedule.createPaymentSchedule(
                    contract, PaymentType.BALANCE, 10_000_000L,
                    today.minusMonths(1), 1));
            scheduleRepository.save(ContractPaymentSchedule.createPaymentSchedule(
                    contract, PaymentType.MONTHLY_RENT, 500_000L,
                    today.plusMonths(1), 2));
            scheduleRepository.save(ContractPaymentSchedule.createPaymentSchedule(
                    contract, PaymentType.MONTHLY_RENT, 500_000L,
                    today.plusMonths(2), 3));
        }

        // 영속성 컨텍스트를 비워 다음 조회 시 DB에서 fresh fetch (LAZY 컬렉션이 정상 로딩되도록)
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("계약 목록 조회 시 임대인의 계약 5건이 반환된다")
    void getContracts_returns_lessor_contracts() {
        List<ContractListResDto> result = contractService.getContracts(testMemberId);

        assertThat(result).hasSize(5);
    }

    @Test
    @DisplayName("응답에 다음 결제 정보가 포함되어 있다")
    void getContracts_includes_next_payment() {
        List<ContractListResDto> result = contractService.getContracts(testMemberId);

        assertThat(result).allSatisfy(dto -> {
            assertThat(dto.getNextPaymentDueDate()).isNotNull();
            assertThat(dto.getNextPaymentAmount()).isEqualTo(500_000L);
            assertThat(dto.getNextPaymentType()).isEqualTo(PaymentType.MONTHLY_RENT);
        });
    }

    private ContractBaseReqDto baseDto() {
        ContractBaseReqDto dto = new ContractBaseReqDto();

        ContractBaseReqDto.AddressInfo addr = new ContractBaseReqDto.AddressInfo();
        ReflectionTestUtils.setField(addr, "address", "서울시 강남구 테헤란로 1");
        ReflectionTestUtils.setField(dto, "addressInfo", addr);

        ContractBaseReqDto.PaymentInfo pay = new ContractBaseReqDto.PaymentInfo();
        ReflectionTestUtils.setField(pay, "deposit", 10_000_000L);
        ReflectionTestUtils.setField(pay, "monthlyRent", 500_000L);
        ReflectionTestUtils.setField(dto, "paymentInfo", pay);

        ContractBaseReqDto.LeaseTerm term = new ContractBaseReqDto.LeaseTerm();
        ReflectionTestUtils.setField(term, "moveInDate", LocalDate.of(2026, 1, 1));
        ReflectionTestUtils.setField(term, "leaseEndDate", LocalDate.of(2028, 1, 1));
        ReflectionTestUtils.setField(term, "leaseMonths", 24);
        ReflectionTestUtils.setField(dto, "leaseTerm", term);

        return dto;
    }
}
