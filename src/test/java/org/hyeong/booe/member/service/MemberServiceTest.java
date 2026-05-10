package org.hyeong.booe.member.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock private MemberRepository memberRepository;
    @Mock private MemberProfileRepository memberProfileRepository;
    @Mock private RefreshTokenRedisService refreshTokenRedisService;

    @InjectMocks private MemberService memberService;

    private Member member;
    private MemberProfile profile;

    @BeforeEach
    void setUp() {
        member = Member.builder().memberCode("booe_xyz").build();
        ReflectionTestUtils.setField(member, "id", 1L);

        profile = MemberProfile.builder()
                .member(member)
                .email("a@b.com").name("홍길동")
                .phoneNumber("01012345678")
                .birth(LocalDate.of(1990, 1, 1))
                .build();
    }

    @Nested
    @DisplayName("getMyInfo")
    class GetMyInfo {

        @Test
        @DisplayName("정상 조회 시 회원/프로필 정보를 합쳐 반환한다")
        void success() {
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
            when(memberProfileRepository.findByMember(member)).thenReturn(Optional.of(profile));

            MemberInfoResDto result = memberService.getMyInfo(1L);

            assertThat(result.getMemberCode()).isEqualTo("booe_xyz");
            assertThat(result.getName()).isEqualTo("홍길동");
            assertThat(result.getEmail()).isEqualTo("a@b.com");
            assertThat(result.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        }

        @Test
        @DisplayName("회원이 없으면 MemberNotFoundException")
        void member_not_found() {
            when(memberRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> memberService.getMyInfo(1L))
                    .isInstanceOf(MemberNotFoundException.class);
        }

        @Test
        @DisplayName("프로필이 없으면 ProfileNotFoundException")
        void profile_not_found() {
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
            when(memberProfileRepository.findByMember(member)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> memberService.getMyInfo(1L))
                    .isInstanceOf(ProfileNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateMyInfo")
    class UpdateMyInfo {

        @Test
        @DisplayName("이름과 이메일이 갱신된다")
        void update() {
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
            when(memberProfileRepository.findByMember(member)).thenReturn(Optional.of(profile));

            MemberUpdateReqDto dto = new MemberUpdateReqDto();
            ReflectionTestUtils.setField(dto, "name", "새이름");
            ReflectionTestUtils.setField(dto, "email", "new@booe.com");

            memberService.updateMyInfo(1L, dto);

            assertThat(profile.getName()).isEqualTo("새이름");
            assertThat(profile.getEmail()).isEqualTo("new@booe.com");
        }
    }

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("탈퇴 시 상태가 DELETED로 변경되고 RefreshToken이 삭제된다")
        void withdraw() {
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

            memberService.withdraw(1L);

            assertThat(member.getStatus()).isEqualTo(MemberStatus.DELETED);
            verify(refreshTokenRedisService).delete("booe_xyz");
        }

        @Test
        @DisplayName("회원이 없으면 MemberNotFoundException")
        void member_not_found() {
            when(memberRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> memberService.withdraw(1L))
                    .isInstanceOf(MemberNotFoundException.class);
        }
    }
}
