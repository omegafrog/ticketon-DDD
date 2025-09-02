package org.codenbug.batch.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
public class AnalyzeStatisticsScheduler {
    
    private final JobLauncher jobLauncher;
    private final Job analyzeStatisticsJob;
    
    public AnalyzeStatisticsScheduler(JobLauncher jobLauncher, Job analyzeStatisticsJob) {
        this.jobLauncher = jobLauncher;
        this.analyzeStatisticsJob = analyzeStatisticsJob;
    }
    
    /**
     * 매주 일요일 새벽 2시에 실행
     * Cron: 초 분 시 일 월 요일
     * 0 0 2 ? * SUN = 매주 일요일 오전 2시
     */
    @Scheduled(cron = "${batch.analyze.cron:0 0 2 ? * SUN}")
    public void runAnalyzeStatisticsJob() {
        LocalDateTime now = LocalDateTime.now();
        log.info("=== Starting weekly ANALYZE statistics job at {} ===", 
                now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        try {
            // Create unique job parameters to allow multiple executions
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("executionTime", now.toString())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            
            // Launch the job
            var jobExecution = jobLauncher.run(analyzeStatisticsJob, jobParameters);
            
            log.info("ANALYZE statistics job launched with execution ID: {}", 
                    jobExecution.getId());
            log.info("Job status: {}", jobExecution.getStatus());
            
        } catch (JobExecutionAlreadyRunningException e) {
            log.warn("ANALYZE statistics job is already running, skipping this execution", e);
        } catch (JobRestartException e) {
            log.error("Failed to restart ANALYZE statistics job", e);
        } catch (JobInstanceAlreadyCompleteException e) {
            log.warn("ANALYZE statistics job instance already completed", e);
        } catch (JobParametersInvalidException e) {
            log.error("Invalid job parameters for ANALYZE statistics job", e);
        } catch (Exception e) {
            log.error("Unexpected error during ANALYZE statistics job execution", e);
        }
    }
    
    /**
     * Manual execution method for testing or emergency runs
     */
    public void runAnalyzeStatisticsJobManually() {
        log.info("=== Manual execution of ANALYZE statistics job requested ===");
        runAnalyzeStatisticsJob();
    }
    
    /**
     * Health check method - runs every 6 hours to log scheduler status
     */
    @Scheduled(fixedRate = 21600000) // 6 hours in milliseconds
    public void healthCheck() {
        log.info("ANALYZE statistics scheduler is running. Next execution: every Sunday at 2:00 AM");
    }
}