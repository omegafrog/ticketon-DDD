# 복잡한 비즈니스 쿼리 및 슬로우 쿼리 분석

## 데이터베이스 구조 분석

### 테이블 현황
- **event**: 100,000개 이벤트 데이터
- **seat**: 62,887,562개 좌석 데이터 (매우 큰 볼륨)
- **purchase/ticket**: 현재 데이터 없음
- **주요 인덱스**: event_category_id, seat_layout_id, layout_id

### 성능 이슈 요인
- `seat` 테이블의 대용량 데이터 (62,887,562 건)
- 복잡한 다중 테이블 JOIN
- GROUP BY와 집계 함수의 조합
- 서브쿼리와 윈도우 함수 사용
- 인덱스가 없는 컬럼에서의 필터링 및 정렬

---

## 1. 인기 이벤트 실시간 좌석 현황 조회
**시나리오**: 메인 페이지에서 인기 이벤트의 실시간 좌석 현황을 보여주는 경우

```sql
SELECT 
    e.id, e.title, e.event_start, e.min_price, e.max_price,
    sl.hall_name, sl.location,
    COUNT(s.seat_id) as total_seats,
    SUM(CASE WHEN s.available = 1 THEN 1 ELSE 0 END) as available_seats,
    (COUNT(s.seat_id) - SUM(CASE WHEN s.available = 1 THEN 1 ELSE 0 END)) as sold_seats,
    ROUND((COUNT(s.seat_id) - SUM(CASE WHEN s.available = 1 THEN 1 ELSE 0 END)) / COUNT(s.seat_id) * 100, 2) as occupancy_rate
FROM event e
JOIN seat_layout sl ON e.seat_layout_id = sl.id
JOIN seat s ON s.layout_id = sl.id
WHERE e.status = 'OPEN' 
    AND e.booking_start <= NOW() 
    AND e.booking_end >= NOW()
    AND e.view_count > 1000
GROUP BY e.id, e.title, e.event_start, e.min_price, e.max_price, sl.hall_name, sl.location
ORDER BY occupancy_rate DESC, e.view_count DESC
LIMIT 20;
```

### 해결방법


---

## 2. 사용자별 구매 이력 및 선호 분석
**시나리오**: 사용자 프로필에서 구매 이력과 선호 카테고리/지역 분석

```sql
SELECT 
    m.user_id, m.name, m.age, m.sex,
    COUNT(DISTINCT p.purchase_id) as total_purchases,
    SUM(p.amount) as total_spent,
    GROUP_CONCAT(DISTINCT ec.name) as preferred_categories,
    GROUP_CONCAT(DISTINCT sl.region_location) as visited_regions,
    AVG(DATEDIFF(e.event_start, p.created_at)) as avg_booking_days_ahead,
    COUNT(DISTINCT CASE WHEN e.event_start > NOW() THEN e.id END) as upcoming_events
FROM members m
JOIN purchase p ON m.user_id = p.user_id
JOIN ticket t ON p.purchase_id = t.purchase_purchase_id
JOIN event e ON t.event_id = e.id
JOIN event_category ec ON e.event_category_id = ec.id
JOIN seat_layout sl ON e.seat_layout_id = sl.id
WHERE p.payment_status = 'DONE'
    AND p.created_at >= DATE_SUB(NOW(), INTERVAL 2 YEAR)
GROUP BY m.user_id, m.name, m.age, m.sex
HAVING total_purchases >= 3
ORDER BY total_spent DESC, total_purchases DESC;
```

---

## 3. 복잡한 좌석 검색 및 가격대별 분석 (수정됨)
**시나리오**: 특정 이벤트의 좌석을 등급별, 가격대별로 필터링하여 검색

