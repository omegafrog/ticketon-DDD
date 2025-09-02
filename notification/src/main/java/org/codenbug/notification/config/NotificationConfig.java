package org.codenbug.notification.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Notification 모듈 설정 클래스
 * 다른 모듈에서 이 설정을 Import하여 사용
 */
@Configuration
@EnableJpaRepositories(
    basePackages = {"org.codenbug.notification.infrastructure"},
    entityManagerFactoryRef = "primaryEntityManagerFactory",
    transactionManagerRef = "primaryTransactionManager"
)
@ComponentScan(basePackages = {
    "org.codenbug.notification"
})
@EnableAspectJAutoProxy
public class NotificationConfig {
    // Bean 설정이 필요한 경우 여기에 추가
}