package org.codenbug.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Configuration
public class QueryFactoryConfig {

	@PersistenceContext(unitName = "primaryPersistenceUnit")
	private EntityManager primaryEntityManager;

	@PersistenceContext(unitName = "readOnlyPersistenceUnit")
	private EntityManager readOnlyEntityManager;

	@Primary
	@Bean(name = "primaryQueryFactory")
	public JPAQueryFactory primaryQueryFactory() {
		return new JPAQueryFactory(primaryEntityManager);
	}

	@Bean(name = "readOnlyQueryFactory")
	public JPAQueryFactory readOnlyQueryFactory(){
		return new JPAQueryFactory(readOnlyEntityManager);
	}
}
