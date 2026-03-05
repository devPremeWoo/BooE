//package org.hyeong.booe.property;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.hyeong.booe.property.api.LandApiClient;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//@Component // 서버 실행 시 자동으로 동작하게 함
//@RequiredArgsConstructor
//@Slf4j
//public class ApiTestRunner implements CommandLineRunner {
//
//    private final LandApiClient landApiClient;
//
//    @Override
//    public void run(String... args) {
//        // 테스트하고 싶은 실제 PNU 번호를 입력하세요.
//        // 예: 서울특별시 종로구 청운동 89-25 (1111010100100890025)
//        String testPnu = "1111010100100890025";
//
//        log.info("🚀 [API 연결 테스트 시작] PNU: {}", testPnu);
//
//        landApiClient.fetchLandAttributes(testPnu)
//                .subscribe(
//                        response -> {
//                            log.info("✅ [API 연결 성공]");
//                            log.info("📍 주소: {}", response.getAddressNm());
//                            log.info("📍 지목: {}", response.getJimok());
//                            log.info("📍 면적: {}", response.getLandArea());
//                            log.info("📍 가공 지번: {}-{}", response.getBun(), response.getJi());
//                        },
//                        error -> {
//                            log.error("❌ [API 연결 실패]");
//                            log.error("에러 메시지: {}", error.getMessage());
//                            // 상세 원인 파악을 위해 에러 타입 출력
//                            log.error("에러 클래스: {}", error.getClass().getSimpleName());
//                        }
//                );
//    }
//}