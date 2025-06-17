package org.codenbug.auth.config;

import java.util.Arrays;
import java.util.List;

import org.codenbug.common.RsData;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {


	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
		http
			.authorizeHttpRequests(
				authorize -> authorize
					.requestMatchers("/**").permitAll()
			)
			.formLogin(config -> config.disable())
			.csrf(config -> config.disable())
			.exceptionHandling(config ->
				config.
					authenticationEntryPoint(
						(request, response, authException) -> {
							log.error(authException.getMessage());
							response.setStatus(401);
							response.setCharacterEncoding("utf-8");
							response.getWriter().write(
								objectMapper.writeValueAsString(new RsData<>("403", authException.getMessage(), null))
							);
						}).
					accessDeniedHandler((request1, response1, accessDeniedException) -> {
						log.error(accessDeniedException.getMessage());
						response1.setStatus(403);
						response1.setCharacterEncoding("utf-8");
						response1.getWriter().write(
							objectMapper.writeValueAsString(
								new RsData<>("403", accessDeniedException.getMessage(), null))
						);
					})).

			cors(config -> config.configurationSource(corsConfigurationSource()));

		return http.build();
	}

	/**
	 * CORS 설정을 제공하는 빈
	 *
	 * @return CORS 구성 소스
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOriginPatterns(List.of("*"));
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(
			Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "entryAuthToken"));
		configuration.setExposedHeaders(List.of("Authorization", "entryAuthToken"));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
