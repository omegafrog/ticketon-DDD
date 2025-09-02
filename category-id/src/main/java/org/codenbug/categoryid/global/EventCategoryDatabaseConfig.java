package org.codenbug.categoryid.global;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;


@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
	basePackages = {"org.codenbug.categoryid.infra"},
	entityManagerFactoryRef = "EventCategoryPrimaryEntityManagerFactory",
	transactionManagerRef = "EventCategoryPrimaryTransactionManager"
)
public class EventCategoryDatabaseConfig {
	@Bean(name = "EventCategoryPrimaryEntityManagerFactory")
	public LocalContainerEntityManagerFactoryBean eventCategoryPrimaryEntityManagerFactory(
		@Qualifier("primaryDataSource") DataSource primaryDataSource) {

		LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
		em.setDataSource(primaryDataSource);
		em.setPackagesToScan("org.codenbug.categoryid.domain");

		HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		em.setJpaVendorAdapter(vendorAdapter);

		Map<String, Object> properties = new HashMap<>();
		properties.put("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
		properties.put("hibernate.hbm2ddl.auto", "update");
		properties.put("hibernate.jdbc.batch_size", 100);
		em.setJpaPropertyMap(properties);

		return em;
	}

	@Bean(name = "eventCategoryPrimaryTransactionManager")
	public PlatformTransactionManager EventCategoryPrimaryTransactionManager(
		@Qualifier("EventCategoryPrimaryEntityManagerFactory") EntityManagerFactory eventCategoryPrimaryEntityManagerFactory) {
		return new JpaTransactionManager(eventCategoryPrimaryEntityManagerFactory);
	}

	// // Primary EntityManagerFactory alias for backward compatibility
	// @Bean(name = "entityManagerFactory")
	// public LocalContainerEntityManagerFactoryBean entityManagerFactory(
	// 	@Qualifier("primaryDataSource") DataSource primaryDataSource) {
	// 	return eventCategoryPrimaryEntityManagerFactory(primaryDataSource);
	// }
}
