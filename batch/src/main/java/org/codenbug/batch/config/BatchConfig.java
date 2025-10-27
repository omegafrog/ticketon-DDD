package org.codenbug.batch.config;

import org.codenbug.batch.tasklet.AnalyzeTableTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Value("${batch.analyze.timeout:300}")
    private int analyzeTimeout;

    private final JdbcTemplate batchJdbcTemplate;

    public BatchConfig(JdbcTemplate batchJdbcTemplate) {
        this.batchJdbcTemplate = batchJdbcTemplate;
    }

    @Bean
    public Job analyzeStatisticsJob(JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {
        return new JobBuilder("analyzeStatisticsJob", jobRepository)
                .start(analyzeEventsStep(jobRepository, transactionManager))
                .next(analyzePurchasesStep(jobRepository, transactionManager))
                .next(analyzeTicketsStep(jobRepository, transactionManager))
                .next(analyzeUsersStep(jobRepository, transactionManager))
                .next(analyzeSeatLayoutsStep(jobRepository, transactionManager)).build();
    }

    @Bean
    public Step analyzeEventsStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {
        return new StepBuilder("analyzeEventsStep", jobRepository)
                .tasklet(new AnalyzeTableTasklet("event", batchJdbcTemplate, analyzeTimeout),
                        transactionManager)
                .build();
    }

    @Bean
    public Step analyzePurchasesStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {
        return new StepBuilder("analyzePurchasesStep", jobRepository)
                .tasklet(new AnalyzeTableTasklet("purchase", batchJdbcTemplate, analyzeTimeout),
                        transactionManager)
                .build();
    }

    @Bean
    public Step analyzeTicketsStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {
        return new StepBuilder("analyzeTicketsStep", jobRepository)
                .tasklet(new AnalyzeTableTasklet("ticket", batchJdbcTemplate, analyzeTimeout),
                        transactionManager)
                .build();
    }

    @Bean
    public Step analyzeUsersStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {
        return new StepBuilder("analyzeUsersStep", jobRepository)
                .tasklet(new AnalyzeTableTasklet("members", batchJdbcTemplate, analyzeTimeout),
                        transactionManager)
                .build();
    }

    @Bean
    public Step analyzeSeatLayoutsStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {
        return new StepBuilder("analyzeSeatLayoutsStep", jobRepository)
                .tasklet(new AnalyzeTableTasklet("seat_layout", batchJdbcTemplate, analyzeTimeout),
                        transactionManager)
                .build();
    }

    @Bean
    public Step analyzeSeatStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {
        return new StepBuilder("analyzeSeatStep", jobRepository)
                .tasklet(new AnalyzeTableTasklet("seat", batchJdbcTemplate, analyzeTimeout),
                        transactionManager)
                .build();
    }
}
