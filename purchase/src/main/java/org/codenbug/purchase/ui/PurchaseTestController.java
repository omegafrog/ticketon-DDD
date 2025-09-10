package org.codenbug.purchase.ui;

import java.util.List;

import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.infra.PurchaseQueryDslRepository;
import org.codenbug.purchase.infra.PurchaseRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/test/purchase")
@RequiredArgsConstructor
public class PurchaseTestController {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseQueryDslRepository purchaseQueryDslRepository;

    /**
     * 기존 JPQL JOIN 쿼리 테스트 (Purchase 드라이빙)
     */
    @GetMapping("/original/{eventId}")
    public String testOriginalQuery(@PathVariable String eventId) {
        log.info("=== 기존 JPQL 쿼리 실행 (Purchase 드라이빙) ===");
        long startTime = System.currentTimeMillis();
        
        try {
            // 이제 String 타입으로 변경됨
            List<Purchase> purchases = purchaseRepository.findAllByEventId(eventId);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            String result = String.format("기존 쿼리 결과: %d건, 실행시간: %dms", purchases.size(), duration);
            log.info(result);
            return result;
            
        } catch (Exception e) {
            log.error("기존 쿼리 실행 중 오류: ", e);
            return "기존 쿼리 실행 실패: " + e.getMessage();
        }
    }

    /**
     * QueryDSL 최적화 쿼리 테스트 (Ticket 드라이빙)
     */
    @GetMapping("/optimized/{eventId}")
    public String testOptimizedQuery(@PathVariable String eventId) {
        log.info("=== QueryDSL 최적화 쿼리 실행 (Ticket 드라이빙) ===");
        long startTime = System.currentTimeMillis();
        
        try {
            List<Purchase> purchases = purchaseQueryDslRepository.findAllByEventIdOptimized(eventId);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            String result = String.format("최적화 쿼리 결과: %d건, 실행시간: %dms", purchases.size(), duration);
            log.info(result);
            return result;
            
        } catch (Exception e) {
            log.error("최적화 쿼리 실행 중 오류: ", e);
            return "최적화 쿼리 실행 실패: " + e.getMessage();
        }
    }

    /**
     * 두 쿼리 성능 비교 테스트
     */
    @GetMapping("/compare/{eventId}")
    public String compareQueries(@PathVariable String eventId) {
        log.info("=== 쿼리 성능 비교 테스트 시작 ===");
        
        // 1. 기존 쿼리 테스트
        long originalStart = System.currentTimeMillis();
        try {
            List<Purchase> originalResults = purchaseRepository.findAllByEventId(eventId);
            long originalDuration = System.currentTimeMillis() - originalStart;
            
            // 2. 최적화 쿼리 테스트
            long optimizedStart = System.currentTimeMillis();
            List<Purchase> optimizedResults = purchaseQueryDslRepository.findAllByEventIdOptimized(eventId);
            long optimizedDuration = System.currentTimeMillis() - optimizedStart;
            
            // 3. 결과 비교
            String result = String.format(
                "성능 비교 결과:\n" +
                "기존 쿼리: %d건, %dms\n" +
                "최적화 쿼리: %d건, %dms\n" +
                "성능 향상: %.2f%% (%dms 단축)",
                originalResults.size(), originalDuration,
                optimizedResults.size(), optimizedDuration,
                originalDuration > 0 ? ((double)(originalDuration - optimizedDuration) / originalDuration * 100) : 0,
                originalDuration - optimizedDuration
            );
            
            log.info(result);
            return result;
            
        } catch (Exception e) {
            log.error("쿼리 비교 중 오류: ", e);
            return "쿼리 비교 실패: " + e.getMessage();
        }
    }

    /**
     * 테스트용 이벤트 ID 목록 조회
     */
    @GetMapping("/events")
    public String getTestEvents() {
        return "테스트 가능한 이벤트: event_001 ~ event_100\n" +
               "예시: /api/test/purchase/compare/event_001";
    }
}