```sql
SELECT 
    s.grade,
    s.amount,
    COUNT(*) as seat_count,
    SUM(CASE WHEN s.available = 1 THEN 1 ELSE 0 END) as available_count,
    MIN(s.amount) as min_price,
    MAX(s.amount) as max_price,
    AVG(s.amount) as avg_price,
    CASE 
        WHEN s.amount < e.min_price + (e.max_price - e.min_price) * 0.3 THEN '저가'
        WHEN s.amount < e.min_price + (e.max_price - e.min_price) * 0.7 THEN '중가'
        ELSE '고가'
    END as price_tier
FROM seat s
JOIN seat_layout sl ON s.layout_id = sl.id
JOIN event e ON e.seat_layout_id = sl.id
WHERE e.id = 'specific-event-id'
    AND s.available = 1
    AND sl.region_location IN ('SEOUL', 'GYEONGGI', 'INCHEON')
    AND s.amount BETWEEN @min_price AND @max_price  -- 가격 범위 필터링 추가
GROUP BY s.grade, s.amount, 
    CASE 
        WHEN s.amount < e.min_price + (e.max_price - e.min_price) * 0.3 THEN '저가'
        WHEN s.amount < e.min_price + (e.max_price - e.min_price) * 0.7 THEN '중가'
        ELSE '고가'
    END
ORDER BY price_tier, s.amount, s.grade;
```

**사용 예시**:
```sql
SET @min_price = 50000;  -- 최소 가격 설정
SET @max_price = 150000; -- 최대 가격 설정
-- 위 쿼리 실행
```

---

## 4. 이벤트 카테고리별 매출 트렌드 분석
**시나리오**: 관리자 대시보드에서 카테고리별 월간 매출 트렌드 분석

```sql
SELECT 
    ec.name as category_name,
    DATE_FORMAT(p.created_at, '%Y-%m') as sales_month,
    COUNT(DISTINCT p.purchase_id) as total_orders,
    COUNT(DISTINCT t.ticket_id) as total_tickets,
    SUM(p.amount) as monthly_revenue,
    AVG(p.amount) as avg_order_value,
    COUNT(DISTINCT p.user_id) as unique_customers,
    COUNT(DISTINCT e.id) as events_sold,
    SUM(p.amount) / COUNT(DISTINCT e.id) as revenue_per_event,
    LAG(SUM(p.amount)) OVER (PARTITION BY ec.id ORDER BY DATE_FORMAT(p.created_at, '%Y-%m')) as prev_month_revenue,
    ROUND(((SUM(p.amount) - LAG(SUM(p.amount)) OVER (PARTITION BY ec.id ORDER BY DATE_FORMAT(p.created_at, '%Y-%m'))) 
           / LAG(SUM(p.amount)) OVER (PARTITION BY ec.id ORDER BY DATE_FORMAT(p.created_at, '%Y-%m')) * 100), 2) as growth_rate
FROM event_category ec
JOIN event e ON ec.id = e.event_category_id
JOIN ticket t ON e.id = t.event_id
JOIN purchase p ON t.purchase_purchase_id = p.purchase_id
WHERE p.payment_status = 'DONE'
    AND p.created_at >= DATE_SUB(NOW(), INTERVAL 12 MONTH)
GROUP BY ec.id, ec.name, DATE_FORMAT(p.created_at, '%Y-%m')
ORDER BY sales_month DESC, monthly_revenue DESC;
```

---

## 5. 좌석 배치별 판매 성과 분석
**시나리오**: 공연장 관리자가 좌석 배치 효율성을 분석하여 향후 가격 전략 수립

```sql
SELECT 
    sl.id as layout_id,
    sl.hall_name,
    sl.location,
    sl.region_location,
    COUNT(DISTINCT s.seat_id) as total_seats,
    COUNT(DISTINCT CASE WHEN s.available = 0 THEN s.seat_id END) as sold_seats,
    COUNT(DISTINCT e.id) as events_held,
    AVG(DATEDIFF(e.booking_end, e.booking_start)) as avg_booking_period,
    SUM(CASE WHEN s.available = 0 THEN s.amount ELSE 0 END) as total_revenue,
    AVG(CASE WHEN s.available = 0 THEN s.amount END) as avg_ticket_price,
    COUNT(DISTINCT s.grade) as grade_variety,
    MAX(s.amount) - MIN(s.amount) as price_range,
    ROUND(COUNT(DISTINCT CASE WHEN s.available = 0 THEN s.seat_id END) / COUNT(DISTINCT s.seat_id) * 100, 2) as occupancy_rate,
    ROW_NUMBER() OVER (ORDER BY SUM(CASE WHEN s.available = 0 THEN s.amount ELSE 0 END) DESC) as revenue_rank
FROM seat_layout sl
JOIN seat s ON sl.id = s.layout_id
JOIN event e ON sl.id = e.seat_layout_id
WHERE e.event_end < NOW()
    AND e.status != 'CANCELLED'
GROUP BY sl.id, sl.hall_name, sl.location, sl.region_location
HAVING events_held >= 5
ORDER BY total_revenue DESC, occupancy_rate DESC;
```

