package org.codenbug.seat.global;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {"org.codenbug.seat.infra"},
        entityManagerFactoryRef = "primaryEntityManagerFactory",
        transactionManagerRef = "primaryTransactionManager")
@ComponentScan(basePackages = {"org.codenbug.seat", "org.codenbug.common"})
public class SeatLayoutConfig {
}
