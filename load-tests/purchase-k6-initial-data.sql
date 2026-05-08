-- Initial DB data for purchase k6 tests.
-- This is intentionally deterministic and re-runnable.
--
-- Expected MySQL database: ticketon
-- Default test event id: event-k6-001
-- Default amount used by seed-purchase-k6-data.sh: 50000

SET FOREIGN_KEY_CHECKS = 0;

INSERT INTO seat_layout (
  id,
  layout,
  location,
  hall_name,
  region_location
) VALUES (
  9001,
  '[["A1","A2","A3"],["B1","B2","B3"]]',
  'K6 Test Location',
  'K6 Test Hall',
  'SEOUL'
)
ON DUPLICATE KEY UPDATE
  layout = VALUES(layout),
  location = VALUES(location),
  hall_name = VALUES(hall_name),
  region_location = VALUES(region_location);

INSERT INTO seat (
  seat_id,
  signature,
  grade,
  amount,
  available,
  layout_id
) VALUES
  ('k6-seat-9001-A1', 'A1', 'K6', 50000, TRUE, 9001),
  ('k6-seat-9001-A2', 'A2', 'K6', 50000, TRUE, 9001),
  ('k6-seat-9001-A3', 'A3', 'K6', 50000, TRUE, 9001),
  ('k6-seat-9001-B1', 'B1', 'K6', 50000, TRUE, 9001),
  ('k6-seat-9001-B2', 'B2', 'K6', 50000, TRUE, 9001),
  ('k6-seat-9001-B3', 'B3', 'K6', 50000, TRUE, 9001)
ON DUPLICATE KEY UPDATE
  signature = VALUES(signature),
  grade = VALUES(grade),
  amount = VALUES(amount),
  available = VALUES(available),
  layout_id = VALUES(layout_id);

INSERT INTO event (
  id,
  title,
  thumbnail_url,
  age_limit,
  restrictions,
  description,
  booking_start,
  booking_end,
  event_start,
  event_end,
  view_count,
  seat_selectable,
  min_price,
  max_price,
  status,
  event_category_id,
  manager_id,
  seat_layout_id,
  deleted,
  created_at,
  modified_at,
  version,
  sales_version
) VALUES (
  'event-k6-001',
  'K6 Purchase Test Event',
  'https://example.com/k6-test-thumbnail.jpg',
  0,
  NULL,
  'K6 purchase performance test event',
  TIMESTAMP '2026-01-01 00:00:00',
  TIMESTAMP '2026-12-31 23:59:59',
  TIMESTAMP '2027-01-01 19:00:00',
  TIMESTAMP '2027-01-01 21:00:00',
  0,
  TRUE,
  50000,
  50000,
  'OPEN',
  1,
  'k6-manager-001',
  9001,
  FALSE,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP,
  0,
  0
)
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  thumbnail_url = VALUES(thumbnail_url),
  age_limit = VALUES(age_limit),
  restrictions = VALUES(restrictions),
  description = VALUES(description),
  booking_start = VALUES(booking_start),
  booking_end = VALUES(booking_end),
  event_start = VALUES(event_start),
  event_end = VALUES(event_end),
  view_count = VALUES(view_count),
  seat_selectable = VALUES(seat_selectable),
  min_price = VALUES(min_price),
  max_price = VALUES(max_price),
  status = VALUES(status),
  event_category_id = VALUES(event_category_id),
  manager_id = VALUES(manager_id),
  seat_layout_id = VALUES(seat_layout_id),
  deleted = VALUES(deleted),
  modified_at = CURRENT_TIMESTAMP,
  version = VALUES(version),
  sales_version = VALUES(sales_version);

SET FOREIGN_KEY_CHECKS = 1;
