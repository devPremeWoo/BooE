package org.hyeong.booe.member.repository;

import org.hyeong.booe.member.domain.MemberCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberCredentialRepository extends JpaRepository<MemberCredential, Long> {

    public boolean existsByLoginId(String loginId);

    Optional<MemberCredential> findByLoginId(String loginId);

    //Optional<MemberCredential> findByMemberCode(String memberCode);

    @Query("select mc from MemberCredential mc " +
            "join fetch mc.member m " +
            "where m.memberCode = :memberCode")
    Optional<MemberCredential> findByMemberCodeWithMember(@Param("memberCode") String memberCode);
}
