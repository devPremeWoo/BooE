package org.hyeong.booe.member.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MemberProfileTest {

    private Member member() {
        return Member.builder().memberCode("booe_x").build();
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("회원만으로 프로필을 만들면 phoneVerified=false")
        void minimal_profile() {
            MemberProfile profile = MemberProfile.create(member());

            assertThat(profile.getPhoneVerified()).isFalse();
        }
    }

    @Nested
    @DisplayName("Builder")
    class Builder {

        @Test
        @DisplayName("phoneNumber까지 받아 생성하면 phoneVerified=true")
        void full_profile_marks_verified() {
            MemberProfile profile = MemberProfile.builder()
                    .member(member())
                    .email("a@b.com")
                    .name("홍길동")
                    .phoneNumber("01012345678")
                    .birth(LocalDate.of(1990, 1, 1))
                    .build();

            assertThat(profile.getEmail()).isEqualTo("a@b.com");
            assertThat(profile.getName()).isEqualTo("홍길동");
            assertThat(profile.getPhoneNumber()).isEqualTo("01012345678");
            assertThat(profile.getPhoneVerified()).isTrue();
            assertThat(profile.getBirthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
        }
    }

    @Nested
    @DisplayName("도메인 메서드")
    class DomainMethods {

        @Test
        @DisplayName("이름 변경")
        void update_name() {
            MemberProfile profile = MemberProfile.create(member());
            profile.updateName("새이름");

            assertThat(profile.getName()).isEqualTo("새이름");
        }

        @Test
        @DisplayName("이메일 변경")
        void update_email() {
            MemberProfile profile = MemberProfile.create(member());
            profile.updateEmail("new@booe.com");

            assertThat(profile.getEmail()).isEqualTo("new@booe.com");
        }

        @Test
        @DisplayName("휴대폰 번호 변경 시 phoneVerified가 false로 초기화")
        void change_phone_resets_verified() {
            MemberProfile profile = MemberProfile.builder()
                    .member(member())
                    .email("a@b.com").name("홍길동")
                    .phoneNumber("01000000000")
                    .birth(LocalDate.of(1990, 1, 1)).build();
            assertThat(profile.getPhoneVerified()).isTrue();

            profile.changePhoneNumber("01099998888");

            assertThat(profile.getPhoneNumber()).isEqualTo("01099998888");
            assertThat(profile.getPhoneVerified()).isFalse();
        }

        @Test
        @DisplayName("verifyPhone() 호출 시 phoneVerified=true")
        void verify_phone() {
            MemberProfile profile = MemberProfile.create(member());

            profile.verifyPhone();

            assertThat(profile.getPhoneVerified()).isTrue();
        }

        @Test
        @DisplayName("clearPhoneNumber() 시 번호와 검증 상태 모두 리셋")
        void clear_phone() {
            MemberProfile profile = MemberProfile.builder()
                    .member(member())
                    .email("a@b.com").name("홍길동")
                    .phoneNumber("01012345678")
                    .birth(LocalDate.of(1990, 1, 1)).build();

            profile.clearPhoneNumber();

            assertThat(profile.getPhoneNumber()).isNull();
            assertThat(profile.getPhoneVerified()).isFalse();
        }
    }
}