---

## 6. 사용자 구매 패턴 및 이탈 위험도 분석
**시나리오**: CRM에서 사용자 세그멘테이션 및 이탈 위험 고객 식별

```sql
SELECT 
    m.user_id,
    m.name,
    m.age,
    TIMESTAMPDIFF(DAY, MAX(p.created_at), NOW()) as days_since_last_purchase,
    COUNT(DISTINCT p.purchase_id) as total_purchases,
    SUM(p.amount) as lifetime_value,
    AVG(p.amount) as avg_order_value,
    COUNT(DISTINCT DATE_FORMAT(p.created_at, '%Y-%m')) as active_months,
    COUNT(DISTINCT ec.id) as category_diversity,
    MIN(p.created_at) as first_purchase_date,
    MAX(p.created_at) as last_purchase_date,
    TIMESTAMPDIFF(MONTH, MIN(p.created_at), MAX(p.created_at)) as customer_tenure_months,
    CASE 
        WHEN TIMESTAMPDIFF(DAY, MAX(p.created_at), NOW()) <= 30 THEN 'Active'
        WHEN TIMESTAMPDIFF(DAY, MAX(p.created_at), NOW()) <= 90 THEN 'At Risk'
        WHEN TIMESTAMPDIFF(DAY, MAX(p.created_at), NOW()) <= 180 THEN 'Dormant'
        ELSE 'Lost'
    END as customer_status,
    CASE 
        WHEN COUNT(DISTINCT p.purchase_id) >= 10 AND SUM(p.amount) >= 500000 THEN 'VIP'
        WHEN COUNT(DISTINCT p.purchase_id) >= 5 AND SUM(p.amount) >= 200000 THEN 'Premium'
        WHEN COUNT(DISTINCT p.purchase_id) >= 2 THEN 'Regular'
        ELSE 'New'
    END as customer_tier
FROM members m
LEFT JOIN purchase p ON m.user_id = p.user_id AND p.payment_status = 'DONE'
LEFT JOIN ticket t ON p.purchase_id = t.purchase_purchase_id
LEFT JOIN event e ON t.event_id = e.id
LEFT JOIN event_category ec ON e.event_category_id = ec.id
GROUP BY m.user_id, m.name, m.age
ORDER BY lifetime_value DESC, days_since_last_purchase ASC;
```

---

## 7. 복잡한 이벤트 추천 알고리즘
**시나리오**: 사용자 구매 이력과 선호도를 기반으로 한 개인화 이벤트 추천

```sql
WITH user_preferences AS (
    SELECT 
        p.user_id,
        ec.id as preferred_category_id,
        sl.region_location as preferred_region,
        COUNT(*) as category_count,
        AVG(p.amount) as avg_spending,
        ROW_NUMBER() OVER (PARTITION BY p.user_id ORDER BY COUNT(*) DESC) as category_rank
    FROM purchase p
    JOIN ticket t ON p.purchase_id = t.purchase_purchase_id
    JOIN event e ON t.event_id = e.id
    JOIN event_category ec ON e.event_category_id = ec.id
    JOIN seat_layout sl ON e.seat_layout_id = sl.id
    WHERE p.payment_status = 'DONE'
        AND p.created_at >= DATE_SUB(NOW(), INTERVAL 1 YEAR)
    GROUP BY p.user_id, ec.id, sl.region_location
),
similar_users AS (
    SELECT 
        up1.user_id,
        up2.user_id as similar_user_id,
        COUNT(*) as common_preferences,
        ABS(up1.avg_spending - up2.avg_spending) as spending_similarity
    FROM user_preferences up1
    JOIN user_preferences up2 ON up1.preferred_category_id = up2.preferred_category_id
        AND up1.user_id != up2.user_id
        AND up1.category_rank <= 3
        AND up2.category_rank <= 3
    GROUP BY up1.user_id, up2.user_id
    HAVING common_preferences >= 2
)
SELECT DISTINCT
    e.id,
    e.title,
    e.event_start,
    e.min_price,
    e.max_price,
    ec.name as category_name,
    sl.region_location,
    COUNT(DISTINCT su.similar_user_id) as recommendation_score,
    AVG(up.avg_spending) as target_price_point,
    COUNT(DISTINCT CASE WHEN s.available = 1 THEN s.seat_id END) as available_seats
FROM event e
JOIN event_category ec ON e.event_category_id = ec.id
JOIN seat_layout sl ON e.seat_layout_id = sl.id
JOIN seat s ON sl.id = s.layout_id
JOIN user_preferences up ON ec.id = up.preferred_category_id
JOIN similar_users su ON up.user_id = su.user_id
WHERE e.status = 'OPEN'
    AND e.booking_start <= NOW()
    AND e.booking_end >= NOW()
    AND e.event_start > NOW()
    AND up.user_id = 'target-user-id'
    AND up.category_rank <= 3
GROUP BY e.id, e.title, e.event_start, e.min_price, e.max_price, ec.name, sl.region_location
HAVING available_seats > 0
ORDER BY recommendation_score DESC, ABS(e.min_price - target_price_point) ASC
LIMIT 10;
```

