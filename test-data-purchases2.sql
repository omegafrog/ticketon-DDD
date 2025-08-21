-- Generate remaining purchases to reach 20,000 total
USE ticketon;

-- Generate remaining 6,000 purchases 
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
    CONCAT('Order ', n + 14000) as order_name,
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
    (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5) t3,
    (SELECT @row := 0) r
    LIMIT 6000
) numbers;

-- Check final purchase count
SELECT COUNT(*) as total_purchase_count FROM purchase;