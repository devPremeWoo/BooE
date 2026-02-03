package org.hyeong.booe.global.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.RequiredArgsConstructor;
import org.hyeong.booe.property.api.properties.PublicDataProperties;
import org.hyeong.booe.property.api.properties.VworldLadfrlProperties;
import org.hyeong.booe.property.api.properties.VworldLdaregProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final PublicDataProperties properties;
    private final VworldLadfrlProperties vworldLadfrlProperties;
    private final VworldLdaregProperties vworldLdaregProperties;

    @Bean
    public WebClient publicDataWebClient() {

        return createWebClient(properties.getBaseUrl(), properties.getTimeout());
    }

    @Bean
    public WebClient vworldLadfrlWebClient() {
        return createWebClient(vworldLadfrlProperties.getBaseUrl(), vworldLadfrlProperties.getTimeout());
    }

    @Bean
    public WebClient vworldLdaregWebClient() {
        return createWebClient(vworldLdaregProperties.getBaseUrl(), vworldLdaregProperties.getTimeout());
    }

    private WebClient createWebClient(String baseUrl, int timeout) {
        return WebClient.builder()
                .uriBuilderFactory(createUriBuilderFactory(baseUrl))
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(createHttpClient(timeout)))
                .build();
    }

    private SslContext createSslContext() {

        try {
            return SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
        } catch (SSLException e) {
            throw new IllegalStateException("Failed to initialize SSL context for WebClient", e);
        }
    }

    private HttpClient createHttpClient(int timeout) {
        return HttpClient.create()
                .secure(sslContextSpec -> sslContextSpec.sslContext(createSslContext()))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                .responseTimeout(Duration.ofMillis(timeout));
    }

    private DefaultUriBuilderFactory createUriBuilderFactory(String baseUrl) {    // 이중 인코딩 방지 설정. 이미 인코딩 된 키 값 또 인코딩하는 것 방지하기 위함
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(baseUrl);
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        return factory;
    }
}