package org.codenbug.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

@Configuration
public class AppOpenApiConfig {

    @Bean("appOpenAPI")
    @Primary
    public OpenAPI appOpenAPI() {
        return new OpenAPI()
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Gateway Server")
            ))
            .info(new Info()
                .title("Ticketon App Service API")
                .description("티켓온 앱 서비스 API 문서 (파일 업로드 등)")
                .version("v1.0")
                .contact(new Contact()
                    .name("Ticketon Team")
                    .email("team@ticketon.com")));
    }

    @Bean
    public GroupedOpenApi eventApi(@Qualifier("eventOpenAPI") OpenAPI eventOpenAPI) {
        return GroupedOpenApi.builder()
                .group("event")
                .displayName("Event Service")
                .pathsToMatch("/api/v1/events/**", "/api/v1/categories/**")
                .build();
    }

    @Bean
    public GroupedOpenApi userApi(@Qualifier("userOpenAPI") OpenAPI userOpenAPI) {
        return GroupedOpenApi.builder()
                .group("user")
                .displayName("User Service")
                .pathsToMatch("/api/v1/users/**")
                .build();
    }

    @Bean
    public GroupedOpenApi purchaseApi(@Qualifier("purchaseOpenAPI") OpenAPI purchaseOpenAPI) {
        return GroupedOpenApi.builder()
                .group("purchase")
                .displayName("Purchase Service")
                .pathsToMatch("/api/v1/payments/**", "/api/v1/purchases/**", "/api/v1/refunds/**")
                .build();
    }

    @Bean
    public GroupedOpenApi seatApi(@Qualifier("seatOpenAPI") OpenAPI seatOpenAPI) {
        return GroupedOpenApi.builder()
                .group("seat")
                .displayName("Seat Service")
                .pathsToMatch("/api/v1/events/**/seats")
                .build();
    }

    @Bean
    public GroupedOpenApi appApi() {
        return GroupedOpenApi.builder()
                .group("app")
                .displayName("App Service")
                .pathsToMatch("/static/**", "/api/v1/notifications/**", "/api/v1/batch/**", "/api/test/**")
                .build();
    }
}