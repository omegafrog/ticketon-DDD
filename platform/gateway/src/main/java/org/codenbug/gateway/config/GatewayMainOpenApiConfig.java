package org.codenbug.gateway.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class GatewayMainOpenApiConfig {

    @Bean("gatewayOpenAPI")
    @Primary
    public OpenAPI gatewayOpenAPI() {
        return new OpenAPI()
                .servers(List
                        .of(new Server().url("http://localhost:8080").description("API Gateway")))
                .info(new Info().title("Ticketon API Gateway")
                        .description("티켓온 마이크로서비스 통합 API 게이트웨이").version("v1.0")
                        .contact(new Contact().name("Ticketon Team").email("team@ticketon.com")));
    }
}
