package org.codenbug.gateway.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Slf4j
@Configuration
@EnableScheduling
public class GatewayConfig {
    @Value("${spring.cloud.gateway.httpclient.pool.max-connections:20000}")
    private int httpClientPoolMaxConnections;

    @Value("${gateway.http-client.pool.max-idle-time:20s}")
    private Duration httpClientPoolMaxIdleTime;

    @Value("${gateway.http-client.pool.max-life-time:3m}")
    private Duration httpClientPoolMaxLifeTime;

    @Value("${gateway.http-client.pool.acquire-timeout:4s}")
    private Duration httpClientPoolAcquireTimeout;

    @Value("${gateway.http-client.pool.evict-in-background:120s}")
    private Duration httpClientPoolEvictInBackground;

    @Value("${gateway.http-client.connect-timeout:2s}")
    private Duration httpClientConnectTimeout;

    @Value("${gateway.http-client.read-timeout:12s}")
    private Duration httpClientReadTimeout;

    @Value("${gateway.http-client.write-timeout:12s}")
    private Duration httpClientWriteTimeout;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 허용할 Origin
        config.setAllowedOriginPatterns(Arrays.asList("*"));
        // 허용할 HTTP Method
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 허용할 Header
        config.addAllowedHeader("*");
        // 인증 정보 허용
        config.setAllowCredentials(true);
        // preflight 캐시 (초)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 모든 경로에 CORS 정책 적용
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }

    private ConnectionProvider connectionProvider;

    @Bean
    public ConnectionProvider connectionProvider() {
        this.connectionProvider = ConnectionProvider.builder("gateway-connection-pool")
                .maxConnections(httpClientPoolMaxConnections)
                .maxIdleTime(httpClientPoolMaxIdleTime)
                .maxLifeTime(httpClientPoolMaxLifeTime)
                .pendingAcquireTimeout(httpClientPoolAcquireTimeout)
                .evictInBackground(httpClientPoolEvictInBackground)
                .build();
        return this.connectionProvider;
    }

    @Bean
    public HttpClient httpClient(ConnectionProvider connectionProvider) {
        return HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) httpClientConnectTimeout.toMillis())
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(httpClientReadTimeout.toSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(httpClientWriteTimeout.toSeconds(), TimeUnit.SECONDS)));
    }

    @Bean
    public ReactorClientHttpConnector reactorClientHttpConnector(HttpClient httpClient) {
        return new ReactorClientHttpConnector(httpClient);
    }
}
