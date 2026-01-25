package org.hyeong.booe.member.repository;

import org.hyeong.booe.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

}
