package org.hyeong.booe.member.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hyeong.booe.exception.InvalidBuildException;
import org.hyeong.booe.exception.MissingRelatedEntityException;
import org.hyeong.booe.global.entity.BaseEntity;
import org.hyeong.booe.member.domain.type.Gender;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Getter
@Table(
        name = "member_identity_verification",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "member_id"),
                @UniqueConstraint(columnNames = "ci_hash")
        },
        indexes = {
                @Index(name = "idx_miv_verified_phone", columnList = "verified_phone")
        }
)
public class MemberIdentityVerification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @Column(name = "ci_hash", nullable = false, length = 64)
    private String ciHash;

    @Column(name = "ci_enc", nullable = false, columnDefinition = "VARBINARY(255)")
    private byte[] ciEnc;

    @Column(name = "verified_name", nullable = false, length = 50)
    private String verifiedName;

    @Column(name = "verified_birth", nullable = false)
    private LocalDate verifiedBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "verified_gender", nullable = false, length = 6)
    private Gender verifiedGender;

    @Column(name = "verified_phone", nullable = false, length = 20)
    private String verifiedPhone;

    @Column(name = "verified_at", nullable = false)
    private LocalDateTime verifiedAt;

    @Column(name = "provider_tx_id", length = 100)
    private String providerTxId;

    @Builder
    private MemberIdentityVerification(
            Member member,
            String ciHash,
            byte[] ciEnc,
            String verifiedName,
            LocalDate verifiedBirth,
            Gender verifiedGender,
            String verifiedPhone,
            String providerTxId
    ) {
        if (member == null) {
            throw new MissingRelatedEntityException();
        }
        if (ciHash == null || ciHash.isBlank()
                || ciEnc == null || ciEnc.length == 0
                || verifiedName == null || verifiedName.isBlank()
                || verifiedBirth == null
                || verifiedGender == null
                || verifiedPhone == null || verifiedPhone.isBlank()) {
            throw new InvalidBuildException();
        }

        this.member = member;
        this.ciHash = ciHash;
        this.ciEnc = ciEnc;
        this.verifiedName = verifiedName;
        this.verifiedBirth = verifiedBirth;
        this.verifiedGender = verifiedGender;
        this.verifiedPhone = verifiedPhone;
        this.verifiedAt = LocalDateTime.now();
        this.providerTxId = providerTxId;
    }

    public void reverifyPhone(String newVerifiedPhone, String providerTxId) {
        if (newVerifiedPhone == null || newVerifiedPhone.isBlank()) {
            throw new InvalidBuildException();
        }
        this.verifiedPhone = newVerifiedPhone;
        this.verifiedAt = LocalDateTime.now();
        this.providerTxId = providerTxId;
    }
}
