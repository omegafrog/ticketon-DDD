-- Test Data Generation Script for Performance Testing
-- Target: 100 events, 20,000 purchases, tickets (1-4 per purchase)

USE ticketon;

-- 1. Generate 100 test events (id: event_001 to event_100)
INSERT IGNORE INTO event (
    id, 
    title, 
    description, 
    status, 
    booking_start, 
    booking_end, 
    event_start, 
    event_end,
    thumbnail_url,
    created_at,
    min_price,
    max_price,
    version
) 
SELECT 
    CONCAT('event_', LPAD(n, 3, '0')) as id,
    CONCAT('Test Event ', n) as title,
    CONCAT('Description for test event ', n) as description,
    'OPEN' as status,
    DATE_SUB(NOW(), INTERVAL 30 DAY) as booking_start,
    DATE_ADD(NOW(), INTERVAL 30 DAY) as booking_end,
    DATE_ADD(NOW(), INTERVAL 60 DAY) as event_start,
    DATE_ADD(NOW(), INTERVAL 61 DAY) as event_end,
    'https://example.com/thumbnail.jpg' as thumbnail_url,
    NOW() as created_at,
    10000 as min_price,
    50000 as max_price,
    1 as version
FROM (
    SELECT a.N + b.N * 10 + 1 as n
    FROM 
    (SELECT 0 as N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a
    CROSS JOIN 
    (SELECT 0 as N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b
    WHERE a.N + b.N * 10 + 1 <= 100
) numbers;

-- 2. Generate 20,000 purchases with random event assignment
INSERT IGNORE INTO purchase (
    purchase_id, 
    event_id, 
    order_id, 
    order_name,
    amount, 
    payment_method, 
    payment_status, 
    pid, 
    user_id, 
    created_at
)
SELECT 
    UUID() as purchase_id,
    CONCAT('event_', LPAD(FLOOR(1 + RAND() * 100), 3, '0')) as event_id,
    UUID() as order_id,
    CONCAT('Order ', n) as order_name,
    FLOOR(10000 + RAND() * 90000) as amount,
    '카드' as payment_method,
    'DONE' as payment_status,
    UUID() as pid,
    CONCAT('user_', LPAD(FLOOR(1 + RAND() * 5000), 5, '0')) as user_id,
    DATE_ADD(NOW(), INTERVAL -FLOOR(RAND() * 365) DAY) as created_at
FROM (
    SELECT @row := @row + 1 as n
    FROM 
    (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t1,
    (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t2,
    (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t3,
    (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t4,
    (SELECT @row := 0) r
    WHERE @row < 20000
) numbers;

-- Check purchase count
SELECT COUNT(*) as purchase_count FROM purchase;