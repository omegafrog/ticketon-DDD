#!/usr/bin/env bash
set -euo pipefail

# Generate deterministic purchase k6 test data.
#
# Output:
#   - load-tests/payment-test-data.json
#   - Redis keys required by EntryTokenValidator
#       ENTRY_TOKEN:{userId} = {entryAuthToken}
#       ENTRY_EVENT:{userId} = {eventId}
#       ENTRY_LAST_SEEN zset = userId -> timestamp
#
# Optional:
#   PREPARE_PURCHASES=1 also calls POST /api/v1/payments/init for every row
#   and fills purchaseId in payment-test-data.json, so confirm tests can run
#   without INIT_PURCHASES=1 in k6.
#
# Example:
#   chmod +x load-tests/seed-purchase-k6-data.sh
#   EVENT_ID=<existing-event-id> \
#   JWT_SECRET=<same-value-as-custom.jwt.secret> \
#   USER_COUNT=1000 \
#   ./load-tests/seed-purchase-k6-data.sh
#
# For confirm test with pre-created purchases:
#   EVENT_ID=<existing-event-id> \
#   JWT_SECRET=<same-value-as-custom.jwt.secret> \
#   USER_COUNT=1000 \
#   PREPARE_PURCHASES=1 \
#   ./load-tests/seed-purchase-k6-data.sh
#
# Then:
#   k6 run \
#     -e BASE_URL=http://localhost:8080 \
#     -e PAYMENT_DATA_FILE=./load-tests/payment-test-data.json \
#     -e INIT_PURCHASES=0 \
#     load-tests/payment-async-confirm.js

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

BASE_URL="${BASE_URL:-http://localhost:8080}"
OUTPUT_FILE="${OUTPUT_FILE:-$ROOT_DIR/load-tests/payment-test-data.json}"

REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"
ENTRY_TOKEN_TTL_SECONDS="${ENTRY_TOKEN_TTL_SECONDS:-3600}"

EVENT_ID="${EVENT_ID:-event-k6-001}"
AMOUNT="${AMOUNT:-50000}"
PAYMENT_PROVIDER="${PAYMENT_PROVIDER:-TOSS}"
RUN_ID="${RUN_ID:-k6-$(date +%Y%m%d%H%M%S)}"

# Must match gateway custom.jwt.secret.
# JJWT hmacShaKeyFor requires a sufficiently long HS256 secret, so use >= 32 bytes.
JWT_SECRET="${JWT_SECRET:-${CUSTOM_JWT_SECRET:-ticketon-local-k6-jwt-secret-32-bytes}}"

# When USER_COUNT is empty, this script computes the same minimum row count
# expected by load-tests/payment-async-confirm.js.
USER_COUNT="${USER_COUNT:-}"
VUS="${VUS:-100}"
RATE="${RATE:-$VUS}"
WARMUP="${WARMUP:-3m}"
DURATION="${DURATION:-5m}"
COOLDOWN="${COOLDOWN:-1m}"
ROW_BLOCK_SIZE="${ROW_BLOCK_SIZE:-}"

# If 1, call /api/v1/payments/init now and store purchaseId in output JSON.
# If 0, k6 can call init during setup by running with INIT_PURCHASES=1.
PREPARE_PURCHASES="${PREPARE_PURCHASES:-0}"
HTTP_TIMEOUT_SECONDS="${HTTP_TIMEOUT_SECONDS:-10}"

command -v python3 >/dev/null 2>&1 || { echo "python3 is required" >&2; exit 1; }
command -v redis-cli >/dev/null 2>&1 || { echo "redis-cli is required" >&2; exit 1; }

mkdir -p "$(dirname "$OUTPUT_FILE")"

python3 - "$OUTPUT_FILE" <<'PY'
import base64
import hashlib
import hmac
import json
import math
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request
import uuid

output_file = sys.argv[1]