---

## 8. 실시간 대기열 및 좌석 경합 분석
**시나리오**: 인기 이벤트의 실시간 좌석 경합 상황과 대기열 상태 모니터링

```sql
SELECT 
    e.id as event_id,
    e.title,
    e.booking_start,
    e.booking_end,
    sl.hall_name,
    COUNT(DISTINCT s.seat_id) as total_seats,
    SUM(CASE WHEN s.available = 1 THEN 1 ELSE 0 END) as available_seats,
    COUNT(DISTINCT p.purchase_id) as completed_purchases,
    COUNT(DISTINCT CASE WHEN p.payment_status = 'IN_PROGRESS' THEN p.purchase_id END) as pending_purchases,
    SUM(CASE WHEN s.available = 1 AND s.grade = 'VIP' THEN 1 ELSE 0 END) as vip_available,
    SUM(CASE WHEN s.available = 1 AND s.grade = 'R석' THEN 1 ELSE 0 END) as r_available,
    SUM(CASE WHEN s.available = 1 AND s.grade = 'S석' THEN 1 ELSE 0 END) as s_available,
    ROUND((COUNT(DISTINCT s.seat_id) - SUM(CASE WHEN s.available = 1 THEN 1 ELSE 0 END)) / COUNT(DISTINCT s.seat_id) * 100, 2) as booking_rate,
    CASE 
        WHEN SUM(CASE WHEN s.available = 1 THEN 1 ELSE 0 END) / COUNT(DISTINCT s.seat_id) > 0.5 THEN 'LOW'
        WHEN SUM(CASE WHEN s.available = 1 THEN 1 ELSE 0 END) / COUNT(DISTINCT s.seat_id) > 0.1 THEN 'MEDIUM'
        ELSE 'HIGH'
    END as competition_level,
    TIMESTAMPDIFF(MINUTE, NOW(), e.booking_end) as minutes_left,
    COUNT(DISTINCT p.user_id) as unique_buyers
FROM event e
JOIN seat_layout sl ON e.seat_layout_id = sl.id
JOIN seat s ON sl.id = s.layout_id
LEFT JOIN ticket t ON e.id = t.event_id
LEFT JOIN purchase p ON t.purchase_purchase_id = p.purchase_id
WHERE e.status = 'OPEN'
    AND e.booking_start <= NOW()
    AND e.booking_end >= NOW()
    AND e.event_start > NOW()
GROUP BY e.id, e.title, e.booking_start, e.booking_end, sl.hall_name
HAVING booking_rate >= 50
ORDER BY competition_level DESC, booking_rate DESC, minutes_left ASC;
```

---

## 9. 수익성 및 ROI 분석을 위한 복합 쿼리
**시나리오**: 이벤트별 수익성 분석과 마케팅 ROI 계산을 위한 상세 데이터

