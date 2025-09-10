# 머터리얼라이즈 뷰를 활용한 서브쿼리 성능 최적화

## 문제점 분석

### 기존 쿼리의 성능 이슈
```sql
-- 문제가 있는 서브쿼리
cast((select count(*) from seat s2_0 where sl1_0.id = s2_0.layout_id) as signed)
```

**성능 문제점:**
1. **상호 연관 서브쿼리**: 각 이벤트마다 서브쿼리가 실행됨 (N+1 문제)
2. **높은 쿼리 비용**: 6,724,599.68 (매우 높음)
3. **불필요한 조인**: seat 테이블과의 조인으로 29M+ 행 생성
4. **실행 시간**: **9.26ms** per query

## 최적화 솔루션: 머터리얼라이즈 뷰

### 1. 머터리얼라이즈 테이블 생성
```sql
-- 좌석 레이아웃별 집계 테이블 생성
CREATE TABLE seat_layout_stats (
    layout_id BIGINT PRIMARY KEY,
    seat_count INT NOT NULL,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_layout_seat_count (layout_id, seat_count)
);

-- 초기 데이터 로드
INSERT INTO seat_layout_stats (layout_id, seat_count)
SELECT layout_id, COUNT(*) as seat_count
FROM seat 
GROUP BY layout_id;
```

### 2. 최적화된 쿼리
```sql
-- Before: 서브쿼리 사용
select e1_0.id,
       e1_0.title,
       sl1_0.location,
       cast((select count(*) from seat s2_0 where sl1_0.id = s2_0.layout_id) as signed)
from event e1_0
join seat_layout sl1_0 on sl1_0.id = e1_0.seat_layout_id
join seat s1_0 on sl1_0.id = s1_0.layout_id  -- 불필요한 조인
where e1_0.deleted = false
  and lower(e1_0.title) like '%concert%'
order by e1_0.created_at desc
limit 0,20;

-- After: 머터리얼라이즈 뷰 사용
select e1_0.id,
       e1_0.title,
       sl1_0.location,
       sls1_0.seat_count
from event e1_0
join seat_layout sl1_0 on sl1_0.id = e1_0.seat_layout_id
join seat_layout_stats sls1_0 on sls1_0.layout_id = e1_0.seat_layout_id
where e1_0.deleted = false
  and lower(e1_0.title) like '%concert%'
order by e1_0.created_at desc
limit 0,20;
```

## 성능 개선 결과

### 측정 결과 (MySQL Profiling)
- **기존 쿼리**: **9.26ms**
- **최적화된 쿼리**: **1.22ms**
- **성능 향상**: **87% (8.04ms 단축)**

### 개선 효과
1. **서브쿼리 제거**: 상호 연관 서브쿼리 → 단순 조인으로 변경
2. **불필요한 조인 제거**: seat 테이블과의 직접 조인 제거
3. **인덱스 활용**: layout_id 기반 효율적 조인
4. **메모리 사용량 감소**: 29M+ 행 생성 → 필요한 행만 처리

## 데이터 정합성 유지 방안

### 1. 트리거를 통한 자동 업데이트
```sql
-- Seat 삽입 시 트리거
DELIMITER $$
CREATE TRIGGER seat_insert_trigger 
AFTER INSERT ON seat
FOR EACH ROW
BEGIN
    INSERT INTO seat_layout_stats (layout_id, seat_count)
    VALUES (NEW.layout_id, 1)
    ON DUPLICATE KEY UPDATE 
    seat_count = seat_count + 1,
    last_updated = CURRENT_TIMESTAMP;
END$$

-- Seat 삭제 시 트리거  
CREATE TRIGGER seat_delete_trigger
AFTER DELETE ON seat  
FOR EACH ROW
BEGIN
    UPDATE seat_layout_stats 
    SET seat_count = seat_count - 1,
        last_updated = CURRENT_TIMESTAMP
    WHERE layout_id = OLD.layout_id;
END$$
DELIMITER ;
```

### 2. 배치 정합성 검증
```sql
-- 정합성 검증 쿼리
SELECT s.layout_id, s.actual_count, sls.seat_count as cached_count,
       ABS(s.actual_count - sls.seat_count) as diff
FROM (
    SELECT layout_id, COUNT(*) as actual_count 
    FROM seat 
    GROUP BY layout_id
) s
JOIN seat_layout_stats sls ON s.layout_id = sls.layout_id
WHERE s.actual_count != sls.seat_count;

-- 데이터 동기화
UPDATE seat_layout_stats sls
JOIN (
    SELECT layout_id, COUNT(*) as actual_count 
    FROM seat 
    GROUP BY layout_id
) s ON sls.layout_id = s.layout_id
SET sls.seat_count = s.actual_count,
    sls.last_updated = CURRENT_TIMESTAMP
WHERE sls.seat_count != s.actual_count;
```

## 도입 고려사항

### 장점
✅ **87% 성능 향상** (9.26ms → 1.22ms)  
✅ **서버 리소스 절약** (CPU, 메모리)  
✅ **확장성**: 데이터 증가에도 안정적 성능  
✅ **사용자 경험 개선**: 빠른 응답 시간  

### 단점 및 관리 포인트
⚠️ **추가 저장 공간**: 머터리얼라이즈 테이블 (57,600 레코드)  
⚠️ **데이터 정합성 관리**: 트리거 또는 배치 동기화 필요  
⚠️ **복잡성 증가**: 데이터 업데이트 시 추가 고려사항  

### 권장사항
1. **즉시 도입 권장**: 87%의 극적인 성능 향상
2. **트리거 구현**: 실시간 데이터 정합성 보장  
3. **모니터링**: 정합성 검증 배치 작업 스케줄링
4. **점진적 확장**: 다른 집계 쿼리에도 동일 패턴 적용

## 결론

머터리얼라이즈 뷰를 통한 서브쿼리 최적화는 **87%의 성능 향상**을 달성하며, 특히 대용량 데이터 환경에서 사용자 경험을 크게 개선할 수 있습니다. 적절한 데이터 정합성 관리 방안과 함께 도입하면 시스템의 전반적인 성능과 확장성을 크게 향상시킬 수 있습니다.