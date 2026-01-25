package org.hyeong.booe.member.util;

import org.hyeong.booe.exception.MemberCodeGenerationException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class MemberCodeGenerator {

    private static final String PREFIX = "booe_";

    public String generate() {
        try {
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));

            // 2. UUID의 첫 번째 섹션(8자리) 추출
            String shortUuid = UUID.randomUUID().toString().split("-")[0];

            // 3. 조합 (booe_202601141030_a1b2c3d4)
            return PREFIX + timestamp + "_" + shortUuid;

        } catch (Exception e) {
            // 시스템 문제로 생성이 실패할 경우 정의한 예외 발생
            throw new MemberCodeGenerationException();
        }
    }
}