```sql
SELECT 
    e.id,
    e.title,
    ec.name as category,
    sl.region_location,
    DATE_FORMAT(e.event_start, '%Y-%m') as event_month,
    COUNT(DISTINCT s.seat_id) as total_capacity,
    COUNT(DISTINCT CASE WHEN s.available = 0 THEN s.seat_id END) as sold_seats,
    SUM(CASE WHEN s.available = 0 THEN s.amount ELSE 0 END) as gross_revenue,
    COUNT(DISTINCT p.purchase_id) as total_orders,
    AVG(CASE WHEN p.payment_status = 'DONE' THEN p.amount END) as avg_order_value,
    COUNT(DISTINCT p.user_id) as unique_customers,
    COUNT(DISTINCT CASE WHEN up.first_purchase_event = e.id THEN p.user_id END) as new_customers,
    ROUND(COUNT(DISTINCT CASE WHEN s.available = 0 THEN s.seat_id END) / COUNT(DISTINCT s.seat_id) * 100, 2) as occupancy_rate,
    ROUND(SUM(CASE WHEN s.available = 0 THEN s.amount ELSE 0 END) / COUNT(DISTINCT s.seat_id), 2) as revenue_per_seat,
    e.view_count,
    ROUND(COUNT(DISTINCT p.purchase_id) / e.view_count * 100, 4) as conversion_rate,
    TIMESTAMPDIFF(DAY, e.booking_start, e.event_start) as booking_window_days,
    AVG(TIMESTAMPDIFF(DAY, p.created_at, e.event_start)) as avg_advance_booking_days,
    CASE 
        WHEN ROUND(COUNT(DISTINCT CASE WHEN s.available = 0 THEN s.seat_id END) / COUNT(DISTINCT s.seat_id) * 100, 2) >= 95 THEN 'Sold Out'
        WHEN ROUND(COUNT(DISTINCT CASE WHEN s.available = 0 THEN s.seat_id END) / COUNT(DISTINCT s.seat_id) * 100, 2) >= 80 THEN 'High Demand'
        WHEN ROUND(COUNT(DISTINCT CASE WHEN s.available = 0 THEN s.seat_id END) / COUNT(DISTINCT s.seat_id) * 100, 2) >= 50 THEN 'Moderate'
        ELSE 'Low Demand'
    END as demand_level
FROM event e
JOIN event_category ec ON e.event_category_id = ec.id
JOIN seat_layout sl ON e.seat_layout_id = sl.id
JOIN seat s ON sl.id = s.layout_id
LEFT JOIN ticket t ON e.id = t.event_id
LEFT JOIN purchase p ON t.purchase_purchase_id = p.purchase_id AND p.payment_status = 'DONE'
LEFT JOIN (
    SELECT user_id, MIN(event_id) as first_purchase_event
    FROM ticket t2
    JOIN purchase p2 ON t2.purchase_purchase_id = p2.purchase_id
    WHERE p2.payment_status = 'DONE'
    GROUP BY user_id
) up ON p.user_id = up.user_id
WHERE e.event_end <= NOW()
    AND e.status != 'CANCELLED'
GROUP BY e.id, e.title, ec.name, sl.region_location, DATE_FORMAT(e.event_start, '%Y-%m'), e.view_count, e.booking_start, e.event_start
ORDER BY gross_revenue DESC, occupancy_rate DESC;
```

---

## 10. 사용자 행동 패턴 및 이탈 예측 분석
**시나리오**: 머신러닝 모델을 위한 사용자 행동 패턴 데이터 추출 및 이탈 예측

