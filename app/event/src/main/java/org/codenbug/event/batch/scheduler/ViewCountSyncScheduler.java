package org.codenbug.event.batch.scheduler;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * ViewCount 동기화 배치 스케줄러
 * 12시간마다 Redis의 viewCount를 DB로 동기화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountSyncScheduler {
    
    private final JobLauncher jobLauncher;
    private final Job viewCountSyncJob;
    
    /**
     * 12시간마다 ViewCount 동기화 배치 실행
     * cron: 0 0 0,12 * * * (매일 0시, 12시에 실행)
     */
    @Scheduled(cron = "0 0 0,12 * * *")
    public void runViewCountSync() {
        try {
            log.info("Starting ViewCount sync batch job at {}", LocalDateTime.now());
            
            // JobParameters에 현재 시간을 추가하여 매번 새로운 JobInstance 생성
            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("triggerType", "scheduled")
                .toJobParameters();
            
            // 배치 실행
            var jobExecution = jobLauncher.run(viewCountSyncJob, jobParameters);
            
            log.info("ViewCount sync batch job completed with status: {}", 
                     jobExecution.getStatus());
            
        } catch (Exception e) {
            log.error("Failed to run ViewCount sync batch job", e);
        }
    }
    
    /**
     * 수동 실행을 위한 메서드 (필요시 REST API로 호출 가능)
     */
    public void runViewCountSyncManually() {
        try {
            log.info("Starting manual ViewCount sync batch job at {}", LocalDateTime.now());
            
            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("triggerType", "manual")
                .toJobParameters();
            
            var jobExecution = jobLauncher.run(viewCountSyncJob, jobParameters);
            
            log.info("Manual ViewCount sync batch job completed with status: {}", 
                     jobExecution.getStatus());
            
        } catch (Exception e) {
            log.error("Failed to run manual ViewCount sync batch job", e);
            throw new RuntimeException("ViewCount sync batch failed", e);
        }
    }
}