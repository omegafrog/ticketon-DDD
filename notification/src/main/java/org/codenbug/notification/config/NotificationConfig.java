package org.codenbug.notification.config;

import org.codenbug.notification.domain.service.NotificationDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Notification 모듈 설정 클래스 다른 모듈에서 이 설정을 Import하여 사용
 */
@Configuration
@EnableJpaRepositories(basePackages = {"org.codenbug.notification.infrastructure"},
        entityManagerFactoryRef = "primaryEntityManagerFactory",
        transactionManagerRef = "primaryTransactionManager")
@ComponentScan(basePackages = {"org.codenbug.notification"})
@EnableAspectJAutoProxy
public class NotificationConfig {

    @Bean
    public NotificationDomainService notificationDomainService() {
        return new NotificationDomainService();
    }
}
