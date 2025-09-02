package org.codenbug.auth.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;

/**
 * Auth 모듈 전용 Database 설정
 * Primary/ReadOnly 데이터소스와 JPA 인프라 구성
 */
@Configuration
public class DatabaseConfig {

    /**
     * Primary 데이터소스 (포트: 3306)
     */
    @Primary
    @Bean(name = "primaryDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * ReadOnly 데이터소스 (포트: 3307)
     */
    @Bean(name = "readOnlyDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.readonly")
    public DataSource readOnlyDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * Primary EntityManagerFactory
     */
    @Primary
    @Bean(name = "primaryEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean primaryEntityManagerFactory(
            @Qualifier("primaryDataSource") DataSource primaryDataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(primaryDataSource);
        em.setPackagesToScan("org.codenbug.auth.domain");
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaProperties(hibernateProperties());
        
        return em;
    }

    /**
     * ReadOnly EntityManagerFactory
     */
    @Bean(name = "readOnlyEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean readOnlyEntityManagerFactory(
            @Qualifier("readOnlyDataSource") DataSource readOnlyDataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(readOnlyDataSource);
        em.setPackagesToScan("org.codenbug.auth.domain");
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaProperties(hibernateProperties());
        
        return em;
    }

    /**
     * Primary TransactionManager
     */
    @Primary
    @Bean(name = {"transactionManager", "primaryTransactionManager"})
    public PlatformTransactionManager primaryTransactionManager(
            @Qualifier("primaryEntityManagerFactory") EntityManagerFactory primaryEntityManagerFactory) {
        return new JpaTransactionManager(primaryEntityManagerFactory);
    }

    /**
     * ReadOnly TransactionManager
     */
    @Bean(name = "readOnlyTransactionManager")
    public PlatformTransactionManager readOnlyTransactionManager(
            @Qualifier("readOnlyEntityManagerFactory") EntityManagerFactory readOnlyEntityManagerFactory) {
        return new JpaTransactionManager(readOnlyEntityManagerFactory);
    }

    /**
     * Primary JPAQueryFactory
     */
    @Primary
    @Bean(name = "primaryQueryFactory")
    public JPAQueryFactory primaryQueryFactory(
            @Qualifier("primaryEntityManagerFactory") EntityManagerFactory primaryEntityManagerFactory) {
        return new JPAQueryFactory(() -> primaryEntityManagerFactory.createEntityManager());
    }

    /**
     * ReadOnly JPAQueryFactory
     */
    @Bean(name = "readOnlyQueryFactory")
    public JPAQueryFactory readOnlyQueryFactory(
            @Qualifier("readOnlyEntityManagerFactory") EntityManagerFactory readOnlyEntityManagerFactory) {
        return new JPAQueryFactory(() -> readOnlyEntityManagerFactory.createEntityManager());
    }

    /**
     * Hibernate 프로퍼티 설정
     */
    private Properties hibernateProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "update");
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        properties.setProperty("hibernate.show_sql", "false");
        properties.setProperty("hibernate.format_sql", "true");
        properties.setProperty("hibernate.use_sql_comments", "true");
        properties.setProperty("hibernate.jdbc.batch_size", "20");
        properties.setProperty("hibernate.jdbc.fetch_size", "50");
        properties.setProperty("hibernate.order_inserts", "true");
        properties.setProperty("hibernate.order_updates", "true");
        properties.setProperty("hibernate.jdbc.batch_versioned_data", "true");
        return properties;
    }
}