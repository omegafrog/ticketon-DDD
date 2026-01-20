package org.codenbug.gateway.config;

import java.util.ArrayList;
import java.util.List;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayOpenApiConfig {

    @Bean
    public List<GroupedOpenApi> apis(RouteDefinitionLocator locator) {
        List<GroupedOpenApi> groups = new ArrayList<>();
        List<RouteDefinition> definitions = locator.getRouteDefinitions().collectList().block();

        if (definitions != null) {
            definitions.stream()
                    .filter(routeDefinition -> routeDefinition.getId().contains("service"))
                    .forEach(routeDefinition -> {
                        String name = routeDefinition.getId().replaceAll("-service", "");
                        groups.add(GroupedOpenApi.builder().pathsToMatch("/" + name + "/**")
                                .group(name).build());
                    });
        }

        // 각 서비스별 수동 그룹 정의
        groups.add(GroupedOpenApi.builder().pathsToMatch("/api/v1/auth/**").group("auth")
                .displayName("Authentication Service").build());

        groups.add(GroupedOpenApi.builder().pathsToMatch("/api/v1/events/**").group("event")
                .displayName("Event Service").build());

        groups.add(GroupedOpenApi.builder().pathsToMatch("/api/v1/payments/**").group("purchase")
                .displayName("Purchase Service").build());

        groups.add(GroupedOpenApi.builder().pathsToMatch("/api/v1/users/**").group("user")
                .displayName("User Service").build());

        groups.add(GroupedOpenApi.builder().pathsToMatch("/api/v1/seats/**").group("seat")
                .displayName("Seat Service").build());

        groups.add(GroupedOpenApi.builder().pathsToMatch("/api/v1/broker/**").group("broker")
                .displayName("Broker Service").build());

        groups.add(GroupedOpenApi.builder().pathsToMatch("/static/**").group("app")
                .displayName("App Service (Static)").build());

        return groups;
    }
}
