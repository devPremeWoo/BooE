package org.hyeong.booe.member.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hyeong.booe.exception.MissingRelatedEntityException;
import org.hyeong.booe.member.domain.type.CredentialStatus;
import org.hyeong.booe.member.domain.type.PasswordEncoderType;

@Entity
@NoArgsConstructor
@Getter
@Table(
        name = "member_credential",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "member_id"),
                @UniqueConstraint(columnNames = "login_id")
        }
)
public class MemberCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @Column(name = "login_id", nullable = false, length = 50)
    private String loginId;

    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "password_encoder", nullable = false, length = 20)
    private PasswordEncoderType passwordEncoder;

    @Enumerated(EnumType.STRING) // 이 로그인 수단의 상태. 정상 로그인, 비밀번호 오류로 잠김, 장기간 미사용, 해당 인증수단 제거
    @Column(name = "credential_Status", nullable = false, length = 20)
    private CredentialStatus status;

    // 빌더 패턴으로 생성하는 것 만들기 할까 싶긴한데, 해당 부분에 값이 늘어날 것
    // 같진 않으니 생성자로 정의해도 될듯? 라고 생각했으나 빌더가 더 편한거 같음
    @Builder
    private MemberCredential(Member member, String loginId, String encodedPassword) {

        if (member == null) {
            throw new MissingRelatedEntityException();
        }

        this.member = member;
        this.loginId = loginId;
        this.password = encodedPassword;
        this.passwordEncoder = PasswordEncoderType.BCRYPT;
        this.status = CredentialStatus.ACTIVE;
    }

//    public static MemberCredential create(
//            Member member,
//            String loginId,
//            String encodedPassword
//    ) {
//        return new MemberCredential(member, loginId, encodedPassword);
//    }
}
