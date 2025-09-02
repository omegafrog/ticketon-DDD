package org.codenbug.app.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@Configuration
@EnableJpaRepositories(
    basePackages = {
        "org.codenbug.event.query",
        "org.codenbug.purchase.ui.repository",
        "org.codenbug.user.query"
    },
    entityManagerFactoryRef = "readOnlyEntityManagerFactory",
    transactionManagerRef = "readOnlyTransactionManager"
)
public class ReadOnlyRepositoryConfig {

    @Bean(name = "readOnlyEntityManager")
    public EntityManager readOnlyEntityManager(
            @Qualifier("readOnlyEntityManagerFactory") EntityManagerFactory readOnlyEntityManagerFactory) {
        return readOnlyEntityManagerFactory.createEntityManager();
    }

    @Bean(name = "readOnlyQueryFactory")
    @Qualifier("readOnlyQueryFactory") 
    public JPAQueryFactory readOnlyQueryFactory(
            @Qualifier("readOnlyEntityManagerFactory") EntityManagerFactory readOnlyEntityManagerFactory) {
        return new JPAQueryFactory(() -> readOnlyEntityManagerFactory.createEntityManager());
    }
}