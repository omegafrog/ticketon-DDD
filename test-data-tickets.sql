-- Generate tickets for each purchase (1-4 tickets per purchase)
USE ticketon;

DELIMITER $$

CREATE PROCEDURE GenerateTickets()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE p_id VARCHAR(255);
    DECLARE p_event_id VARCHAR(255);
    DECLARE ticket_count INT;
    DECLARE i INT;
    
    DECLARE purchase_cursor CURSOR FOR 
        SELECT purchase_id, event_id FROM purchase;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN purchase_cursor;
    
    purchase_loop: LOOP
        FETCH purchase_cursor INTO p_id, p_event_id;
        
        IF done THEN
            LEAVE purchase_loop;
        END IF;
        
        -- Random number of tickets per purchase (1-4)
        SET ticket_count = FLOOR(1 + RAND() * 4);
        SET i = 1;
        
        ticket_loop: WHILE i <= ticket_count DO
            INSERT INTO ticket (
                ticket_id,
                event_id,
                location,
                purchase_date,
                seat_id,
                purchase_purchase_id
            ) VALUES (
                UUID(),
                p_event_id,
                CONCAT('Section ', CHAR(65 + FLOOR(RAND() * 10)), '-', FLOOR(1 + RAND() * 20)),
                NOW(),
                CONCAT('seat_', UUID()),
                p_id
            );
            
            SET i = i + 1;
        END WHILE ticket_loop;
        
    END LOOP purchase_loop;
    
    CLOSE purchase_cursor;
    
    SELECT COUNT(*) as total_tickets FROM ticket;
    SELECT 
        COUNT(DISTINCT purchase_purchase_id) as purchases_with_tickets,
        MIN(ticket_count.cnt) as min_tickets_per_purchase,
        MAX(ticket_count.cnt) as max_tickets_per_purchase,
        AVG(ticket_count.cnt) as avg_tickets_per_purchase
    FROM (
        SELECT purchase_purchase_id, COUNT(*) as cnt
        FROM ticket 
        GROUP BY purchase_purchase_id
    ) as ticket_count;
    
END$$

DELIMITER ;

CALL GenerateTickets();
DROP PROCEDURE GenerateTickets;