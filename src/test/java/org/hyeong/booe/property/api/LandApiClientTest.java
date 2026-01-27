//package org.hyeong.booe.property.api;
//
//import okhttp3.mockwebserver.MockResponse;
//import okhttp3.mockwebserver.MockWebServer;
//import org.hyeong.booe.exception.publicData.land.LandDataNotFoundException;
//import org.hyeong.booe.property.dto.response.LandResDto;
//import org.junit.jupiter.api.*;
//import org.springframework.web.reactive.function.client.WebClient;
//import reactor.test.StepVerifier;
//
//import java.io.IOException;
//
//class LandApiClientTest {
//
//    private static MockWebServer mockWebServer;
//    private LandApiClient landApiClient;
//
//    @BeforeAll
//    static void setUp() throws IOException {
//        mockWebServer = new MockWebServer();
//        mockWebServer.start();
//    }
//
//    @AfterAll
//    static void tearDown() throws IOException {
//        mockWebServer.shutdown();
//    }
//
//    @BeforeEach
//    void initialize() {
//        // MockWebServer 주소를 사용하는 WebClient 생성
//        WebClient webClient = WebClient.builder()
//                .baseUrl(mockWebServer.url("/").toString())
//                .build();
//
//        // 설정 정보 모킹 (Properties 클래스는 상황에 맞게 Mock 처리)
//        VworldProperties properties = new VworldProperties();
//        properties.setServiceKey("test-key");
//
//        landApiClient = new LandApiClient(properties, webClient);
//    }
//
//    @Test
//    @DisplayName("성공: 토지 정보를 조회하고 지번을 가공한다")
//    void fetchLandAttributes_Success() {
//        // Given: 브이월드 응답 JSON 시뮬레이션
//        String mockJsonResponse = "{" +
//                "\"ladfrlVOList\": {" +
//                "\"ladfrlVOList\": [{" +
//                "\"pnu\": \"1111010100100890025\"," +
//                "\"lndpclAr\": \"376.9\"," +
//                "\"lndcgrCodeNm\": \"대\"," +
//                "\"ldCodeNm\": \"서울특별시 종로구 청운동\"," +
//                "\"mnnmSlno\": \"89-25\"" +
//                "}]" +
//                "}}";
//
//        mockWebServer.enqueue(new MockResponse()
//                .setHeader("Content-Type", "application/json")
//                .setBody(mockJsonResponse));
//
//        // When & Then
//        StepVerifier.create(landApiClient.fetchLandAttributes("1111010100100890025"))
//                .assertNext(response -> {
//                    Assertions.assertEquals("376.9", response.getLandArea().toString());
//                    Assertions.assertEquals("대", response.getJimok());
//                    Assertions.assertEquals("0089", response.getBun()); // 가공 검증
//                    Assertions.assertEquals("0025", response.getJi());  // 가공 검증
//                    Assertions.assertEquals("서울특별시 종로구 청운동", response.getAddressNm());
//                })
//                .verifyComplete();
//    }
//
//    @Test
//    @DisplayName("실패: 결과가 없을 때 LandDataNotFoundException을 던진다")
//    void fetchLandAttributes_NotFound() {
//        // Given: 검색 결과가 없는 JSON
//        String emptyResponse = "{\"ladfrlVOList\": {\"ladfrlVOList\": []}}";
//
//        mockWebServer.enqueue(new MockResponse()
//                .setHeader("Content-Type", "application/json")
//                .setBody(emptyResponse));
//
//        // When & Then
//        StepVerifier.create(landApiClient.fetchLandAttributes("invalid-pnu"))
//                .expectError(LandDataNotFoundException.class) // [2026-01-13] 커스텀 예외 검증
//                .verify();
//    }
//}