package org.codenbug.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;


@Configuration
@EnableJpaRepositories(basePackages = {"org.codenbug.auth.infra"},
    entityManagerFactoryRef = "primaryEntityManagerFactory",
    transactionManagerRef = "primaryTransactionManager")
@EntityScan(basePackages = {"org.codenbug.auth.domain"})
public class AuthConfig {
  @Bean
  public PasswordEncoder passwordEncoder(@Value("${custom.password.secret}") String secret) {
    return new Pbkdf2PasswordEncoder(secret, 16, 10,
        Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);
  }
}
