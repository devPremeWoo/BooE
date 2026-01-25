package org.hyeong.booe.member.repository;

import org.hyeong.booe.member.domain.Member;
import org.hyeong.booe.member.domain.MemberProfile;
import org.hyeong.booe.member.domain.type.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberProfileRepository extends JpaRepository<MemberProfile, Long> {

    /**
     * 휴대폰 번호와 회원의 상태를 조건으로 프로필 조회
     * JPQL을 사용하여 연관된 Member의 상태(status)까지 한 번에 체크합니다.
     */
    @Query("SELECT p FROM MemberProfile p " +
            "JOIN p.member m " +
            "WHERE p.phoneNumber = :phoneNum AND m.status = :status")
    Optional<MemberProfile> findByPhoneNumberAndMemberStatus(
            @Param("phoneNum") String phoneNum,
            @Param("status") MemberStatus status
    );

    Optional<MemberProfile> findByMember(Member member);

}

