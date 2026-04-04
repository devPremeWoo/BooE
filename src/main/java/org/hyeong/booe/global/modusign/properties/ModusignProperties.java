package org.hyeong.booe.global.modusign.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "modusign")
public class ModusignProperties {
    private String apiKey;
    private String baseUrl;
}
