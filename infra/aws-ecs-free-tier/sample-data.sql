INSERT INTO event_category (id, name, thumbnail_url)
VALUES
  (1, 'Concert', 'https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?auto=format&fit=crop&w=1200&q=80'),
  (2, 'Musical', 'https://images.unsplash.com/photo-1503095396549-807759245b35?auto=format&fit=crop&w=1200&q=80'),
  (3, 'Sports', 'https://images.unsplash.com/photo-1461896836934-ffe607ba8211?auto=format&fit=crop&w=1200&q=80')
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  thumbnail_url = VALUES(thumbnail_url);

INSERT INTO seat_layout (id, layout, location, hall_name, region_location)
VALUES
  (1001, '[
["A1", "A2", "A3", "A4"],
["B1", "B2", "B3", "B4"],
["C1", "C2", "C3", "C4"],
]', 'Seoul Arts Center', 'Main Hall', 'SEOUL'),
  (1002, '[
["VIP1", "VIP2", "VIP3"],
["R1", "R2", "R3"],
["S1", "S2", "S3"],
]', 'Busan Dream Arena', 'Blue Stage', 'BUSAN')
ON DUPLICATE KEY UPDATE
  layout = VALUES(layout),
  location = VALUES(location),
  hall_name = VALUES(hall_name),
  region_location = VALUES(region_location);

INSERT INTO seat (seat_id, signature, grade, amount, available, layout_id)
VALUES
  ('sample-seat-1001-a1', 'A1', 'VIP', 120000, true, 1001),
  ('sample-seat-1001-a2', 'A2', 'VIP', 120000, true, 1001),
  ('sample-seat-1001-a3', 'A3', 'VIP', 120000, true, 1001),
  ('sample-seat-1001-a4', 'A4', 'VIP', 120000, true, 1001),
  ('sample-seat-1001-b1', 'B1', 'R', 90000, true, 1001),
  ('sample-seat-1001-b2', 'B2', 'R', 90000, true, 1001),
  ('sample-seat-1001-b3', 'B3', 'R', 90000, true, 1001),
  ('sample-seat-1001-b4', 'B4', 'R', 90000, true, 1001),
  ('sample-seat-1001-c1', 'C1', 'S', 70000, true, 1001),
  ('sample-seat-1001-c2', 'C2', 'S', 70000, true, 1001),
  ('sample-seat-1001-c3', 'C3', 'S', 70000, true, 1001),
  ('sample-seat-1001-c4', 'C4', 'S', 70000, true, 1001),
  ('sample-seat-1002-vip1', 'VIP1', 'VIP', 150000, true, 1002),
  ('sample-seat-1002-vip2', 'VIP2', 'VIP', 150000, true, 1002),
  ('sample-seat-1002-vip3', 'VIP3', 'VIP', 150000, true, 1002),
  ('sample-seat-1002-r1', 'R1', 'R', 110000, true, 1002),
  ('sample-seat-1002-r2', 'R2', 'R', 110000, true, 1002),
  ('sample-seat-1002-r3', 'R3', 'R', 110000, true, 1002),
  ('sample-seat-1002-s1', 'S1', 'S', 80000, true, 1002),
  ('sample-seat-1002-s2', 'S2', 'S', 80000, true, 1002),
  ('sample-seat-1002-s3', 'S3', 'S', 80000, true, 1002)
ON DUPLICATE KEY UPDATE
  signature = VALUES(signature),
  grade = VALUES(grade),
  amount = VALUES(amount),
  available = VALUES(available),
  layout_id = VALUES(layout_id);

INSERT INTO seat_layout_stats (layout_id, seat_count, min_price, max_price, last_updated)
VALUES
  (1001, 12, 70000, 120000, NOW()),
  (1002, 9, 80000, 150000, NOW())
ON DUPLICATE KEY UPDATE
  seat_count = VALUES(seat_count),
  min_price = VALUES(min_price),
  max_price = VALUES(max_price),
  last_updated = VALUES(last_updated);

INSERT INTO `event` (
  id, title, thumbnail_url, age_limit, restrictions, description,
  booking_start, booking_end, event_start, event_end, view_count,
  seat_selectable, min_price, max_price, status, event_category_id,
  manager_id, seat_layout_id, deleted, created_at, modified_at, version, sales_version
)
VALUES
  (
    'sample-event-spring-rock-2026',
    'Spring Rock Festival 2026',
    'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?auto=format&fit=crop&w=1200&q=80',
    12,
    'No outside food. Doors open 60 minutes before showtime.',
    'Live band festival with reserved seats and a full evening lineup.',
    '2026-01-01 09:00:00',
    '2026-08-15 18:00:00',
    '2026-08-20 19:30:00',
    '2026-08-20 22:00:00',
    128,
    true,
    70000,
    120000,
    'OPEN',
    1,
    'sample-manager',
    1001,
    false,
    NOW(),
    NOW(),
    0,
    0
  ),
  (
    'sample-event-ocean-musical-2026',
    'Ocean Light Musical',
    'https://images.unsplash.com/photo-1503095396549-807759245b35?auto=format&fit=crop&w=1200&q=80',
    7,
    'Children under 7 not admitted.',
    'A stage musical for demo browsing and seat selection.',
    '2026-01-01 09:00:00',
    '2026-09-05 18:00:00',
    '2026-09-10 20:00:00',
    '2026-09-10 22:20:00',
    96,
    true,
    80000,
    150000,
    'OPEN',
    2,
    'sample-manager',
    1002,
    false,
    NOW(),
    NOW(),
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
  modified_at = VALUES(modified_at),
  sales_version = VALUES(sales_version);
