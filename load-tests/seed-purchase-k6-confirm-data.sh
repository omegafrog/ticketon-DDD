#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

BASE_URL="${BASE_URL:-http://localhost:8080}"
OUTPUT_FILE="${OUTPUT_FILE:-$ROOT_DIR/load-tests/payment-test-data.json}"

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-password}"
MYSQL_DATABASE="${MYSQL_DATABASE:-ticketon}"

EVENT_ID="${EVENT_ID:-event-k6-001}"
AMOUNT="${AMOUNT:-50000}"
USER_COUNT="${USER_COUNT:-1000}"
PAYMENT_PROVIDER="${PAYMENT_PROVIDER:-TOSS}"
RUN_ID="${RUN_ID:-k6-$(date +%Y%m%d%H%M%S)}"
DEFAULT_PASSWORD="${DEFAULT_PASSWORD:-password123!}"
HTTP_TIMEOUT_SECONDS="${HTTP_TIMEOUT_SECONDS:-10}"

export BASE_URL OUTPUT_FILE
export MYSQL_HOST MYSQL_PORT MYSQL_USER MYSQL_PASSWORD MYSQL_DATABASE
export EVENT_ID AMOUNT USER_COUNT PAYMENT_PROVIDER RUN_ID DEFAULT_PASSWORD HTTP_TIMEOUT_SECONDS

command -v python3 >/dev/null 2>&1 || { echo "python3 is required" >&2; exit 1; }
command -v mysql >/dev/null 2>&1 || { echo "mysql client is required" >&2; exit 1; }

mkdir -p "$(dirname "$OUTPUT_FILE")"

python3 - <<'PY'
import base64
import json
import os
import subprocess
import tempfile
import time
import urllib.error
import urllib.request
import uuid

base_url = os.environ["BASE_URL"].rstrip("/")
out = os.environ["OUTPUT_FILE"]

mysql_host = os.environ["MYSQL_HOST"]
mysql_port = os.environ["MYSQL_PORT"]
mysql_user = os.environ["MYSQL_USER"]
mysql_password = os.environ["MYSQL_PASSWORD"]
mysql_database = os.environ["MYSQL_DATABASE"]

event_id = os.environ["EVENT_ID"]
amount = int(os.environ["AMOUNT"])
user_count = int(os.environ["USER_COUNT"])
provider = os.environ["PAYMENT_PROVIDER"]
run_id = os.environ["RUN_ID"]
password = os.environ["DEFAULT_PASSWORD"]
timeout = int(os.environ["HTTP_TIMEOUT_SECONDS"])


def call(method, path, body, headers=None):
    req_headers = {"Content-Type": "application/json"}
    if headers:
        req_headers.update(headers)

    req = urllib.request.Request(
        base_url + path,
        data=json.dumps(body).encode("utf-8"),
        method=method,
        headers=req_headers,
    )

    try:
        with urllib.request.urlopen(req, timeout=timeout) as res:
            raw = res.read().decode("utf-8")
            return res.status, res.headers, json.loads(raw or "{}")
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="replace")
        try:
            parsed = json.loads(raw or "{}")
        except json.JSONDecodeError:
            parsed = {"raw": raw}
        return e.code, e.headers, parsed


def jwt_payload(token):
    token = token.removeprefix("Bearer ").strip()
    payload = token.split(".")[1]
    payload += "=" * (-len(payload) % 4)
    return json.loads(base64.urlsafe_b64decode(payload).decode("utf-8"))


def phone_number(i):
    digits = f"{i:08d}"[-8:]
    return f"010-{digits[:4]}-{digits[4:]}"


def sql_quote(value):
    if value is None:
        return "NULL"
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def register(row):
    status, _, body = call("POST", "/api/v1/auth/register", {
        "email": row["email"],
        "password": row["password"],
        "name": row["name"],
        "age": row["age"],
        "sex": row["sex"],
        "phoneNum": row["phoneNum"],
        "location": row["location"],
    })

    if status in (200, 201, 202):
        return

    msg = json.dumps(body, ensure_ascii=False).lower()
    if status in (400, 409) and ("already" in msg or "exists" in msg or "이미" in msg):
        return

    raise RuntimeError(
        f"register failed status={status} email={row['email']} body={msg[:500]}"
    )


