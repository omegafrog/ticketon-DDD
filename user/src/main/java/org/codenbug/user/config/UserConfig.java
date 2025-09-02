package org.codenbug.user.config;

import java.util.Map;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@Configuration
@EnableJpaRepositories(
    basePackages = {"org.codenbug.user.infra"},
    entityManagerFactoryRef = "primaryEntityManagerFactory",
    transactionManagerRef = "primaryTransactionManager"
)
@EntityScan(basePackages = {"org.codenbug.user"})
@ComponentScan(basePackages = {"org.codenbug.user"})
public class UserConfig {

}
