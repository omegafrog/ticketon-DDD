package org.codenbug.broker.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class BrokerOpenApiConfig {

    @Bean("brokerOpenAPI")
    public OpenAPI brokerCustomOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Gateway Server")))
                .info(new Info().title("Ticketon Broker Service API")
                        .description("티켓온 브로커 서비스 API 문서 (대기열 및 SSE)").version("v1.0")
                        .contact(new Contact().name("Ticketon Team").email("team@ticketon.com")));
    }
}