def login(row):
    status, headers, body = call("POST", "/api/v1/auth/login", {
        "email": row["email"],
        "password": row["password"],
    })

    if status != 200:
        raise RuntimeError(
            f"login failed status={status} email={row['email']} "
            f"body={json.dumps(body, ensure_ascii=False)[:500]}"
        )

    token = str(
        (body.get("data") if isinstance(body, dict) else None)
        or headers.get("Authorization")
        or ""
    )
    token = token.removeprefix("Bearer ").strip()

    refresh = None
    for cookie in headers.get_all("Set-Cookie", []):
        if cookie.startswith("refreshToken="):
            refresh = cookie.split(";", 1)[0].split("=", 1)[1]
            break

    if not token or not refresh:
        raise RuntimeError(
            f"login response missing token or refresh cookie email={row['email']}"
        )

    claims = jwt_payload(token)

    row["userId"] = claims["userId"]
    row["role"] = str(claims.get("role", "USER"))
    row["accessToken"] = token
    row["refreshToken"] = refresh


def insert_purchases(rows):
    values = []

    for row in rows:
        values.append("(" + ", ".join([
            sql_quote(row["purchaseId"]),
            sql_quote(row["orderId"]),
            sql_quote(f"K6 Order {row['purchaseId']}"),
            sql_quote(row["eventId"]),
            "NULL",
            str(row["amount"]),
            "0",
            "NULL",
            sql_quote("IN_PROGRESS"),
            "NOW()",
            "DATE_ADD(NOW(), INTERVAL 1 HOUR)",
            sql_quote(row["userId"]),
        ]) + ")")

    sql = f"""
INSERT INTO purchase (
  purchase_id,
  order_id,
  order_name,
  event_id,
  pid,
  amount,
  expected_sales_version,
  payment_method,
  payment_status,
  created_at,
  payment_deadline_at,
  user_id
)
VALUES
{",\n".join(values)}
ON DUPLICATE KEY UPDATE
  order_id = VALUES(order_id),
  order_name = VALUES(order_name),
  event_id = VALUES(event_id),
  pid = VALUES(pid),
  amount = VALUES(amount),
  expected_sales_version = VALUES(expected_sales_version),
  payment_method = VALUES(payment_method),
  payment_status = VALUES(payment_status),
  payment_deadline_at = VALUES(payment_deadline_at),
  user_id = VALUES(user_id);
"""

    with tempfile.NamedTemporaryFile("w", encoding="utf-8", delete=False) as f:
        f.write(sql)
        path = f.name

    try:
        subprocess.run([
            "mysql",
            "-h", mysql_host,
            "-P", mysql_port,
            "-u", mysql_user,
            f"-p{mysql_password}",
            mysql_database,
            "--default-character-set=utf8mb4",
            "-e", f"source {path}",
        ], check=True)
    finally:
        os.remove(path)


print(f"runId={run_id}")
print(f"baseUrl={base_url}")
print(f"eventId={event_id}")
print(f"userCount={user_count}")
print("flow=auth register -> auth login -> direct purchase insert -> payment-test-data.json")

rows = []

for i in range(1, user_count + 1):
    key = f"{run_id}-{i:06d}"

    row = {
        "email": f"{key}@example.com",
        "password": password,
        "name": f"K6 User {i}",
        "age": 20,
        "sex": "MALE",
        "phoneNum": phone_number(i),
        "location": "SEOUL",

        "eventId": event_id,
        "purchaseId": str(uuid.uuid4()),
        "orderId": f"{run_id}-order-{i:06d}",
        "amount": amount,
        "provider": provider,
        "paymentKey": f"{run_id}-payment-key-{i:06d}",

        # confirm controller currently requires this header.
        # Current confirm implementation receives it but does not validate it.
        # Keep a stable dummy value until issue #30 is implemented.
        "entryAuthToken": "dummy-entry-token",
    }

    register(row)
    login(row)

    row.pop("password", None)
    rows.append(row)

    if i % 100 == 0 or i == user_count:
        print(f"auth prepared {i}/{user_count}")

insert_purchases(rows)
print(f"purchase rows inserted={len(rows)}")

tmp = out + ".tmp"
with open(tmp, "w", encoding="utf-8") as f:
    json.dump({"payments": rows}, f, ensure_ascii=False, indent=2)
    f.write("\n")

os.replace(tmp, out)

print(f"created={out}")
print("k6 must run with INIT_PURCHASES=0")
PY

echo "Done."