base_url = os.environ.get("BASE_URL", "http://localhost:8080").rstrip("/")
redis_host = os.environ.get("REDIS_HOST", "127.0.0.1")
redis_port = os.environ.get("REDIS_PORT", "6379")
redis_password = os.environ.get("REDIS_PASSWORD", "")
entry_ttl = int(os.environ.get("ENTRY_TOKEN_TTL_SECONDS", "3600"))

event_id = os.environ.get("EVENT_ID", "event-k6-001")
amount = int(os.environ.get("AMOUNT", "50000"))
provider = os.environ.get("PAYMENT_PROVIDER", "TOSS")
run_id = os.environ.get("RUN_ID") or time.strftime("k6-%Y%m%d%H%M%S")
jwt_secret = (os.environ.get("JWT_SECRET") or os.environ.get("CUSTOM_JWT_SECRET") or "ticketon-local-k6-jwt-secret-32-bytes").encode("utf-8")

vus = int(os.environ.get("VUS", "100"))
rate = int(os.environ.get("RATE", str(vus)))
warmup = os.environ.get("WARMUP", "3m")
duration = os.environ.get("DURATION", "5m")
cooldown = os.environ.get("COOLDOWN", "1m")
row_block_size_env = os.environ.get("ROW_BLOCK_SIZE", "")
user_count_env = os.environ.get("USER_COUNT", "")
prepare_purchases = os.environ.get("PREPARE_PURCHASES", "0") == "1"
http_timeout = int(os.environ.get("HTTP_TIMEOUT_SECONDS", "10"))

if len(jwt_secret) < 32:
    raise SystemExit("JWT_SECRET must be at least 32 bytes for HS256")

def duration_seconds(value: str) -> int:
    value = str(value).strip()
    if value.endswith("ms"):
        return math.ceil(int(value[:-2]) / 1000)
    if value.endswith("s"):
        return int(value[:-1])
    if value.endswith("m"):
        return int(value[:-1]) * 60
    if value.endswith("h"):
        return int(value[:-1]) * 3600
    return int(value)

def scenario_count(name: str) -> int:
    if name == "warmup":
        return math.ceil(max(1, math.floor(rate / 2)) * duration_seconds(warmup))
    if name == "measurement":
        return math.ceil(rate * duration_seconds(duration))
    if name == "cooldown":
        return math.ceil(max(1, math.floor(rate / 4)) * duration_seconds(cooldown))
    return 0

max_vus = max(vus, rate * 2)
estimated_iterations = scenario_count("warmup") + scenario_count("measurement") + scenario_count("cooldown") + max_vus
row_block_size = int(row_block_size_env) if row_block_size_env else math.ceil(estimated_iterations / max_vus) + 10
required_rows = max_vus * row_block_size
user_count = int(user_count_env) if user_count_env else required_rows

def b64url(raw: bytes) -> str:
    return base64.urlsafe_b64encode(raw).rstrip(b"=").decode("ascii")

def sign_jwt(claims: dict, exp_seconds: int) -> str:
    now = int(time.time())
    payload = dict(claims)
    payload.setdefault("iat", now)
    payload.setdefault("exp", now + exp_seconds)
    header = {"alg": "HS256", "typ": "JWT"}
    signing_input = ".".join([
        b64url(json.dumps(header, separators=(",", ":")).encode("utf-8")),
        b64url(json.dumps(payload, separators=(",", ":")).encode("utf-8")),
    ])
    signature = hmac.new(jwt_secret, signing_input.encode("ascii"), hashlib.sha256).digest()
    return signing_input + "." + b64url(signature)

def redis_cmd(*args: str) -> None:
    cmd = ["redis-cli", "-h", redis_host, "-p", str(redis_port)]
    if redis_password:
        cmd.extend(["-a", redis_password])
    cmd.extend(args)
    subprocess.run(cmd, check=True, stdout=subprocess.DEVNULL)

