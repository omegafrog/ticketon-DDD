package org.codenbug.app.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
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


@Configuration
public class DatabaseConfig {

    @Primary
    @Bean(name = {"primaryDataSource", "dataSource"})
    public DataSource primaryDataSource(PrimaryDataSourceProperties properties) {
        return DataSourceBuilder.create().driverClassName(properties.getDriverClassName())
                .url(properties.getUrl()).password(properties.getPassword())
                .username(properties.getUsername()).build();
    }

    @Bean(name = "readOnlyDataSource")
    public DataSource readOnlyDataSource(ReadOnlyDataSourceProperties properties) {
        return DataSourceBuilder.create().driverClassName(properties.getDriverClassName())
                .url(properties.getUrl()).username(properties.getUsername())
                .password(properties.getPassword()).build();
    }

    @Primary
    @Bean(name = "primaryEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean primaryEntityManagerFactory(
            @Qualifier("primaryDataSource") DataSource primaryDataSource) {

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(primaryDataSource);
        em.setPackagesToScan("org.codenbug.user.domain", "org.codenbug.event.domain",
                "org.codenbug.seat.domain", "org.codenbug.purchase.domain",
                "org.codenbug.notification.domain.entity", "org.codenbug.event.category.domain");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.jdbc.batch_size", 100);
        em.setJpaPropertyMap(properties);

        return em;
    }

    @Bean(name = "readOnlyEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean readOnlyEntityManagerFactory(
            @Qualifier("readOnlyDataSource") DataSource readOnlyDataSource) {

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(readOnlyDataSource);
        em.setPackagesToScan("org.codenbug.user.domain", "org.codenbug.event.domain",
                "org.codenbug.seat.domain", "org.codenbug.purchase.domain",
                "org.codenbug.notification.domain.entity", "org.codenbug.event.category.domain");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");

        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.jdbc.batch_size", 100);
        em.setJpaPropertyMap(properties);

        return em;
    }

    @Primary
    @Bean(name = {"transactionManager", "primaryTransactionManager"})
    public PlatformTransactionManager primaryTransactionManager(
            @Qualifier("primaryEntityManagerFactory") EntityManagerFactory primaryEntityManagerFactory) {
        return new JpaTransactionManager(primaryEntityManagerFactory);
    }

    @Bean(name = "readOnlyTransactionManager")
    public PlatformTransactionManager readOnlyTransactionManager(
            @Qualifier("readOnlyEntityManagerFactory") EntityManagerFactory readOnlyEntityManagerFactory) {
        return new JpaTransactionManager(readOnlyEntityManagerFactory);
    }

    @Primary
    @Bean(name = "primaryQueryFactory")
    public JPAQueryFactory primaryQueryFactory(
            @Qualifier("primaryEntityManagerFactory") EntityManagerFactory primaryEntityManagerFactory) {
        return new JPAQueryFactory(() -> primaryEntityManagerFactory.createEntityManager());
    }

    @Bean(name = "readOnlyQueryFactory")
    public JPAQueryFactory readOnlyQueryFactory(
            @Qualifier("readOnlyEntityManagerFactory") EntityManagerFactory readOnlyEntityManagerFactory) {
        return new JPAQueryFactory(() -> readOnlyEntityManagerFactory.createEntityManager());
    }
}
