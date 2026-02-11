-- Seed data for H2 (MODE=MySQL) with globally_quoted_identifiers=true.
-- Populates: seat_layout, seat, event

-- Re-runnable (delete in FK-safe order)
DELETE FROM "seat";
DELETE FROM "seat_layout_stats";
DELETE FROM "event";
DELETE FROM "seat_layout";

-- seat_layout
INSERT INTO "seat_layout" (
  "id",
  "layout",
  "location",
  "hall_name",
  "region_location"
) VALUES
  (
    1001,
    '[["A1","A2","A3"],["B1","B2","B3"]]',
    'Seoul Arts Center',
    'Main Hall',
    'SEOUL'
  ),
  (
    1002,
    '[["C1","C2"],["D1","D2"]]',
    'Busan Culture Center',
    'Ocean Hall',
    'BUSAN'
  );

-- seat
INSERT INTO "seat" (
  "seat_id",
  "signature",
  "grade",
  "amount",
  "available",
  "layout_id"
) VALUES
  ('seat-1001-A1', 'A1', 'VIP',  50000, TRUE, 1001),
  ('seat-1001-A2', 'A2', 'VIP',  70000, TRUE, 1001),
  ('seat-1001-A3', 'A3', 'R',   100000, TRUE, 1001),
  ('seat-1001-B1', 'B1', 'R',    80000, TRUE, 1001),
  ('seat-1001-B2', 'B2', 'S',    60000, TRUE, 1001),
  ('seat-1001-B3', 'B3', 'S',    90000, TRUE, 1001),
  ('seat-1002-C1', 'C1', 'VIP', 120000, TRUE, 1002),
  ('seat-1002-C2', 'C2', 'R',    40000, TRUE, 1002),
  ('seat-1002-D1', 'D1', 'S',    55000, TRUE, 1002),
  ('seat-1002-D2', 'D2', 'S',    65000, TRUE, 1002);

-- event
-- Table/column names are quoted because of globally_quoted_identifiers=true.
-- Column mapping comes from:
-- - EventId: id
-- - EventInformation: title, thumbnail_url, ageLimit, restrictions, description, booking_start/end, event_start/end,
--                    view_count, seat_selectable, min_price, max_price, status, event_category_id
-- - Embedded: manager_id, seat_layout_id
-- - MetaData: deleted, created_at, modified_at
-- - Versioning: version, sales_version
INSERT INTO "event" (
  "id",
  "title",
  "thumbnail_url",
  "age_limit",
  "restrictions",
  "description",
  "booking_start",
  "booking_end",
  "event_start",
  "event_end",
  "view_count",
  "seat_selectable",
  "min_price",
  "max_price",
  "status",
  "event_category_id",
  "manager_id",
  "seat_layout_id",
  "deleted",
  "created_at",
  "modified_at",
  "version",
  "sales_version"
) VALUES
  (
    'evt-1001',
    'Seoul Concert 2026',
    'https://example.com/thumb1.jpg',
    0,
    NULL,
    'Test event in Seoul',
    TIMESTAMP '2026-02-15 10:00:00',
    TIMESTAMP '2026-02-18 23:59:59',
    TIMESTAMP '2026-02-20 19:00:00',
    TIMESTAMP '2026-02-20 21:00:00',
    0,
    TRUE,
    50000,
    100000,
    'OPEN',
    1,
    'mgr-1',
    1001,
    FALSE,
    TIMESTAMP '2026-02-11 12:00:00',
    TIMESTAMP '2026-02-11 12:00:00',
    0,
    0
  ),
  (
    'evt-1002',
    'Busan Jazz Night',
    'https://example.com/thumb2.jpg',
    12,
    NULL,
    'Test event in Busan',
    TIMESTAMP '2026-02-16 10:00:00',
    TIMESTAMP '2026-02-19 23:00:00',
    TIMESTAMP '2026-02-22 18:00:00',
    TIMESTAMP '2026-02-22 20:00:00',
    0,
    TRUE,
    40000,
    120000,
    'OPEN',
    2,
    'mgr-1',
    1002,
    FALSE,
    TIMESTAMP '2026-02-11 12:05:00',
    TIMESTAMP '2026-02-11 12:05:00',
    0,
    0
  );

-- Optional: If you want seatCount/min/max from the materialized stats table in H2 tests,
-- populate seat_layout_stats once (H2 has no MySQL triggers in this repo by default).
--
-- INSERT INTO "seat_layout_stats" ("layout_id", "seat_count", "min_price", "max_price", "last_updated")
-- SELECT "layout_id", COUNT(*), MIN("amount"), MAX("amount"), CURRENT_TIMESTAMP
-- FROM "seat"
-- GROUP BY "layout_id";
