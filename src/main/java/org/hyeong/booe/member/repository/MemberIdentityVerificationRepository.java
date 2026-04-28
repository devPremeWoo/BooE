package org.hyeong.booe.member.repository;

import org.hyeong.booe.member.domain.Member;
import org.hyeong.booe.member.domain.MemberIdentityVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberIdentityVerificationRepository extends JpaRepository<MemberIdentityVerification, Long> {

    boolean existsByCiHash(String ciHash);

    Optional<MemberIdentityVerification> findByMember(Member member);
}
