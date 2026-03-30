package org.hyeong.booe.contract.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hyeong.booe.contract.domain.type.ContractStatus;
import org.hyeong.booe.contract.domain.type.ContractType;
import org.hyeong.booe.contract.dto.req.ContractSaveReqDto;
import org.hyeong.booe.global.entity.BaseEntity;
import org.hyeong.booe.member.domain.Member;

import java.time.LocalDate;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "contract")
public class Contract extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name ="address", nullable = false)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ContractStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name= "type", nullable = false)
    private ContractType type;

    @Column(name = "total_deposit")
    private Long totalDeposit;

    @Column(name = "monthly_Rent")
    private Long monthlyRent;

    @Column(name = "start_date")
    private LocalDate startDate; // 임대 시작일

    @Column(name = "end_date")
    private LocalDate endDate;   // 임대 종료일

    @Column(name = "term_months")
    private Integer termMonths;  // 계약 기간 (예: 12, 24)



    @Builder
    private Contract(Member member, String title, String address, ContractStatus status, ContractType type, Long totalDeposit, Long monthlyRent, LocalDate startDate, LocalDate endDate, Integer termMonths) {
        this.member = member;
        this.title = title;
        this.address = address;
        this.status = status;
        this.type = type;
        this.totalDeposit = totalDeposit;
        this.monthlyRent = monthlyRent;
        this.startDate = startDate;        this.endDate = endDate;
        this.termMonths = termMonths;
    }

    public void update(ContractSaveReqDto dto) {
        this.title = dto.getTitle();
        this.address = dto.getAddressInfo().getAddress();
        this.totalDeposit = dto.getPaymentInfo() != null ? dto.getPaymentInfo().getDeposit() : null;
        this.monthlyRent = dto.getPaymentInfo() != null ? dto.getPaymentInfo().getMonthlyRent() : null;
        this.startDate = dto.getLeaseTerm() != null ? dto.getLeaseTerm().getMoveInDate() : null;
        this.endDate = dto.getLeaseTerm() != null ? dto.getLeaseTerm().getLeaseEndDate() : null;
        this.termMonths = dto.getLeaseTerm() != null ? dto.getLeaseTerm().getLeaseMonths() : null;
    }

    public static Contract createContract(Member member, ContractSaveReqDto dto) {
        return Contract.builder()
                .member(member)
                .title(dto.getTitle())
                .address(dto.getAddressInfo().getAddress())
                .status(ContractStatus.DRAFT)
                .type(ContractType.MONTHLY)
                .totalDeposit(dto.getPaymentInfo() != null ? dto.getPaymentInfo().getDeposit() : null)
                .monthlyRent(dto.getPaymentInfo() != null ? dto.getPaymentInfo().getMonthlyRent() : null)
                .startDate(dto.getLeaseTerm() != null ? dto.getLeaseTerm().getMoveInDate() : null)
                .endDate(dto.getLeaseTerm() != null ? dto.getLeaseTerm().getLeaseEndDate() : null)
                .termMonths(dto.getLeaseTerm() != null ? dto.getLeaseTerm().getLeaseMonths() : null)
                .build();
    }
}
