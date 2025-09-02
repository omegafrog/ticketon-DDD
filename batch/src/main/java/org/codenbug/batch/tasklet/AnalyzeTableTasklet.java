package org.codenbug.batch.tasklet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class AnalyzeTableTasklet implements Tasklet {
    
    private final String tableName;
    private final JdbcTemplate jdbcTemplate;
    private final int timeoutSeconds;
    
    public AnalyzeTableTasklet(String tableName, JdbcTemplate jdbcTemplate, int timeoutSeconds) {
        this.tableName = tableName;
        this.jdbcTemplate = jdbcTemplate;
        this.timeoutSeconds = timeoutSeconds;
    }
    
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("Starting ANALYZE TABLE {} at {}", tableName, startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        try {
            // Set statement timeout
            jdbcTemplate.setQueryTimeout(timeoutSeconds);
            
            // Check if table exists first
            String checkTableSql = """
                SELECT COUNT(*)
                FROM information_schema.tables 
                WHERE table_schema = DATABASE() 
                AND table_name = ?
                """;
            
            Integer tableExists = jdbcTemplate.queryForObject(checkTableSql, Integer.class, tableName);
            
            if (tableExists == null || tableExists == 0) {
                log.warn("Table {} does not exist, skipping ANALYZE", tableName);
                return RepeatStatus.FINISHED;
            }
            
            // Get table statistics before ANALYZE
            String rowCountSql = "SELECT COUNT(*) FROM " + tableName;
            Long rowCount = jdbcTemplate.queryForObject(rowCountSql, Long.class);
            log.info("Table {} has {} rows before ANALYZE", tableName, rowCount);
            
            // Execute ANALYZE TABLE
            String analyzeSql = "ANALYZE TABLE " + tableName;
            log.info("Executing: {}", analyzeSql);
            
            long analyzeStartTime = System.currentTimeMillis();
            jdbcTemplate.execute(analyzeSql);
            long analyzeEndTime = System.currentTimeMillis();
            long analyzeDuration = analyzeEndTime - analyzeStartTime;
            
            // Log results
            LocalDateTime endTime = LocalDateTime.now();
            log.info("ANALYZE TABLE {} completed successfully in {}ms at {}", 
                    tableName, analyzeDuration, endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // Optional: Get updated statistics information
            try {
                String statsSql = """
                    SELECT 
                        table_rows,
                        avg_row_length,
                        data_length,
                        index_length,
                        update_time
                    FROM information_schema.tables 
                    WHERE table_schema = DATABASE() 
                    AND table_name = ?
                    """;
                
                jdbcTemplate.queryForList(statsSql, tableName).forEach(row -> {
                    log.info("Updated statistics for {}: rows={}, avg_row_length={}, data_length={}, index_length={}, update_time={}", 
                            tableName,
                            row.get("table_rows"),
                            row.get("avg_row_length"), 
                            row.get("data_length"),
                            row.get("index_length"),
                            row.get("update_time"));
                });
                
            } catch (Exception e) {
                log.warn("Failed to retrieve updated statistics for {}: {}", tableName, e.getMessage());
            }
            
        } catch (DataAccessException e) {
            log.error("Failed to ANALYZE TABLE {}: {}", tableName, e.getMessage(), e);
            
            // Check if it's a timeout or permission issue
            if (e.getMessage() != null) {
                if (e.getMessage().contains("timeout")) {
                    log.error("ANALYZE TABLE {} timed out after {} seconds", tableName, timeoutSeconds);
                } else if (e.getMessage().contains("Access denied")) {
                    log.error("Access denied for ANALYZE TABLE {}. Please check database permissions.", tableName);
                } else if (e.getMessage().contains("doesn't exist")) {
                    log.warn("Table {} doesn't exist, skipping", tableName);
                    return RepeatStatus.FINISHED;
                }
            }
            
            throw new RuntimeException("ANALYZE TABLE " + tableName + " failed", e);
            
        } catch (Exception e) {
            log.error("Unexpected error during ANALYZE TABLE {}: {}", tableName, e.getMessage(), e);
            throw new RuntimeException("ANALYZE TABLE " + tableName + " failed with unexpected error", e);
        }
        
        return RepeatStatus.FINISHED;
    }
}