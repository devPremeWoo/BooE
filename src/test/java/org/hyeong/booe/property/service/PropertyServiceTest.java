package org.hyeong.booe.property.service;

import org.hyeong.booe.global.security.jwt.JwtProvider;
import org.hyeong.booe.global.security.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // 이 부분이 핵심입니다!
import reactor.test.StepVerifier;

@SpringBootTest
class PropertyServiceTest {

    @Autowired
    private PropertyService propertyService;

    // 스프링 부트 3.4 이상에서는 @MockBean 대신 @MockitoBean을 사용합니다.
    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("성지, 퍼스트월드, 마스터뷰 전용면적 통합 테스트")
    void integratedAreaTest() {
        checkArea("송도 성지아파트", "28185", "10600", "0013", "0046", "101", "304");

        // 2. 더샵퍼스트월드
        checkArea("더샵퍼스트월드", "28185", "10600", "0004", "0001", "3", "5803");

        // 3. 더샵마스터뷰 21블럭
        checkArea("더샵마스터뷰", "28185", "10600", "0018", "0001", "2102", "2703");
    }

    private void checkArea(String name, String sigungu, String bjdong, String bun, String ji, String dong, String ho) {
        System.out.println("\n>>> [" + name + "] 조회 시작");

        propertyService.prepareAreaRequest(sigungu, bjdong, bun, ji, dong, ho)
                .flatMap(dto -> propertyService.getPrivateAreaValue(dto))
                .doOnNext(area -> {
                    System.out.println("------------------------------------");
                    System.out.println(name + " " + dong + "동 " + ho + "호");
                    System.out.println("계산된 전용면적: " + area + "㎡");
                    System.out.println("------------------------------------");
                })
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }
}