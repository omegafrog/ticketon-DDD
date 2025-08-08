package org.codenbug.gateway.config;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

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
    
    private ConnectionProvider connectionProvider;
    
    @Bean
    public ConnectionProvider connectionProvider() {
        this.connectionProvider = ConnectionProvider.builder("gateway-connection-pool")
                .maxConnections(2000)
                .maxIdleTime(Duration.ofSeconds(30))
                .maxLifeTime(Duration.ofMinutes(5))
                .pendingAcquireTimeout(Duration.ofSeconds(30))
                .evictInBackground(Duration.ofSeconds(120))
                .build();
        return this.connectionProvider;
    }
    
    @Bean
    public HttpClient httpClient(ConnectionProvider connectionProvider) {
        return HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS))
                            .addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS)));
    }
    
    @Bean
    public ReactorClientHttpConnector reactorClientHttpConnector(HttpClient httpClient) {
        return new ReactorClientHttpConnector(httpClient);
    }
    
    @Scheduled(fixedRate = 30000)
    public void logConnectionPoolStats() {
        if (connectionProvider != null) {
            try {
                log.info("=== Gateway Connection Pool Status ===");
                log.info("Connection Provider: {}", connectionProvider.getClass().getSimpleName());
                log.info("Pool Name: gateway-connection-pool");
                log.info("Max Connections: 2000");
                log.info("Max Idle Time: 30s");
                log.info("======================================");
            } catch (Exception e) {
                log.debug("Unable to get detailed connection pool metrics: {}", e.getMessage());
            }
        }
    }
}

