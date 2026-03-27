CREATE TABLE IF NOT EXISTS seat_layout_stats (
    layout_id BIGINT PRIMARY KEY,
    seat_count INT NOT NULL DEFAULT 0,
    min_price INT NOT NULL DEFAULT 0,
    max_price INT NOT NULL DEFAULT 0,
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_layout_seat_count (layout_id, seat_count)
) ENGINE=InnoDB;

INSERT INTO seat_layout_stats (layout_id, seat_count, min_price, max_price, last_updated)
SELECT
    s.layout_id,
    COUNT(*) AS seat_count,
    COALESCE(MIN(s.amount), 0) AS min_price,
    COALESCE(MAX(s.amount), 0) AS max_price,
    CURRENT_TIMESTAMP
FROM seat s
GROUP BY s.layout_id
ON DUPLICATE KEY UPDATE
    seat_count = VALUES(seat_count),
    min_price = VALUES(min_price),
    max_price = VALUES(max_price),
    last_updated = CURRENT_TIMESTAMP;

DROP TRIGGER IF EXISTS seat_ai_stats;
DROP TRIGGER IF EXISTS seat_ad_stats;
DROP TRIGGER IF EXISTS seat_au_stats;

DELIMITER $$

CREATE TRIGGER seat_ai_stats
AFTER INSERT ON seat
FOR EACH ROW
BEGIN
    INSERT INTO seat_layout_stats (layout_id, seat_count, min_price, max_price, last_updated)
    VALUES (NEW.layout_id, 1, NEW.amount, NEW.amount, CURRENT_TIMESTAMP)
    ON DUPLICATE KEY UPDATE
        seat_count = seat_count + 1,
        min_price = IF(seat_count = 0, NEW.amount, LEAST(min_price, NEW.amount)),
        max_price = GREATEST(max_price, NEW.amount),
        last_updated = CURRENT_TIMESTAMP;
END$$

CREATE TRIGGER seat_ad_stats
AFTER DELETE ON seat
FOR EACH ROW
BEGIN
    INSERT INTO seat_layout_stats (layout_id, seat_count, min_price, max_price, last_updated)
    SELECT
        OLD.layout_id,
        COUNT(*),
        COALESCE(MIN(s.amount), 0),
        COALESCE(MAX(s.amount), 0),
        CURRENT_TIMESTAMP
    FROM seat s
    WHERE s.layout_id = OLD.layout_id
    ON DUPLICATE KEY UPDATE
        seat_count = VALUES(seat_count),
        min_price = VALUES(min_price),
        max_price = VALUES(max_price),
        last_updated = CURRENT_TIMESTAMP;
END$$

CREATE TRIGGER seat_au_stats
AFTER UPDATE ON seat
FOR EACH ROW
BEGIN
    INSERT INTO seat_layout_stats (layout_id, seat_count, min_price, max_price, last_updated)
    SELECT
        NEW.layout_id,
        COUNT(*),
        COALESCE(MIN(s.amount), 0),
        COALESCE(MAX(s.amount), 0),
        CURRENT_TIMESTAMP
    FROM seat s
    WHERE s.layout_id = NEW.layout_id
    ON DUPLICATE KEY UPDATE
        seat_count = VALUES(seat_count),
        min_price = VALUES(min_price),
        max_price = VALUES(max_price),
        last_updated = CURRENT_TIMESTAMP;

    IF OLD.layout_id <> NEW.layout_id THEN
        INSERT INTO seat_layout_stats (layout_id, seat_count, min_price, max_price, last_updated)
        SELECT
            OLD.layout_id,
            COUNT(*),
            COALESCE(MIN(s.amount), 0),
            COALESCE(MAX(s.amount), 0),
            CURRENT_TIMESTAMP
        FROM seat s
        WHERE s.layout_id = OLD.layout_id
        ON DUPLICATE KEY UPDATE
            seat_count = VALUES(seat_count),
            min_price = VALUES(min_price),
            max_price = VALUES(max_price),
            last_updated = CURRENT_TIMESTAMP;
    END IF;
END$$

DELIMITER ;
