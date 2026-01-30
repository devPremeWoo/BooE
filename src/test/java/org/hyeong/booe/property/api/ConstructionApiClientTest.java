package org.hyeong.booe.property.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        // .env에 있는 값을 테스트 실행 환경에 직접 주입합니다.
        "jwt.secret=QfPVGvva7yVqWUQm36spFFgNdDa0z9uGgaHdkuP/A0ctwO6TN9ma6PAqon5pRJckOA7mXM3m5nbbdejXLAjNOg=="
})
@ActiveProfiles("test")
class ConstructionApiClientTest {

    @Autowired
    private ConstructionApiClient apiClient;

    @Test
    @DisplayName("성지아파트(동/호 패턴) 조회 성공 테스트")
    void testSeongjiApartment() {
        // 성지 리벨루스: 112동 1704호 (2번째 패턴 "동/호"에서 성공 예상)
        StepVerifier.create(apiClient.fetchExposWithRetry("28185", "10600", "0002", "0012", "112", "1704"))
                .assertNext(res -> {
                    assertNotNull(res);
                    assertTrue(res.hasData());

                    var item = res.getResponse().getBody().getItems().getItem().get(0);
                    // 실제 데이터에 '호'가 붙어있는지 확인
                    assertEquals("112동", item.getDongNm());
                    assertEquals("1704호", item.getHoNm());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("더샵 퍼스트월드(동 패턴) 조회 성공 테스트")
    void testFirstWorldApartment() {
        // 더샵 퍼스트월드: 3동 5803 (1번째 패턴 "동/숫자"에서 바로 성공 예상)
        StepVerifier.create(apiClient.fetchExposWithRetry("28185", "10600", "0004", "0001", "3", "5803"))
                .assertNext(res -> {
                    assertNotNull(res);
                    assertTrue(res.hasData());

                    var item = res.getResponse().getBody().getItems().getItem().get(0);
                    // 실제 데이터에 '호'가 없는지 확인
                    assertEquals("3동", item.getDongNm());
                    assertEquals("5803", item.getHoNm());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 정보 조회 시 에러 처리 테스트")
    void testNotFoundData() {
        // 존재할 수 없는 번지수 요청
        StepVerifier.create(apiClient.fetchExposWithRetry("28185", "10600", "9999", "9999", "1", "1"))
                .expectError() // PublicDataCommunicationException 발생 예상
                .verify();
    }

    @Test
    @DisplayName("전유부 상세 데이터 원본 로그 확인")
    void checkRawResponse() {
        StepVerifier.create(apiClient.fetchExposWithRetry("28185", "10600", "0002", "0012", "112", "1704"))
                .assertNext(res -> {
                    // 1. 데이터가 비어있지 않은지 먼저 확인
                    assertNotNull(res.getResponse().getBody().getItems());

                    // 2. 첫 번째 아이템의 원본 데이터를 ToString으로 확인
                    var item = res.getResponse().getBody().getItems().getItem().get(0);
                    System.out.println("DEBUG - Raw Item: " + item.toString());
                })
                .verifyComplete();
    }
}