def post_init(row: dict) -> str:
    body = json.dumps({
        "eventId": row["eventId"],
        "orderId": row["orderId"],
        "amount": row["amount"],
    }).encode("utf-8")
    req = urllib.request.Request(
        f"{base_url}/api/v1/payments/init",
        data=body,
        method="POST",
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {row['accessToken']}",
            "entryAuthToken": row["entryAuthToken"],
            "Cookie": f"refreshToken={row['refreshToken']}",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=http_timeout) as res:
            status = res.status
            text = res.read().decode("utf-8")
    except urllib.error.HTTPError as e:
        text = e.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"init failed userId={row['userId']} status={e.code} body={text[:500]}") from e
    if status != 201:
        raise RuntimeError(f"init failed userId={row['userId']} status={status} body={text[:500]}")
    parsed = json.loads(text or "{}")
    data = parsed.get("data") or parsed
    purchase_id = data.get("purchaseId")
    if not purchase_id:
        raise RuntimeError(f"init response has no purchaseId userId={row['userId']} body={text[:500]}")
    return purchase_id

rows = []
print(f"seed run_id={run_id}")
print(f"event_id={event_id}, amount={amount}, provider={provider}")
print(f"rows={user_count}, row_block_size={row_block_size}, required_rows={required_rows}")

now_ms = int(time.time() * 1000)
for i in range(1, user_count + 1):
    user_id = f"{run_id}-user-{i:06d}"
    email = f"{user_id}@example.com"
    access_jti = str(uuid.uuid4())
    access_token = sign_jwt({
        "userId": user_id,
        "role": "USER",
        "email": email,
        "jti": access_jti,
    }, exp_seconds=60 * 60 * 6)
    refresh_token = sign_jwt({
        "access-jti": access_jti,
    }, exp_seconds=60 * 60 * 24 * 7)
    entry_token = str(uuid.uuid4())

    row = {
        "userId": user_id,
        "email": email,
        "role": "USER",
        "accessToken": access_token,
        "refreshToken": refresh_token,
        "entryAuthToken": entry_token,
        "eventId": event_id,
        "orderId": f"{run_id}-order-{i:06d}",
        "amount": amount,
        "provider": provider,
        "paymentKey": f"{run_id}-payment-key-{i:06d}",
    }

    redis_cmd("SETEX", f"ENTRY_TOKEN:{user_id}", str(entry_ttl), entry_token)
    redis_cmd("SETEX", f"ENTRY_EVENT:{user_id}", str(entry_ttl), event_id)
    redis_cmd("ZADD", "ENTRY_LAST_SEEN", str(now_ms), user_id)

    rows.append(row)

    if i % 1000 == 0:
        print(f"redis seeded {i}/{user_count}")

if prepare_purchases:
    print("prepare purchases through /api/v1/payments/init")
    for i, row in enumerate(rows, start=1):
        row["purchaseId"] = post_init(row)
        if i % 100 == 0:
            print(f"purchase initiated {i}/{user_count}")

tmp_file = output_file + ".tmp"
with open(tmp_file, "w", encoding="utf-8") as f:
    json.dump({"payments": rows}, f, ensure_ascii=False, indent=2)
    f.write("\n")
os.replace(tmp_file, output_file)

print(f"created {output_file}")
print("redis keys:")
print("  ENTRY_TOKEN:{userId}")
print("  ENTRY_EVENT:{userId}")
print("  ENTRY_LAST_SEEN")
if not prepare_purchases:
    print("next: run k6 with INIT_PURCHASES=1, or rerun this script with PREPARE_PURCHASES=1")
PY

echo
echo "Done."
echo "PAYMENT_DATA_FILE=$OUTPUT_FILE"
echo
echo "Example async confirm run:"
echo "k6 run \\\n  -e BASE_URL=$BASE_URL \\\n  -e PAYMENT_DATA_FILE=$OUTPUT_FILE \\\n  -e INIT_PURCHASES=0 \\\n  load-tests/payment-async-confirm.js"
