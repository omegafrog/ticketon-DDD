package org.codenbug.event.batch.controller;

import org.codenbug.common.RsData;
import org.codenbug.event.batch.scheduler.ViewCountSyncScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ViewCount 배치 수동 실행을 위한 Controller
 * 테스트나 긴급 상황에서 수동으로 배치를 실행할 수 있음
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
public class ViewCountBatchController {
    
    private final ViewCountSyncScheduler viewCountSyncScheduler;
    
    /**
     * ViewCount 동기화 배치 수동 실행
     */
    @PostMapping("/viewcount-sync")
    public ResponseEntity<RsData<String>> runViewCountSync() {
        try {
            log.info("Manual ViewCount sync batch requested");
            
            viewCountSyncScheduler.runViewCountSyncManually();
            
            return ResponseEntity.ok(new RsData<>("200", 
                "ViewCount 동기화 배치가 성공적으로 실행되었습니다.", 
                "SUCCESS"));
                
        } catch (Exception e) {
            log.error("Failed to run manual ViewCount sync batch", e);
            
            return ResponseEntity.status(500)
                .body(new RsData<>("500", 
                    "ViewCount 동기화 배치 실행에 실패했습니다: " + e.getMessage(), 
                    "FAILED"));
        }
    }
}