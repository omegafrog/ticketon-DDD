package org.codenbug.app.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * App 모듈의 JPA 설정
 * Notification 엔티티와 리포지토리를 포함
 */
@Configuration
@EntityScan(basePackages = {
    "org.codenbug.app",
    "org.codenbug.user",
    "org.codenbug.event", 
    "org.codenbug.purchase",
    "org.codenbug.notification.domain.notification.entity"
})
@EnableJpaRepositories(basePackages = {
    "org.codenbug.app",
    "org.codenbug.user",
    "org.codenbug.event",
    "org.codenbug.purchase",
    "org.codenbug.notification.infrastructure.persistence"
})
public class JpaConfig {
}