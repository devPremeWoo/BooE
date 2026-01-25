package org.hyeong.booe.global.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.RequiredArgsConstructor;
import org.hyeong.booe.property.api.PublicDataProperties;
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

    @Bean
    public WebClient publicDataWebClient() {

        return WebClient.builder()
                .uriBuilderFactory(createUriBuilderFactory())
                .baseUrl(properties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
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

    private HttpClient createHttpClient() {
        return HttpClient.create()
                .secure(sslContextSpec -> sslContextSpec.sslContext(createSslContext()))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getTimeout())
                .responseTimeout(Duration.ofMillis(properties.getTimeout()));
    }

    private DefaultUriBuilderFactory createUriBuilderFactory() {    // 이중 인코딩 방지 설정. 이미 인코딩 된 키 값 또 인코딩하는 것 방지하기 위함
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(properties.getBaseUrl());
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        return factory;
    }
}