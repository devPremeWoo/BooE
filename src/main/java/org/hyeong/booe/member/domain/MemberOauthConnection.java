package org.hyeong.booe.member.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hyeong.booe.global.entity.BaseEntity;
import org.hyeong.booe.member.domain.type.OauthProviderType;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Getter
@Table(
        name = "member_oauth_connection",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"provider_type", "provider_user_id"})
        }
)
public class MemberOauthConnection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 20)
    private OauthProviderType providerType;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId; // 소셜 서비스로부터 제공받은 사용자의 고유 키

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    private MemberOauthConnection(Member member, OauthProviderType providerType, String providerUserId) {
        this.member = member;
        this.providerType = providerType;
        this.providerUserId = providerUserId;
        this.lastLoginAt = LocalDateTime.now();
    }

    public static MemberOauthConnection create(Member member, OauthProviderType providerType, String providerUserId) {
        return new MemberOauthConnection(member, providerType, providerUserId);
    }

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }
}
