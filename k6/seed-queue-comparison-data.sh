#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-password}"
MYSQL_DATABASE="${MYSQL_DATABASE:-ticketon}"

EVENT_ID="${EVENT_ID:-event-k6-001}"
SEAT_LAYOUT_ID="${SEAT_LAYOUT_ID:-9001}"
EVENT_TITLE="${EVENT_TITLE:-K6 Purchase Test Event}"
EVENT_MANAGER_ID="${EVENT_MANAGER_ID:-k6-manager-001}"
EVENT_CATEGORY_ID="${EVENT_CATEGORY_ID:-1}"
SEAT_PRICE="${SEAT_PRICE:-50000}"

USER_COUNT="${USER_COUNT:-1000}"
USER_EMAIL_PREFIX="${USER_EMAIL_PREFIX:-user}"
LOGIN_EMAIL_DOMAIN="${LOGIN_EMAIL_DOMAIN:-example.com}"
DEFAULT_PASSWORD="${DEFAULT_PASSWORD:-password123!}"
START_INDEX="${START_INDEX:-1}"
HTTP_TIMEOUT_SECONDS="${HTTP_TIMEOUT_SECONDS:-10}"
BASE_URL="${BASE_URL:-http://localhost:8080}"

command -v mysql >/dev/null 2>&1 || { echo "mysql client is required" >&2; exit 1; }
command -v python3 >/dev/null 2>&1 || { echo "python3 is required" >&2; exit 1; }

mysql_exec() {
  mysql \
    -h "${MYSQL_HOST}" \
    -P "${MYSQL_PORT}" \
    -u"${MYSQL_USER}" \
    -p"${MYSQL_PASSWORD}" \
    "${MYSQL_DATABASE}"
}

mysql_exec <<SQL
SET FOREIGN_KEY_CHECKS = 0;

INSERT INTO seat_layout (
  id,
  layout,
  location,
  hall_name,
  region_location
) VALUES (
  ${SEAT_LAYOUT_ID},
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
  ('k6-seat-${SEAT_LAYOUT_ID}-A1', 'A1', 'K6', ${SEAT_PRICE}, TRUE, ${SEAT_LAYOUT_ID}),
  ('k6-seat-${SEAT_LAYOUT_ID}-A2', 'A2', 'K6', ${SEAT_PRICE}, TRUE, ${SEAT_LAYOUT_ID}),
  ('k6-seat-${SEAT_LAYOUT_ID}-A3', 'A3', 'K6', ${SEAT_PRICE}, TRUE, ${SEAT_LAYOUT_ID}),
  ('k6-seat-${SEAT_LAYOUT_ID}-B1', 'B1', 'K6', ${SEAT_PRICE}, TRUE, ${SEAT_LAYOUT_ID}),
  ('k6-seat-${SEAT_LAYOUT_ID}-B2', 'B2', 'K6', ${SEAT_PRICE}, TRUE, ${SEAT_LAYOUT_ID}),
  ('k6-seat-${SEAT_LAYOUT_ID}-B3', 'B3', 'K6', ${SEAT_PRICE}, TRUE, ${SEAT_LAYOUT_ID})
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
  '${EVENT_ID}',
  '${EVENT_TITLE}',
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
  ${SEAT_PRICE},
  ${SEAT_PRICE},
  'OPEN',
  ${EVENT_CATEGORY_ID},
  '${EVENT_MANAGER_ID}',
  ${SEAT_LAYOUT_ID},
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
SQL

USER_COUNT="${USER_COUNT}" \
USER_EMAIL_PREFIX="${USER_EMAIL_PREFIX}" \
LOGIN_EMAIL_DOMAIN="${LOGIN_EMAIL_DOMAIN}" \
DEFAULT_PASSWORD="${DEFAULT_PASSWORD}" \
START_INDEX="${START_INDEX}" \
HTTP_TIMEOUT_SECONDS="${HTTP_TIMEOUT_SECONDS}" \
BASE_URL="${BASE_URL}" \
python3 "${ROOT_DIR}/k6/seed-queue-comparison-users.py"

echo "Seeded queue comparison data:"
echo "  eventId=${EVENT_ID}"
echo "  users=${USER_COUNT}"
echo "  emailPrefix=${USER_EMAIL_PREFIX}"
echo "  domain=${LOGIN_EMAIL_DOMAIN}"
