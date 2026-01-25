package org.hyeong.booe.verification.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyeong.booe.verification.domain.PhoneVerification;
import org.hyeong.booe.verification.domain.type.VerificationStatus;
import org.hyeong.booe.verification.repository.PhoneVerificationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PhoneVerificationService {

    private final PhoneVerificationRepository verificationRepository;

    public void sendCode(String phoneNum) {
        // 6자리 랜덤 번호 생성
        String code = String.format("%06d", (int) (Math.random() * 1000000));

        PhoneVerification verification = PhoneVerification.builder()
                .phoneNumber(phoneNum)
                .verificationCode(code)
                .expiresAt(LocalDateTime.now().plusMinutes(3))
                .build();

        verificationRepository.save(verification);

        log.info("==================================================");
        log.info("[SMS 인증 요청 발송 시뮬레이션]");
        log.info("수신 번호 : {}", phoneNum);
        log.info("인증 코드 : {}", code);
        log.info("만료 시간 : {}", verification.getExpiresAt());
        log.info("==================================================");
    }


    public Long verifyCode(String phoneNum, String code) {
        PhoneVerification verification = verificationRepository.findTopByPhoneNumberOrderByCreatedAtDesc(phoneNum)
                .orElseThrow(() -> new IllegalArgumentException("인증 요청 내역을 찾을 수 없습니다."));

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("[Verification] 인증 시간 만료: {}", phoneNum);
            throw new IllegalStateException("인증 시간이 만료되었습니다.");
        }

        if (!verification.getVerificationCode().equals(code)) {
            log.warn("[Verification] 코드 불일치: {} (입력값: {})", phoneNum, code);
            throw new IllegalArgumentException("인증 코드가 일치하지 않습니다.");
        }

        verification.verify(); // 상태를 VERIFIED로 변경 (더티 체킹)
        log.info("[Verification] 인증 성공: {}", phoneNum);

        return verification.getId();
    }
}
