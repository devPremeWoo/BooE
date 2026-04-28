package org.hyeong.booe.member.repository;

import org.hyeong.booe.member.domain.MemberOauthConnection;
import org.hyeong.booe.member.domain.type.OauthProviderType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberOauthConnectionRepository extends JpaRepository<MemberOauthConnection, Long> {

    Optional<MemberOauthConnection> findByProviderTypeAndProviderUserId(OauthProviderType providerType, String providerUserId);
}
