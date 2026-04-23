package org.hyeong.booe.payment.api.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "api.toss-payment")
public class TossPaymentProperties {
    private String baseUrl;
    private String secretKey;
    private int timeout;
}
