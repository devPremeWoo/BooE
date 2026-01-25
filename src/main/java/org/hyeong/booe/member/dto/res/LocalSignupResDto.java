package org.hyeong.booe.member.dto.res;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hyeong.booe.member.domain.Member;

@Getter
@NoArgsConstructor
public class LocalSignupResDto {

    private String memberCode;

    public LocalSignupResDto(Member member) {
        this.memberCode = member.getMemberCode();
    }
}
