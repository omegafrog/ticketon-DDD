package org.codenbug.auth.config;

import org.codenbug.common.RsData;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

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
							response.setContentType("application/json");
							response.setCharacterEncoding("utf-8");
							response.getWriter().write(
								objectMapper.writeValueAsString(new RsData<>("403", authException.getMessage(), null))
							);
						}).
					accessDeniedHandler((request1, response1, accessDeniedException) -> {
						log.error(accessDeniedException.getMessage());
						response1.setStatus(403);
						response1.setContentType("application/json");
						response1.setCharacterEncoding("utf-8");
						response1.getWriter().write(
							objectMapper.writeValueAsString(
								new RsData<>("403", accessDeniedException.getMessage(), null))
						);
					}));


		return http.build();
	}

}
