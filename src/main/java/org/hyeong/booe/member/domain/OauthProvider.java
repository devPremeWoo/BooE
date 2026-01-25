package org.hyeong.booe.member.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Getter
@Table(
        name = "oauth_provider",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "provider_key")
        }
)
public class OauthProvider {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_key", nullable = false, length = 20)
    private String providerKey; // kakao, google, apple

    @Column(name = "provider_name", nullable = false, length = 50)
    private String providerName;

}
