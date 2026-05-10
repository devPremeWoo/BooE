package org.hyeong.booe.member.domain;

import org.hyeong.booe.member.domain.type.OauthProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MemberOauthConnectionTest {

    private Member member() {
        return Member.builder().memberCode("booe_x").build();
    }

    @Test
    @DisplayName("create()로 생성하면 lastLoginAt이 현재 시각으로 채워진다")
    void create_sets_last_login() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        MemberOauthConnection conn = MemberOauthConnection.create(
                member(), OauthProviderType.KAKAO, "kakao_user_42"
        );

        assertThat(conn.getProviderType()).isEqualTo(OauthProviderType.KAKAO);
        assertThat(conn.getProviderUserId()).isEqualTo("kakao_user_42");
        assertThat(conn.getLastLoginAt()).isAfter(before);
    }

    @Test
    @DisplayName("updateLastLogin() 호출 시 lastLoginAt이 갱신된다")
    void update_last_login() {
        MemberOauthConnection conn = MemberOauthConnection.create(
                member(), OauthProviderType.KAKAO, "kakao_user_42"
        );
        LocalDateTime before = conn.getLastLoginAt();

        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        conn.updateLastLogin();

        assertThat(conn.getLastLoginAt()).isAfter(before);
    }
}
