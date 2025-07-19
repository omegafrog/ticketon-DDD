package org.codenbug.gateway.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class GatewayConfig {
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
}

