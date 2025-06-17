package org.codenbug.user.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {"org.codenbug.user"})
@EntityScan(basePackages = {"org.codenbug.user"})
@ComponentScan(basePackages = {"org.codenbug.user"})
public class UserConfig {
}
