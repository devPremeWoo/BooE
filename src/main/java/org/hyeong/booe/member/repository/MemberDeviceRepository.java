package org.hyeong.booe.member.repository;

import org.hyeong.booe.member.domain.Member;
import org.hyeong.booe.member.domain.MemberDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberDeviceRepository extends JpaRepository<MemberDevice, Long> {

    Optional<MemberDevice> findByMemberAndDeviceId(Member member, String deviceId);

    List<MemberDevice> findAllByMember(Member member);
}
