package org.codenbug.event.batch.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.support.DatabaseType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Spring Batch 설정
 * ViewCount 동기화를 위한 배치 작업 구성
 */
@Configuration
@EnableBatchProcessing
public class BatchConfig {
    
    /**
     * JobRepository 설정
     * Spring Batch 메타데이터를 관리하는 Repository
     */
    @Bean
    public JobRepository jobRepository(DataSource dataSource, @Qualifier("primaryTransactionManager") PlatformTransactionManager transactionManager) throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTransactionManager(transactionManager);
        factory.setDatabaseType(DatabaseType.MYSQL.getProductName());
        factory.setTablePrefix("BATCH_"); // 배치 테이블 prefix
        factory.afterPropertiesSet();
        return factory.getObject();
    }
}