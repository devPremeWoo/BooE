package org.hyeong.booe.member.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hyeong.booe.global.entity.BaseEntity;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Getter
@Table(
        name = "user_oauth_connection",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"provider_id", "provider_user_id"})
        }
)
public class userOauthConnection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    private OauthProvider oauthProvider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId; // 소셜 서비스로부터 제공받은 사용자의 고유 키

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }
}
