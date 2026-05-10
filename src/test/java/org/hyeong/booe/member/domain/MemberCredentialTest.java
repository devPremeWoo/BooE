package org.hyeong.booe.member.domain;

import org.hyeong.booe.exception.MissingRelatedEntityException;
import org.hyeong.booe.member.domain.type.CredentialStatus;
import org.hyeong.booe.member.domain.type.PasswordEncoderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberCredentialTest {

    private Member member() {
        return Member.builder().memberCode("booe_x").build();
    }

    @Test
    @DisplayName("빌더로 생성 시 BCRYPT 인코더 + ACTIVE 상태가 기본값")
    void default_values() {
        MemberCredential credential = MemberCredential.builder()
                .member(member())
                .loginId("user01")
                .encodedPassword("encoded")
                .build();

        assertThat(credential.getLoginId()).isEqualTo("user01");
        assertThat(credential.getPassword()).isEqualTo("encoded");
        assertThat(credential.getPasswordEncoder()).isEqualTo(PasswordEncoderType.BCRYPT);
        assertThat(credential.getStatus()).isEqualTo(CredentialStatus.ACTIVE);
    }

    @Test
    @DisplayName("member가 null이면 MissingRelatedEntityException")
    void throw_when_member_null() {
        assertThatThrownBy(() -> MemberCredential.builder()
                .member(null)
                .loginId("x")
                .encodedPassword("x")
                .build())
                .isInstanceOf(MissingRelatedEntityException.class);
    }
}
