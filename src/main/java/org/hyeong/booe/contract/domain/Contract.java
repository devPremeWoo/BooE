package org.hyeong.booe.contract.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hyeong.booe.contract.domain.type.ContractStatus;
import org.hyeong.booe.contract.domain.type.ContractType;
import org.hyeong.booe.contract.dto.req.ContractBaseReqDto;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lessee_member_id")
    private Member lesseeMember;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "address", nullable = false)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ContractStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ContractType type;

    @Column(name = "total_deposit")
    private Long totalDeposit;

    @Column(name = "monthly_rent")
    private Long monthlyRent;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "term_months")
    private Integer termMonths;

    @Column(name = "receiver_name", length = 50)
    private String receiverName;   // 계약금 영수자 이름

    @Column(name = "receiver_phone", length = 20)
    private String receiverPhone;  // 계약금 영수자 연락처

    @Column(name = "modusign_document_id", length = 100)
    private String modusignDocumentId;

    @Builder
    private Contract(Member member, String title, String address, ContractStatus status, ContractType type,
                     Long totalDeposit, Long monthlyRent, LocalDate startDate, LocalDate endDate, Integer termMonths) {
        this.member = member;
        this.title = title;
        this.address = address;
        this.status = status;
        this.type = type;
        this.totalDeposit = totalDeposit;
        this.monthlyRent = monthlyRent;
        this.startDate = startDate;
        this.endDate = endDate;
        this.termMonths = termMonths;
    }

    public static Contract createContract(Member member, ContractBaseReqDto dto) {
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

    public void update(ContractBaseReqDto dto) {
        this.title = dto.getTitle();
        this.address = dto.getAddressInfo().getAddress();
        this.totalDeposit = dto.getPaymentInfo() != null ? dto.getPaymentInfo().getDeposit() : null;
        this.monthlyRent = dto.getPaymentInfo() != null ? dto.getPaymentInfo().getMonthlyRent() : null;
        this.startDate = dto.getLeaseTerm() != null ? dto.getLeaseTerm().getMoveInDate() : null;
        this.endDate = dto.getLeaseTerm() != null ? dto.getLeaseTerm().getLeaseEndDate() : null;
        this.termMonths = dto.getLeaseTerm() != null ? dto.getLeaseTerm().getLeaseMonths() : null;
    }

    public void requestReview(Member lesseeMember) {
        this.lesseeMember = lesseeMember;
        this.status = ContractStatus.REVIEW_REQUESTED;
    }

    public void submitByLessee() {
        this.status = ContractStatus.LESSEE_SUBMITTED;
    }

    public void confirmByLessor(String receiverName, String receiverPhone) {
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.status = ContractStatus.PAYMENT_PENDING;
    }

    public void completePayment() {
        this.status = ContractStatus.PAYMENT_COMPLETED;
    }

    public void requestSign(String modusignDocumentId) {
        this.modusignDocumentId = modusignDocumentId;
        this.status = ContractStatus.SIGN_REQUESTED;
    }

    public void completeSigning() {
        this.status = ContractStatus.SIGNED;
    }
}
