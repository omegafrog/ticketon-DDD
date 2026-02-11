package org.codenbug.event.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class EventOpenApiConfig {

    @Bean("eventOpenAPI")
    public OpenAPI eventCustomOpenAPI() {
        return new OpenAPI()
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Gateway Server")
            ))
            .info(new Info()
                .title("Ticketon Event Service API")
                .description("티켓온 이벤트 서비스 API 문서")
                .version("v1.0")
                .contact(new Contact()
                    .name("Ticketon Team")
                    .email("team@ticketon.com")));
    }
}