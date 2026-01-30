package org.hyeong.booe.property.api;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "api.vworld.ladfrl-list")
public class VworldLadfrlProperties {
    private String baseUrl;
    private String serviceKey;
    private int timeout;
}
