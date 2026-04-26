package org.hyeong.booe.member.dto.res;

import lombok.Getter;
import org.hyeong.booe.member.domain.Member;
import org.hyeong.booe.member.domain.MemberProfile;
import org.hyeong.booe.member.domain.type.MemberStatus;

import java.time.LocalDate;

@Getter
public class MemberInfoResDto {

    private final Long memberId;
    private final String memberCode;
    private final String name;
    private final String email;
    private final String phoneNumber;
    private final LocalDate birthDate;
    private final MemberStatus status;

    private MemberInfoResDto(Long memberId, String memberCode, String name,
                             String email, String phoneNumber,
                             LocalDate birthDate, MemberStatus status) {
        this.memberId = memberId;
        this.memberCode = memberCode;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.birthDate = birthDate;
        this.status = status;
    }

    public static MemberInfoResDto of(Member member, MemberProfile profile) {
        return new MemberInfoResDto(
                member.getId(), member.getMemberCode(),
                profile.getName(), profile.getEmail(),
                profile.getPhoneNumber(), profile.getBirthDate(),
                member.getStatus());
    }
}