```sql
WITH user_behavior_metrics AS (
    SELECT 
        m.user_id,
        m.age,
        m.sex,
        su.created_at as registration_date,
        su.last_login_at,
        TIMESTAMPDIFF(DAY, su.created_at, NOW()) as account_age_days,
        TIMESTAMPDIFF(DAY, su.last_login_at, NOW()) as days_since_last_login,
        COUNT(DISTINCT p.purchase_id) as total_purchases,
        COALESCE(SUM(p.amount), 0) as total_spent,
        COUNT(DISTINCT DATE_FORMAT(p.created_at, '%Y-%m')) as active_months,
        COUNT(DISTINCT ec.id) as category_diversity,
        COUNT(DISTINCT sl.region_location) as region_diversity,
        AVG(p.amount) as avg_order_value,
        STDDEV(p.amount) as spending_volatility,
        MIN(p.created_at) as first_purchase_date,
        MAX(p.created_at) as last_purchase_date,
        AVG(TIMESTAMPDIFF(DAY, p.created_at, e.event_start)) as avg_advance_booking_days,
        COUNT(DISTINCT CASE WHEN p.payment_status = 'CANCELED' THEN p.purchase_id END) as canceled_orders,
        COUNT(DISTINCT CASE WHEN p.payment_status = 'REFUNDED' THEN p.purchase_id END) as refunded_orders
    FROM members m
    LEFT JOIN security_user su ON m.security_user_id = su.security_user_id
    LEFT JOIN purchase p ON m.user_id = p.user_id
    LEFT JOIN ticket t ON p.purchase_id = t.purchase_purchase_id
    LEFT JOIN event e ON t.event_id = e.id
    LEFT JOIN event_category ec ON e.event_category_id = ec.id
    LEFT JOIN seat_layout sl ON e.seat_layout_id = sl.id
    WHERE su.created_at >= DATE_SUB(NOW(), INTERVAL 2 YEAR)
    GROUP BY m.user_id, m.age, m.sex, su.created_at, su.last_login_at
),
purchase_frequency AS (
    SELECT 
        user_id,
        CASE 
            WHEN total_purchases = 0 THEN 'Never Purchased'
            WHEN total_purchases / (account_age_days / 30.44) >= 1 THEN 'High Frequency'
            WHEN total_purchases / (account_age_days / 30.44) >= 0.3 THEN 'Medium Frequency'
            ELSE 'Low Frequency'
        END as purchase_frequency_segment,
        CASE 
            WHEN days_since_last_login <= 7 THEN 'Very Active'
            WHEN days_since_last_login <= 30 THEN 'Active' 
            WHEN days_since_last_login <= 90 THEN 'Moderate'
            WHEN days_since_last_login <= 180 THEN 'Inactive'
            ELSE 'Dormant'
        END as activity_level,
        CASE 
            WHEN total_purchases = 0 AND account_age_days > 30 THEN 'High Risk'
            WHEN total_purchases > 0 AND TIMESTAMPDIFF(DAY, last_purchase_date, NOW()) > 120 THEN 'Medium Risk'
            WHEN days_since_last_login > 90 THEN 'Medium Risk'
            ELSE 'Low Risk'
        END as churn_risk
    FROM user_behavior_metrics
)
SELECT 
    ubm.*,
    pf.purchase_frequency_segment,
    pf.activity_level,
    pf.churn_risk,
    CASE 
        WHEN total_spent >= 1000000 THEN 'VIP'
        WHEN total_spent >= 500000 THEN 'Premium'
        WHEN total_spent >= 100000 THEN 'Gold'
        WHEN total_spent > 0 THEN 'Silver'
        ELSE 'Bronze'
    END as customer_tier,
    ROUND(COALESCE(canceled_orders + refunded_orders, 0) / NULLIF(total_purchases, 0) * 100, 2) as cancellation_rate,
    RANK() OVER (ORDER BY total_spent DESC) as spending_rank,
    NTILE(5) OVER (ORDER BY total_spent DESC) as spending_quintile
FROM user_behavior_metrics ubm
JOIN purchase_frequency pf ON ubm.user_id = pf.user_id
ORDER BY spending_rank ASC, account_age_days DESC;
```

---

## 성능 최적화 권장사항

### 인덱스 추가 권장
1. `seat` 테이블: `amount`, `available`, `grade` 컬럼 복합 인덱스
2. `event` 테이블: `status`, `booking_start`, `booking_end`, `view_count` 복합 인덱스
3. `purchase` 테이블: `payment_status`, `created_at`, `user_id` 복합 인덱스
4. `ticket` 테이블: `event_id`, `purchase_purchase_id` 복합 인덱스

### 쿼리 최적화 방안
1. 파티셔닝: `seat` 테이블을 `layout_id` 기준으로 파티셔닝
2. 캐싱: 자주 조회되는 집계 데이터 Redis 캐싱
3. 읽기 전용 복제본: 분석 쿼리용 별도 읽기 전용 DB 사용
4. 배치 처리: 실시간성이 중요하지 않은 통계는 배치로 사전 계산

### 모니터링 포인트
- `seat` 테이블 조인이 포함된 쿼리의 실행 시간
- 메모리 사용량 (큰 결과셋으로 인한 메모리 부족)
- 임시 테이블 사용 여부
- 풀 테이블 스캔 발생 빈도