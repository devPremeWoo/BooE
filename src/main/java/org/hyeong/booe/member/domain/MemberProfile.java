package org.hyeong.booe.member.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hyeong.booe.global.entity.BaseEntity;

import java.time.LocalDate;

@Entity
@NoArgsConstructor
@Getter
@Table(
        name = "member_profile",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "member_id")
        }
)
public class MemberProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "phone_verified", nullable = false)
    private Boolean phoneVerified;

    @Column(name = "birth")
    private LocalDate birthDate;

    @Column(name = "gender")
    private String gender;

    private MemberProfile(Member member) {
        this.member = member;
        this.phoneVerified = false;
    }

    public static MemberProfile create(Member member) {
        return new MemberProfile(member);
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateEmail(String email) {
        this.email = email;
    }

    public void changePhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        this.phoneVerified = false;
    }

    public void verifyPhone() {
        this.phoneVerified = true;
    }

    public void clearPhoneNumber() {
        this.phoneNumber = null;
        this.phoneVerified = false;
    }

    @Builder
    private MemberProfile(Member member, String email, String name, String phoneNumber, LocalDate birth, String gender) {
        this.member = member;
        this.email = email;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.phoneVerified = true;
        this.birthDate = birth;
        //this.gender = gender;
    }
}
