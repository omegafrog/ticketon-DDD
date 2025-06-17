package org.codenbug.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

@Configuration
class InternalConfig {
	@Bean
	public PasswordEncoder passwordEncoder(@Value("${custom.password.secret}") String secret) {
		return new Pbkdf2PasswordEncoder(secret, 16, 10,
			Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);
	}
}
