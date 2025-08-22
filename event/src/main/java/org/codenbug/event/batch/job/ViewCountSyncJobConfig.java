package org.codenbug.event.batch.job;

import org.codenbug.event.batch.dto.ViewCountSyncDto;
import org.codenbug.event.batch.processor.ViewCountSyncProcessor;
import org.codenbug.event.batch.reader.ViewCountSyncReader;
import org.codenbug.event.batch.writer.ViewCountSyncWriter;
import org.codenbug.event.query.RedisViewCountService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ViewCount 동기화 배치 Job 설정
 * Redis의 viewCount를 DB로 동기화하는 12시간 주기 배치
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ViewCountSyncJobConfig {
    
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ViewCountSyncReader viewCountSyncReader;
    private final ViewCountSyncProcessor viewCountSyncProcessor;
    private final ViewCountSyncWriter viewCountSyncWriter;
    private final RedisViewCountService redisViewCountService;
    
    // 청크 사이즈 설정 (한 번에 처리할 아이템 개수)
    private static final int CHUNK_SIZE = 100;
    
    /**
     * ViewCount 동기화 Job
     */
    @Bean
    public Job viewCountSyncJob() {
        return new JobBuilder("viewCountSyncJob", jobRepository)
            .incrementer(new RunIdIncrementer()) // 실행 시마다 새로운 JobInstance 생성
            .start(viewCountSyncStep())
            .build();
    }
    
    /**
     * ViewCount 동기화 Step
     */
    @Bean
    public Step viewCountSyncStep() {
        return new StepBuilder("viewCountSyncStep", jobRepository)
            .<ViewCountSyncDto, ViewCountSyncDto>chunk(CHUNK_SIZE, transactionManager)
            .reader(viewCountSyncReader)
            .processor(viewCountSyncProcessor)
            .writer(viewCountSyncWriter)
            .faultTolerant() // 오류 허용 설정
            .skipLimit(10) // 최대 10개까지 스킵 허용
            .skip(Exception.class) // 모든 예외에 대해 스킵 허용
            .listener(new ViewCountSyncStepListener(redisViewCountService))
            .build();
    }
    
    /**
     * Step 실행 전후 처리를 위한 리스너
     */
    public static class ViewCountSyncStepListener implements org.springframework.batch.core.StepExecutionListener {
        
        private final RedisViewCountService redisViewCountService;
        
        public ViewCountSyncStepListener(RedisViewCountService redisViewCountService) {
            this.redisViewCountService = redisViewCountService;
        }
        
        @Override
        public void beforeStep(org.springframework.batch.core.StepExecution stepExecution) {
            log.info("Starting ViewCount sync step...");
            
            // 배치 시작 전 락 확인
            if (!redisViewCountService.acquireBatchLock()) {
                log.warn("Another ViewCount sync batch is already running, terminating...");
                stepExecution.setTerminateOnly(); // 배치 중단
            }
        }
        
        @Override
        public org.springframework.batch.core.ExitStatus afterStep(org.springframework.batch.core.StepExecution stepExecution) {
            try {
                log.info("ViewCount sync step completed. Read: {}, Write: {}, Skip: {}", 
                         stepExecution.getReadCount(),
                         stepExecution.getWriteCount(), 
                         stepExecution.getSkipCount());
                
                // 처리 완료 후 락 해제
                redisViewCountService.releaseBatchLock();
                
                return stepExecution.getExitStatus();
                
            } catch (Exception e) {
                log.error("Error in ViewCount sync step afterStep", e);
                redisViewCountService.releaseBatchLock(); // 에러 시에도 락 해제
                return org.springframework.batch.core.ExitStatus.FAILED;
            }
        }
    }
}