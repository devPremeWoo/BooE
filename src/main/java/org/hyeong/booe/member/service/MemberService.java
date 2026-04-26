package org.hyeong.booe.member.service;

import lombok.RequiredArgsConstructor;
import org.hyeong.booe.exception.MemberNotFoundException;
import org.hyeong.booe.exception.ProfileNotFoundException;
import org.hyeong.booe.global.security.jwt.RefreshTokenRedisService;
import org.hyeong.booe.member.domain.Member;
import org.hyeong.booe.member.domain.MemberProfile;
import org.hyeong.booe.member.domain.type.MemberStatus;
import org.hyeong.booe.member.dto.req.MemberUpdateReqDto;
import org.hyeong.booe.member.dto.res.MemberInfoResDto;
import org.hyeong.booe.member.repository.MemberProfileRepository;
import org.hyeong.booe.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final RefreshTokenRedisService refreshTokenRedisService;

    public MemberInfoResDto getMyInfo(Long memberId) {
        Member member = findMember(memberId);
        MemberProfile profile = findProfile(member);
        return MemberInfoResDto.of(member, profile);
    }

    @Transactional
    public void updateMyInfo(Long memberId, MemberUpdateReqDto dto) {
        Member member = findMember(memberId);
        MemberProfile profile = findProfile(member);
        profile.updateName(dto.getName());
        profile.updateEmail(dto.getEmail());
    }

    @Transactional
    public void withdraw(Long memberId) {
        Member member = findMember(memberId);
        member.withdraw();
        refreshTokenRedisService.delete(member.getMemberCode());
    }

    public void logout(String memberCode) {
        refreshTokenRedisService.delete(memberCode);
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }

    private MemberProfile findProfile(Member member) {
        return memberProfileRepository.findByMember(member)
                .orElseThrow(ProfileNotFoundException::new);
    }
}
