-- Generate additional purchases to reach 20,000 total
USE ticketon;

-- Generate additional 10,000 purchases (we already have 10,000)
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
    CONCAT('Order ', @row := @row + 1) as order_name,
    FLOOR(10000 + RAND() * 90000) as amount,
    '카드' as payment_method,
    'DONE' as payment_status,
    UUID() as pid,
    CONCAT('user_', LPAD(FLOOR(1 + RAND() * 5000), 5, '0')) as user_id,
    DATE_ADD(NOW(), INTERVAL -FLOOR(RAND() * 365) DAY) as created_at
FROM 
    (SELECT @row := 10000) r,
    (SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 0) t1,
    (SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 0) t2,
    (SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 0) t3,
    (SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) t4
LIMIT 10000;

-- Check final purchase count
SELECT COUNT(*) as total_purchase_count FROM purchase;