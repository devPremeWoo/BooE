package org.hyeong.booe.global.modusign;

import lombok.RequiredArgsConstructor;
import org.hyeong.booe.global.modusign.properties.ModusignProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class ModusignConfig {

    private final ModusignProperties properties;

    @Bean
    public WebClient modusignWebClient() {
        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .build();
    }
}
