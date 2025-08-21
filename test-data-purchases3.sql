-- Generate remaining purchases to reach exactly 20,000
USE ticketon;

-- Simple approach: generate 5,400 more purchases one by one
DELIMITER $$

CREATE PROCEDURE GenerateRemainingPurchases()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE current_count INT;
    
    SELECT COUNT(*) INTO current_count FROM purchase;
    
    WHILE current_count < 20000 DO
        INSERT INTO purchase (
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
        ) VALUES (
            UUID(),
            CONCAT('event_', LPAD(FLOOR(1 + RAND() * 100), 3, '0')),
            UUID(),
            CONCAT('Order ', current_count + 1),
            FLOOR(10000 + RAND() * 90000),
            '카드',
            'DONE',
            UUID(),
            CONCAT('user_', LPAD(FLOOR(1 + RAND() * 5000), 5, '0')),
            DATE_ADD(NOW(), INTERVAL -FLOOR(RAND() * 365) DAY)
        );
        
        SET current_count = current_count + 1;
        
        -- Progress indicator every 1000 records
        IF current_count % 1000 = 0 THEN
            SELECT CONCAT('Generated ', current_count, ' purchases') as progress;
        END IF;
    END WHILE;
    
    SELECT COUNT(*) as final_purchase_count FROM purchase;
END$$

DELIMITER ;

CALL GenerateRemainingPurchases();
DROP PROCEDURE GenerateRemainingPurchases;