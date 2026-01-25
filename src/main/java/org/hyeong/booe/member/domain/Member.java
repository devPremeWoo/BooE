package org.hyeong.booe.member.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hyeong.booe.exception.InvalidBuildException;
import org.hyeong.booe.global.entity.BaseEntity;
import org.hyeong.booe.member.domain.type.MemberStatus;
import org.hyeong.booe.member.domain.type.Role;

@Entity
@NoArgsConstructor
@Table(name = "member")
@Getter
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private Role role;

    @Column(name = "status") // 서비스에서 계정주의 상태
    private MemberStatus status;

    @Column(name = "member_code", nullable = false, unique = true, updatable = false)
    private String memberCode;

    @Builder
    private Member(String memberCode) {

        if (memberCode == null || memberCode.isBlank()) {
            throw new InvalidBuildException();
        }

        this.role = Role.MEMBER;
        this.status = MemberStatus.ACTIVE;
        this.memberCode = memberCode;
    }

//    private static String generateMemberCode() {
//        return "booe_" + UUID.randomUUID()
//                .toString()
//                .replace("-", "")
//                .substring(0, 12);
//    }


}
