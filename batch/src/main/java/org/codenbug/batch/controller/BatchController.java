package org.codenbug.batch.controller;

import lombok.extern.slf4j.Slf4j;
import org.codenbug.batch.job.AnalyzeStatisticsScheduler;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/batch")
@Slf4j
public class BatchController {
    
    private final AnalyzeStatisticsScheduler scheduler;
    private final JobExplorer jobExplorer;
    private final JdbcTemplate batchJdbcTemplate;
    
    public BatchController(AnalyzeStatisticsScheduler scheduler, 
                          JobExplorer jobExplorer,
                          JdbcTemplate batchJdbcTemplate) {
        this.scheduler = scheduler;
        this.jobExplorer = jobExplorer;
        this.batchJdbcTemplate = batchJdbcTemplate;
    }
    
    /**
     * Manual execution of ANALYZE statistics job
     */
    @PostMapping("/analyze/run")
    public ResponseEntity<Map<String, Object>> runAnalyzeJob() {
        log.info("Manual ANALYZE job execution requested at {}", LocalDateTime.now());
        
        try {
            scheduler.runAnalyzeStatisticsJobManually();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "ANALYZE statistics job started successfully");
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to start ANALYZE job manually", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to start ANALYZE job: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Get recent job executions
     */
    @GetMapping("/analyze/history")
    public ResponseEntity<Map<String, Object>> getJobHistory() {
        try {
            List<Map<String, Object>> jobHistory = new ArrayList<>();
            
            // Get recent job instances
            Set<JobExecution> jobExecutions = jobExplorer.findRunningJobExecutions("analyzeStatisticsJob");
            jobExecutions.addAll(jobExplorer.findJobInstancesByJobName("analyzeStatisticsJob", 0, 10)
                    .stream()
                    .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
                    .toList());
            
            // Convert to response format
            jobExecutions.stream()
                    .sorted((a, b) -> b.getStartTime().compareTo(a.getStartTime()))
                    .limit(10)
                    .forEach(execution -> {
                        Map<String, Object> jobInfo = new HashMap<>();
                        jobInfo.put("executionId", execution.getId());
                        jobInfo.put("status", execution.getStatus().toString());
                        jobInfo.put("startTime", execution.getStartTime());
                        jobInfo.put("endTime", execution.getEndTime());
                        jobInfo.put("exitCode", execution.getExitStatus().getExitCode());
                        jobInfo.put("exitDescription", execution.getExitStatus().getExitDescription());
                        jobHistory.add(jobInfo);
                    });
            
            Map<String, Object> response = new HashMap<>();
            response.put("jobHistory", jobHistory);
            response.put("totalExecutions", jobHistory.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to retrieve job history", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to retrieve job history: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Health check for database connections
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> database = new HashMap<>();
        
        // Check database connection
        try {
            String dbResult = batchJdbcTemplate.queryForObject("SELECT 'OK' as status", String.class);
            database.put("status", "UP");
            database.put("result", dbResult);
            
            // Check if batch_analyze user has proper permissions
            try {
                batchJdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'ticketon'", Integer.class);
                database.put("permissions", "OK");
            } catch (Exception e) {
                database.put("permissions", "FAILED - " + e.getMessage());
            }
            
        } catch (Exception e) {
            database.put("status", "DOWN");
            database.put("error", e.getMessage());
        }
        
        response.put("status", "UP".equals(database.get("status")) ? "UP" : "DOWN");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.put("database", database);
        
        if (!"UP".equals(database.get("status"))) {
            return ResponseEntity.status(503).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get table statistics
     */
    @GetMapping("/analyze/stats")
    public ResponseEntity<Map<String, Object>> getTableStatistics() {
        try {
            String sql = """
                SELECT 
                    table_name,
                    table_rows,
                    avg_row_length,
                    data_length,
                    index_length,
                    update_time,
                    create_time
                FROM information_schema.tables 
                WHERE table_schema = 'ticketon'
                AND table_name IN ('events', 'purchases', 'tickets', 'users', 'seat_layouts')
                ORDER BY table_name
                """;
            
            List<Map<String, Object>> stats = batchJdbcTemplate.queryForList(sql);
            
            Map<String, Object> response = new HashMap<>();
            response.put("tableStatistics", stats);
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to retrieve table statistics", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to retrieve table statistics: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }
}