package org.hyeong.booe.member.domain;

import org.hyeong.booe.exception.InvalidBuildException;
import org.hyeong.booe.member.domain.type.MemberStatus;
import org.hyeong.booe.member.domain.type.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberTest {

    @Nested
    @DisplayName("Member 빌더")
    class Builder {

        @Test
        @DisplayName("memberCode가 있으면 MEMBER 권한 + ACTIVE 상태로 생성된다")
        void create_with_valid_code() {
            Member member = Member.builder().memberCode("booe_abc").build();

            assertThat(member.getMemberCode()).isEqualTo("booe_abc");
            assertThat(member.getRole()).isEqualTo(Role.MEMBER);
            assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        }

        @Test
        @DisplayName("memberCode가 null이면 InvalidBuildException")
        void throw_when_code_null() {
            assertThatThrownBy(() -> Member.builder().memberCode(null).build())
                    .isInstanceOf(InvalidBuildException.class);
        }

        @Test
        @DisplayName("memberCode가 빈 문자열이면 InvalidBuildException")
        void throw_when_code_blank() {
            assertThatThrownBy(() -> Member.builder().memberCode("   ").build())
                    .isInstanceOf(InvalidBuildException.class);
        }
    }

    @Nested
    @DisplayName("withdraw()")
    class Withdraw {

        @Test
        @DisplayName("탈퇴 시 상태가 DELETED로 변경된다")
        void mark_as_deleted() {
            Member member = Member.builder().memberCode("booe_x").build();
            member.withdraw();

            assertThat(member.getStatus()).isEqualTo(MemberStatus.DELETED);
        }
    }
}
