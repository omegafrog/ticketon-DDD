#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BASE_URL="${BASE_URL:-http://localhost:8080}"
OUTPUT_FILE="${OUTPUT_FILE:-$ROOT_DIR/load-tests/payment-test-data.json}"
REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"
ENTRY_TOKEN_TTL_SECONDS="${ENTRY_TOKEN_TTL_SECONDS:-3600}"
EVENT_ID="${EVENT_ID:-event-k6-001}"
AMOUNT="${AMOUNT:-50000}"
USER_COUNT="${USER_COUNT:-1000}"
PAYMENT_PROVIDER="${PAYMENT_PROVIDER:-TOSS}"
RUN_ID="${RUN_ID:-k6-$(date +%Y%m%d%H%M%S)}"
DEFAULT_PASSWORD="${DEFAULT_PASSWORD:-password123!}"
HTTP_TIMEOUT_SECONDS="${HTTP_TIMEOUT_SECONDS:-10}"

export BASE_URL OUTPUT_FILE REDIS_HOST REDIS_PORT REDIS_PASSWORD ENTRY_TOKEN_TTL_SECONDS
export EVENT_ID AMOUNT USER_COUNT PAYMENT_PROVIDER RUN_ID DEFAULT_PASSWORD HTTP_TIMEOUT_SECONDS

command -v python3 >/dev/null 2>&1 || { echo "python3 is required" >&2; exit 1; }
command -v redis-cli >/dev/null 2>&1 || { echo "redis-cli is required" >&2; exit 1; }
mkdir -p "$(dirname "$OUTPUT_FILE")"

python3 - <<'PY'
import base64, json, os, subprocess, time, urllib.error, urllib.request, uuid

base_url = os.environ["BASE_URL"].rstrip("/")
out = os.environ["OUTPUT_FILE"]
redis_host = os.environ["REDIS_HOST"]
redis_port = os.environ["REDIS_PORT"]
redis_password = os.environ.get("REDIS_PASSWORD", "")
entry_ttl = int(os.environ["ENTRY_TOKEN_TTL_SECONDS"])
event_id = os.environ["EVENT_ID"]
amount = int(os.environ["AMOUNT"])
user_count = int(os.environ["USER_COUNT"])
provider = os.environ["PAYMENT_PROVIDER"]
run_id = os.environ["RUN_ID"]
password = os.environ["DEFAULT_PASSWORD"]
timeout = int(os.environ["HTTP_TIMEOUT_SECONDS"])

def call(method, path, body, headers=None):
    data = json.dumps(body).encode()
    req_headers = {"Content-Type": "application/json"}
    if headers:
        req_headers.update(headers)
    req = urllib.request.Request(base_url + path, data=data, method=method, headers=req_headers)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as res:
            text = res.read().decode()
            return res.status, res.headers, json.loads(text or "{}")
    except urllib.error.HTTPError as e:
        text = e.read().decode(errors="replace")
        try:
            body = json.loads(text or "{}")
        except json.JSONDecodeError:
            body = {"raw": text}
        return e.code, e.headers, body

def jwt_claims(token):
    token = token.removeprefix("Bearer ").strip()
    payload = token.split(".")[1]
    payload += "=" * (-len(payload) % 4)
    return json.loads(base64.urlsafe_b64decode(payload).decode())

def redis(*args):
    cmd = ["redis-cli", "-h", redis_host, "-p", redis_port]
    if redis_password:
        cmd += ["-a", redis_password]
    subprocess.run(cmd + list(args), check=True, stdout=subprocess.DEVNULL)

def register(row):
    status, _, body = call("POST", "/api/v1/auth/register", {
        "email": row["email"], "password": row["password"], "name": row["name"],
        "age": row["age"], "sex": row["sex"], "phoneNum": row["phoneNum"], "location": row["location"]
    })
    if status in (200, 201, 202):
        return
    msg = json.dumps(body, ensure_ascii=False).lower()
    if status in (400, 409) and ("already" in msg or "exists" in msg or "이미" in msg):
        return
    raise RuntimeError(f"register failed email={row['email']} status={status} body={msg[:500]}")

def login(row):
    status, headers, body = call("POST", "/api/v1/auth/login", {"email": row["email"], "password": row["password"]})
    if status != 200:
        raise RuntimeError(f"login failed email={row['email']} status={status} body={json.dumps(body, ensure_ascii=False)[:500]}")
    token = str((body.get("data") if isinstance(body, dict) else None) or headers.get("Authorization") or "").removeprefix("Bearer ").strip()
    cookie = None
    for value in headers.get_all("Set-Cookie", []):
        if value.startswith("refreshToken="):
            cookie = value.split(";", 1)[0].split("=", 1)[1]
            break
    if not token or not cookie:
        raise RuntimeError(f"login response missing token/cookie email={row['email']}")
    claims = jwt_claims(token)
    row["userId"] = claims["userId"]
    row["role"] = str(claims.get("role", "USER"))
    row["accessToken"] = token
    row["refreshToken"] = cookie

def init(row):
    status, _, body = call("POST", "/api/v1/payments/init", {
        "eventId": row["eventId"], "orderId": row["orderId"], "amount": row["amount"]
    }, {
        "Authorization": "Bearer " + row["accessToken"],
        "entryAuthToken": row["entryAuthToken"],
        "Cookie": "refreshToken=" + row["refreshToken"],
    })
    if status != 201:
        raise RuntimeError(f"init failed userId={row['userId']} status={status} body={json.dumps(body, ensure_ascii=False)[:500]}")
    row["purchaseId"] = body["data"]["purchaseId"]

print(f"runId={run_id} baseUrl={base_url} eventId={event_id} userCount={user_count}")
rows = []
now_ms = str(int(time.time() * 1000))
for i in range(1, user_count + 1):
    key = f"{run_id}-{i:06d}"
    row = {
        "email": f"{key}@example.com", "password": password, "name": f"K6 User {i}",
        "age": 20, "sex": "M", "phoneNum": f"010{i:08d}"[-11:], "location": "SEOUL",
        "entryAuthToken": str(uuid.uuid4()), "eventId": event_id,
        "orderId": f"{run_id}-order-{i:06d}", "amount": amount,
        "provider": provider, "paymentKey": f"{run_id}-payment-key-{i:06d}",
    }
    register(row)
    login(row)
    redis("SETEX", "ENTRY_TOKEN:" + row["userId"], str(entry_ttl), row["entryAuthToken"])
    redis("SETEX", "ENTRY_EVENT:" + row["userId"], str(entry_ttl), row["eventId"])
    redis("ZADD", "ENTRY_LAST_SEEN", now_ms, row["userId"])
    init(row)
    row.pop("password", None)
    rows.append(row)
    if i % 100 == 0 or i == user_count:
        print(f"seeded {i}/{user_count}")

tmp = out + ".tmp"
with open(tmp, "w", encoding="utf-8") as f:
    json.dump({"payments": rows}, f, ensure_ascii=False, indent=2)
    f.write("\n")
os.replace(tmp, out)
print(f"created={out}")
PY

echo "Done. k6 must run with INIT_PURCHASES=0"
