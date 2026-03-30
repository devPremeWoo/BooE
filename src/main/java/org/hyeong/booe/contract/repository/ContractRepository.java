package org.hyeong.booe.contract.repository;

import org.hyeong.booe.contract.domain.Contract;
import org.hyeong.booe.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    List<Contract> findAllByMember(Member member);              // 임대인 계약 목록

    List<Contract> findAllByLesseeMember(Member lesseeMember);  // 임차인 계약 목록
}
