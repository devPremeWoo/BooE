package org.hyeong.booe.verification.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hyeong.booe.global.entity.BaseEntity;
import org.hyeong.booe.verification.domain.type.VerificationStatus;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Getter
@Table(
        name = "phone_verification",
        indexes = {
                @Index(name = "idx_phone", columnList = "phone_number")
        }
)
public class PhoneVerification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 인증 대상 전화번호 */
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    /** 인증 코드 */
    @Column(name = "verification_code", nullable = false, length = 10)
    private String verificationCode;

    /** 인증 상태 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationStatus status;

    /** 만료 시각 */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** 시도 횟수 */
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Builder
    public PhoneVerification(
            String phoneNumber,
            String verificationCode,
            LocalDateTime expiresAt
    ) {
        this.phoneNumber = phoneNumber;
        this.verificationCode = verificationCode;
        this.expiresAt = expiresAt;
        this.status = VerificationStatus.PENDING;
        this.attemptCount = 0;
    }

    public void verify() {
        this.status = VerificationStatus.VERIFIED;
    }

    public boolean isVerified() {
        return this.status == VerificationStatus.VERIFIED;
    }

    public boolean isSamePhoneNumber(String phoneNumber) {
        return this.phoneNumber.equals(phoneNumber);
    }
}
