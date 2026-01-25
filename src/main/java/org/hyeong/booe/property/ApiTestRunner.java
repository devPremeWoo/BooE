//package org.hyeong.booe.property;
//
//import lombok.extern.slf4j.Slf4j;
//import org.hyeong.booe.property.api.ConstructionApiClient;
//import org.hyeong.booe.property.dto.response.BrExposInfoResDto;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//import reactor.core.scheduler.Schedulers;
//
//import java.time.Duration;
//import java.util.List;
//import java.util.Optional;
//
//@Configuration
//@Slf4j
//public class ApiTestRunner {
//
////    @Bean
////    public CommandLineRunner testApi(ConstructionApiClient apiClient) {
////        return args -> {
////            // 우선 "동", "호" 없이 숫자로만 찔러봅니다.
////            apiClient.fetchExposDetail("28185", "10600", "0110", "0000", "2102동", "2703")
////                    .subscribe(res -> {
////                        log.info("📊 결과 개수(totalCount): {}", res.getResponse().getBody().getTotalCount());
////
////                        if (res.getResponse().getBody().getItems() != null && res.getResponse().getBody().getItems().getItem() != null) {
////                            res.getResponse().getBody().getItems().getItem().forEach(item -> {
////                                log.info("🏠 데이터 확인 -> 동: {}, 호: {}, 구분: {}, 면적: {}",
////                                        item.getDongNm(), item.getHoNm(), item.getExposPubuseGbCdNm(), item.getArea());
////                            });
////                        }
////                    }, error -> log.error("❌ 에러 발생: {}", error.getMessage()));
////        };
////    }
//@Bean
//public CommandLineRunner testApi(ConstructionApiClient apiClient) {
//    return args -> {
//        log.info("🚀 [검증] 데이터 확인된 퍼스트월드(4-1) 3동 5803호 단건 조회를 실시합니다.");
//
//        // 아까 로그에서 본 대로 '동'만 붙여서 보냅니다.
//        apiClient.fetchExposDetail("28185", "10600", "0012", "0002", "101동", "301")
//                .subscribe(res -> {
//                    if (res != null && res.getResponse().getBody().getItems() != null) {
//                        var items = res.getResponse().getBody().getItems().getItem();
//                        items.forEach(item -> {
//                            log.info("🎯 [조회 성공]");
//                            log.info("🏠 주소: {} {} {}", item.getPlatPlc(), item.getDongNm(), item.getHoNm());
//                            log.info("🏗️ 건물항목 - 구조: {}, 용도: {}", item.getStrctCdNm(), item.getMainPurpsCdNm());
//                            log.info("📏 전용면적: {}㎡ ({})", item.getArea(), item.getExposPubuseGbCdNm());
//                        });
//                    } else {
//                        log.warn("❌ 지번은 맞으나 동/호수 매칭에 실패했습니다. (totalCount: 0)");
//                    }
//                }, error -> log.error("❌ 통신 에러: {}", error.getMessage()));
//    };
//}
//}