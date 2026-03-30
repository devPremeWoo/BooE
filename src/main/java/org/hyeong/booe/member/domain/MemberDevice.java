package org.hyeong.booe.member.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hyeong.booe.global.entity.BaseEntity;
import org.hyeong.booe.member.domain.type.Platform;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_device",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "device_id"}))
public class MemberDevice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "fcm_token", nullable = false)
    private String fcmToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 10)
    private Platform platform;

    @Builder
    private MemberDevice(Member member, String deviceId, String fcmToken, Platform platform) {
        this.member = member;
        this.deviceId = deviceId;
        this.fcmToken = fcmToken;
        this.platform = platform;
    }

    public static MemberDevice create(Member member, String deviceId, String fcmToken, Platform platform) {
        return MemberDevice.builder()
                .member(member)
                .deviceId(deviceId)
                .fcmToken(fcmToken)
                .platform(platform)
                .build();
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}
