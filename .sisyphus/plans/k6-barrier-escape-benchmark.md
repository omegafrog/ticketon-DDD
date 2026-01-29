# k6 Barrier Escape Benchmark (main vs benchmark/hash-waiting-queue)

## Context

### Original Request
- `main` 브랜치와 `benchmark/hash-waiting-queue` 브랜치에서 **동일 조건**으로 `sse-throughput-test`를 실행해 *waiting-queue barrier test*를 진행한다.
- 성능 측정 대상: **"VU가 0이 되기까지 걸린 시간"** (모든 유저가 대기열을 탈출해 k6 실행이 종료될 때까지의 시간).
- 산출물: 테스트 시나리오 + 테스트 실행용 쉘 스크립트.

### Confirmed Inputs
- EVENT_IDS (5, 고정):
  - `0dc2efd8-f760-11f0-8e3b-2a897f646219`
  - `b55834d0-f834-11f0-999b-a7e49b723099`
  - `ba1d5e02-f834-11f0-999b-05d0f9a25396`
  - `bc113ec4-f834-11f0-999b-1786beee8658`
  - `be54c626-f834-11f0-999b-9fdbde76a760`
- Load-test users: `user{1..10000}@example.com` / `password123`
- Runner script scope: **k6-only runner**
  - Redis 정리/옵션 설정(선택) → `k6 run` → `summary.json` 파싱 → 결과 폴더 저장
  - 브랜치 체크아웃/서비스 기동은 스크립트 범위 밖(사용자가 각 브랜치에서 서비스 기동 후 실행)

### Repo References (primary)
- Harness: `k6/sse-throughput-test.js`
- Existing planning context:
  - `.sisyphus/plans/k6-waiting-queue-hash-vs-zset.md`
  - `.sisyphus/plans/k6-admincontroller-exit.md`
- Infra key model docs:
  - `docs/dispatcher/waitingqueue.md`
  - `docs/troubleshooting/entry-queue-count-meaning.md`

---

## Work Objectives

### Core Objective
두 브랜치에서 동일한 k6 harness를 사용해, **k6 종료까지 걸린 시간(=VUs→0)** 을 비교 가능한 형태로 수집/저장한다.

### Concrete Deliverables
- (A) 브랜치-비의존 k6 harness 사본: `.sisyphus/bench/sse-throughput-test.js`
  - WAITING 키 타입(zset/hash) 차이로 인한 `WRONGTYPE`를 피하도록 type-aware 처리
- (B) k6-only runner script: `scripts/run-k6-barrier-benchmark.sh`
  - Redis scoped cleanup + (옵션) slot-boost + k6 실행 + 결과 저장 + 주요 지표 추출
- (C) 결과 저장 구조(표준화): `.sisyphus/evidence/k6/<timestamp>/<branch>/<scenario>/...`

### Definition of Done
- 두 브랜치 모두에서 아래가 성립:
  - `k6 run` exit code = 0
  - 결과 폴더에 `summary.json` + `stdout.txt` + `meta.txt`(및 선택적으로 `meta.json`)가 생성됨
  - `summary.json.metrics.completed_users.values.count == VUS` (또는 스크립트가 지정한 VUS)
  - headline metric(`state.testRunDurationMs`)가 추출/저장됨

### Must NOT Have (Guardrails)
- Redis가 “공유” 환경일 수 있으므로 기본값은 **scoped deletes only**로 한다. `FLUSHALL/FLUSHDB` 기본 금지.
- 브랜치별로 서로 다른 k6 스크립트를 쓰지 않는다(동일 harness 사본 사용).
- 브랜치 비교 중간에 파라미터(ENDPOINT, SLOT_BOOST 등)를 바꾸지 않는다.

---

## Task Checklist

- [x] 1) Create branch-agnostic harness copy at `.sisyphus/bench/sse-throughput-test.js`
- [x] 2) Create runner script at `scripts/run-k6-barrier-benchmark.sh`
- [x] 3) Verify runner on current branch (smoke: `SCENARIO=custom VUS=50`)
- [ ] 4) Verify barrier works on both branches without `WRONGTYPE`
- [x] 5) Document exact run commands for both branches (smoke/baseline/target)

---

## Run Commands (copy/paste)

> IMPORTANT: 각 브랜치에서 서비스를 띄운 뒤(해당 브랜치 코드로), 아래 runner를 실행한다.
> Redis는 테스트 전용이 아니므로, runner는 해당 `EVENT_IDS`에 대한 큐 키만 scoped delete 한다.

Common env (both branches):

```bash
export EVENT_IDS="0dc2efd8-f760-11f0-8e3b-2a897f646219,b55834d0-f834-11f0-999b-a7e49b723099,ba1d5e02-f834-11f0-999b-05d0f9a25396,bc113ec4-f834-11f0-999b-1786beee8658,be54c626-f834-11f0-999b-9fdbde76a760"
export BASE_URL="http://localhost:8080"  # gateway
export LOGIN_EMAIL_DOMAIN="example.com"
export ADMIN_EMAIL="admin@example.com"
export ADMIN_PASSWORD="password123"
export AGGRESSIVE_CLEAN=0
```

Smoke (sanity):

```bash
SCENARIO=custom VUS=50 scripts/run-k6-barrier-benchmark.sh
```

Baseline:

```bash
SCENARIO=baseline scripts/run-k6-barrier-benchmark.sh
```

Target:

```bash
SCENARIO=target scripts/run-k6-barrier-benchmark.sh
```

---

## Verification Strategy (Manual / Benchmark)

### Primary Metric
- `summary.json.state.testRunDurationMs`
  - k6 실행 전체 wall time(일시정지 제외)로 해석
  - 추가 신뢰성 확보를 위해 runner가 외부 wall-clock start/end도 기록

### Validity Gates (실패 시 결과 무효)
- `k6` exit code != 0
- `summary.json` 없음
- `completed_users.count != VUS`
- `login_final_failures.count > 0` 또는 `sse_failures.count > 0`가 비정상적으로 큼(임계값은 환경에 맞게 조정)

---

## Technical Decisions (Defaults Applied)

### Load Levels
- 기본 3단계:
  - smoke: `VUS=50`, `SCENARIO=custom`
  - baseline: `SCENARIO=baseline` (default 2000)
  - target: `SCENARIO=target` (default 10000)
- `stress`(30000) 기본 제외

### Access Path (SSE)
- 기본값(권장): SSE도 gateway를 통해 통과
  - `BASE_URL=http://localhost:8080`
  - `BROKER_BASE_URL`는 미지정(=BASE_URL) 또는 동일값으로 지정
  - 장점: broker 포트가 랜덤(`server.port:0`)이어도 gateway+eureka로 라우팅 가능
  - 단점: gateway 오버헤드가 포함됨

### Redis Cleanup Policy
- 기본값: scoped deletes only (eventIds 기준으로 관련 키만 삭제)
- Redis는 테스트 전용이 아니므로 `AGGRESSIVE_CLEAN=0` 고정(권장)
- `AGGRESSIVE_CLEAN=1`은 테스트 전용 Redis에서만 고려

### Admin Credentials
- 확정: `ADMIN_EMAIL=admin@example.com`, `ADMIN_PASSWORD=password123`

### xk6 Extensions
- xk6 build (pinned):
  - `xk6 build --with github.com/grafana/xk6-redis@v0.3.6 --with github.com/phymbert/xk6-sse@v0.1.12`

---

## Task Flow

1) (1회) xk6/k6 바이너리 준비
2) (1회) 브랜치-비의존 harness 생성: `.sisyphus/bench/sse-throughput-test.js`
3) (1회) k6 runner 스크립트 생성: `scripts/run-k6-barrier-benchmark.sh`
4) (각 브랜치마다) 서비스 기동(수동) 후 runner 실행
5) 결과 비교/리포팅(수동 또는 간단 스크립트로)

---

## TODOs

### 0) Preflight: k6/xk6 준비

**What to do**:
- k6에서 `k6/x/redis`, `k6/x/sse` import가 가능하도록 xk6 바이너리 준비
- 권장: 위 pinned 빌드 커맨드로 `k6` 바이너리를 생성하고 PATH에 둔다

**References**:
- `k6/sse-throughput-test.js` (imports)

**Acceptance Criteria**:
- `k6 run k6/sse-throughput-test.js` 실행 시 `k6/x/redis`, `k6/x/sse` import 에러 없음(서비스 미기동이면 HTTP 실패는 가능)

### 1) Branch-agnostic k6 harness 생성 (.sisyphus/bench)

**What to do**:
- `k6/sse-throughput-test.js`를 기반으로 `.sisyphus/bench/sse-throughput-test.js`를 만든다.
- WAITING 키 카운트/프리컨디션 검사 부분을 type-aware로 수정:
  - `TYPE WAITING:<eventId>`
    - `zset` → `ZCARD`
    - `hash` → `HLEN`
    - `none` → 0
    - 그 외 → fail-fast (key+type 로그 포함)
- 적용 위치:
  - setup()의 precondition check
  - `getWaitingTotal()`
  - `getWaitingCountsByEvent()`

**Must NOT do**:
- 브랜치별로 다른 business logic을 넣지 않는다(오직 타입 분기 + 안전장치만)

**References**:
- `k6/sse-throughput-test.js`
- `.sisyphus/plans/k6-waiting-queue-hash-vs-zset.md` (동일 harness 유지 지침)

**Acceptance Criteria**:
- `main`에서 실행 시 barrier가 `WRONGTYPE` 없이 동작
- `benchmark/hash-waiting-queue`에서 실행 시 barrier가 `WRONGTYPE` 없이 동작

### 2) k6 runner script 작성 (k6-only)

**What to do**:
- `scripts/run-k6-barrier-benchmark.sh` 생성
- 기능:
  - 입력(ENV/args):
    - `SCENARIO` (custom|baseline|target)
    - `VUS` (custom에서 사용)
    - `EVENT_IDS` (쉼표로 join)
    - `BASE_URL` (default http://localhost:8080)
    - `BROKER_BASE_URL` (default empty → BASE_URL)
    - `REDIS_URL` (default redis://127.0.0.1:6379)
    - `LOGIN_EMAIL_DOMAIN=example.com`
    - `ADMIN_EMAIL`, `ADMIN_PASSWORD`
    - `SLOT_BOOST` (0/1), `SLOT_BOOST_VALUE` (예: 10000)
    - `AGGRESSIVE_CLEAN` (0/1)
  - Redis cleanup (scoped):
    - per eventId:
      - `DEL WAITING:<eventId>`
      - `DEL WAITING_USER_IDS:<eventId>`
      - `DEL WAITING_QUEUE_INDEX_RECORD:<eventId>`
      - `HDEL WAITING_QUEUE_IDX <eventId>`
      - (slot-boost OFF면) `HDEL ENTRY_QUEUE_SLOTS <eventId>`
      - (slot-boost ON면) `HSET ENTRY_QUEUE_SLOTS <eventId> <SLOT_BOOST_VALUE>`
    - if `AGGRESSIVE_CLEAN=1`:
      - `DEL ENTRY`
      - `SCAN MATCH DISPATCH:*` 후 해당 키들 DEL
  - `k6 run` 실행:
    - harness: `.sisyphus/bench/sse-throughput-test.js`
    - stdout 캡처(tee)
    - summary.json/메타 저장
  - 결과 폴더 생성:
    - `.sisyphus/evidence/k6/<timestamp>/<branch>/<scenario>/`
    - `meta.txt` 또는 `meta.json`: branch/sha, timestamps, env snapshot, k6 version
    - `stdout.txt`, `summary.json`, `derived.json`(headline values)
  - `derived.json`에 최소 포함:
    - `testRunDurationMs` (from summary.json.state.testRunDurationMs)
    - `completedUsers` (metrics.completed_users.count)
    - `barrierWaitMaxMs` (metrics.barrier_wait_ms.max)

**References**:
- `k6/sse-throughput-test.js` (env vars + summary.json 구조)
- `docs/dispatcher/waitingqueue.md` (Redis key 모델)

**Acceptance Criteria**:
- runner는 같은 브랜치에서 3개 시나리오(smoke/baseline/target)를 연속 실행 가능
- 각 실행마다 결과 폴더 생성 및 `derived.json` 생성

### 3) 실행 시나리오(사용자 실행 절차)

**What to do**:
- 각 브랜치에서 아래 절차로 실행:
  1) 브랜치 체크아웃
  2) 서비스 기동(수동; 기존 방식 사용)
  3) runner로 smoke → baseline → target 실행
  4) 다른 브랜치로 전환 후 동일 반복

**Noise reduction (optional)**:
- warmup 1 cycle discard + N=5 cycles(또는 7) interleaving은 추가 실험으로 수행

---

## Script Content (to be written by executor)

> executor는 아래 내용을 `scripts/run-k6-barrier-benchmark.sh`로 저장한다.

```bash
#!/usr/bin/env bash
set -euo pipefail

# Required env
EVENT_IDS_CSV="${EVENT_IDS:-}"  # comma-separated
if [[ -z "$EVENT_IDS_CSV" ]]; then
  echo "ERROR: EVENT_IDS is required (comma-separated)" >&2
  exit 2
fi

SCENARIO="${SCENARIO:-baseline}"
VUS="${VUS:-}" # used only when SCENARIO=custom

BASE_URL="${BASE_URL:-http://localhost:8080}"
BROKER_BASE_URL="${BROKER_BASE_URL:-}"
REDIS_URL="${REDIS_URL:-redis://127.0.0.1:6379}"

LOGIN_EMAIL_DOMAIN="${LOGIN_EMAIL_DOMAIN:-example.com}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@example.com}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-password123}"

SLOT_BOOST="${SLOT_BOOST:-0}"
SLOT_BOOST_VALUE="${SLOT_BOOST_VALUE:-10000}"
AGGRESSIVE_CLEAN="${AGGRESSIVE_CLEAN:-0}"

if [[ "$AGGRESSIVE_CLEAN" != "0" ]]; then
  echo "ERROR: Redis is not dedicated; AGGRESSIVE_CLEAN must be 0" >&2
  exit 2
fi

ROOT="$(pwd)"
TS="$(date +%Y%m%d-%H%M%S)"

BRANCH="unknown"
SHA="unknown"
if command -v git >/dev/null 2>&1; then
  BRANCH="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo unknown)"
  SHA="$(git rev-parse --short HEAD 2>/dev/null || echo unknown)"
fi

OUTDIR="$ROOT/.sisyphus/evidence/k6/$TS/$BRANCH/$SCENARIO"
mkdir -p "$OUTDIR"

export OUTDIR BRANCH SHA SCENARIO EVENT_IDS_CSV BASE_URL BROKER_BASE_URL REDIS_URL LOGIN_EMAIL_DOMAIN SLOT_BOOST SLOT_BOOST_VALUE AGGRESSIVE_CLEAN

echo "[meta] branch=$BRANCH sha=$SHA scenario=$SCENARIO" | tee "$OUTDIR/meta.txt"
echo "[meta] base_url=$BASE_URL broker_base_url=${BROKER_BASE_URL:-<default-to-base_url>} redis_url=$REDIS_URL" | tee -a "$OUTDIR/meta.txt"
echo "[meta] event_ids=$EVENT_IDS_CSV" | tee -a "$OUTDIR/meta.txt"

python3 - <<'PY'
import json, os, pathlib
outdir = pathlib.Path(os.environ['OUTDIR'])
meta = {
  'branch': os.environ.get('BRANCH','unknown'),
  'sha': os.environ.get('SHA','unknown'),
  'scenario': os.environ.get('SCENARIO',''),
  'eventIds': os.environ.get('EVENT_IDS_CSV',''),
  'baseUrl': os.environ.get('BASE_URL',''),
  'brokerBaseUrl': os.environ.get('BROKER_BASE_URL',''),
  'redisUrl': os.environ.get('REDIS_URL',''),
  'loginEmailDomain': os.environ.get('LOGIN_EMAIL_DOMAIN',''),
  'slotBoost': os.environ.get('SLOT_BOOST',''),
  'slotBoostValue': os.environ.get('SLOT_BOOST_VALUE',''),
  'aggressiveClean': os.environ.get('AGGRESSIVE_CLEAN',''),
}
(outdir / 'meta.json').write_text(json.dumps(meta, indent=2))
PY

if [[ "$SCENARIO" == "custom" ]]; then
  if [[ -z "$VUS" ]]; then
    echo "ERROR: VUS is required when SCENARIO=custom" >&2
    exit 2
  fi
fi

redis_hostport="${REDIS_URL#redis://}"
redis_host="${redis_hostport%%:*}"
redis_port="${redis_hostport##*:}"

redis_cli=(redis-cli -h "$redis_host" -p "$redis_port")

IFS=',' read -r -a event_ids <<< "$EVENT_IDS_CSV"

echo "[cleanup] starting scoped deletes" | tee "$OUTDIR/cleanup.txt"
for eid in "${event_ids[@]}"; do
  eid="$(echo "$eid" | xargs)"
  [[ -z "$eid" ]] && continue
  "${redis_cli[@]}" DEL "WAITING:$eid" "WAITING_USER_IDS:$eid" "WAITING_QUEUE_INDEX_RECORD:$eid" >>"$OUTDIR/cleanup.txt"
  "${redis_cli[@]}" HDEL "WAITING_QUEUE_IDX" "$eid" >>"$OUTDIR/cleanup.txt" || true

  if [[ "$SLOT_BOOST" == "1" ]]; then
    "${redis_cli[@]}" HSET "ENTRY_QUEUE_SLOTS" "$eid" "$SLOT_BOOST_VALUE" >>"$OUTDIR/cleanup.txt"
  else
    "${redis_cli[@]}" HDEL "ENTRY_QUEUE_SLOTS" "$eid" >>"$OUTDIR/cleanup.txt" || true
  fi
done

if [[ "$AGGRESSIVE_CLEAN" == "1" ]]; then
  echo "[cleanup] aggressive clean enabled" | tee -a "$OUTDIR/cleanup.txt"
  "${redis_cli[@]}" DEL "ENTRY" >>"$OUTDIR/cleanup.txt" || true
  while read -r k; do
    [[ -z "$k" ]] && continue
    "${redis_cli[@]}" DEL "$k" >>"$OUTDIR/cleanup.txt" || true
  done < <("${redis_cli[@]}" --scan --pattern 'DISPATCH:*')
fi

HARNESS="$ROOT/.sisyphus/bench/sse-throughput-test.js"
if [[ ! -f "$HARNESS" ]]; then
  echo "ERROR: harness not found: $HARNESS" >&2
  exit 2
fi

START_MS="$(python3 - <<'PY'
import time
print(int(time.time()*1000))
PY
)"

echo "[k6] starting" | tee "$OUTDIR/stdout.txt"

k6_env=(
  -e "BASE_URL=$BASE_URL"
  -e "REDIS_URL=$REDIS_URL"
  -e "EVENT_IDS=$EVENT_IDS_CSV"
  -e "LOGIN_EMAIL_DOMAIN=$LOGIN_EMAIL_DOMAIN"
  -e "ADMIN_EMAIL=$ADMIN_EMAIL"
  -e "ADMIN_PASSWORD=$ADMIN_PASSWORD"
  -e "SCENARIO=$SCENARIO"
)

if [[ -n "$BROKER_BASE_URL" ]]; then
  k6_env+=( -e "BROKER_BASE_URL=$BROKER_BASE_URL" )
fi

if [[ "$SCENARIO" == "custom" ]]; then
  k6_env+=( -e "VUS=$VUS" )
fi

set +e
k6 run "${k6_env[@]}" "$HARNESS" 2>&1 | tee -a "$OUTDIR/stdout.txt"
k6_ec=${PIPESTATUS[0]}
set -e

END_MS="$(python3 - <<'PY'
import time
print(int(time.time()*1000))
PY
)"

echo "[k6] exit_code=$k6_ec" | tee -a "$OUTDIR/meta.txt"
echo "[meta] wall_clock_ms=$((END_MS-START_MS))" | tee -a "$OUTDIR/meta.txt"

if [[ $k6_ec -ne 0 ]]; then
  echo "ERROR: k6 failed (exit=$k6_ec)" >&2
  exit $k6_ec
fi

# k6 handleSummary writes summary.json to CWD; prefer to copy from working dir if present.
if [[ -f "$ROOT/summary.json" ]]; then
  cp "$ROOT/summary.json" "$OUTDIR/summary.json"
else
  echo "ERROR: summary.json not found at repo root after k6 run" >&2
  exit 2
fi

python3 - <<'PY'
import json, sys, pathlib
outdir = pathlib.Path(sys.argv[1])
data = json.loads((outdir / 'summary.json').read_text())

def get(path, default=None):
    cur = data
    for p in path:
        if isinstance(cur, dict) and p in cur:
            cur = cur[p]
        else:
            return default
    return cur

derived = {
  'testRunDurationMs': get(['state','testRunDurationMs']),
  'completedUsers': get(['metrics','completed_users','values','count']),
  'barrierWaitMaxMs': get(['metrics','barrier_wait_ms','values','max']),
  'httpReqFailedRate': get(['metrics','http_req_failed','values','rate']),
  'loginFinalFailures': get(['metrics','login_final_failures','values','count']),
  'sseFailures': get(['metrics','sse_failures','values','count']),
}

(outdir / 'derived.json').write_text(json.dumps(derived, indent=2))
print(json.dumps(derived, indent=2))
PY
"$OUTDIR"

echo "OK: $OUTDIR" | tee -a "$OUTDIR/meta.txt"
```

---

## Decisions Needed

Resolved:
- Redis is NOT dedicated: scoped deletes only (`AGGRESSIVE_CLEAN=0`) and script enforces it.
- Admin credentials: `ADMIN_EMAIL=admin@example.com`, `ADMIN_PASSWORD=password123`
- SSE path: via gateway (`BASE_URL=http://localhost:8080`, `BROKER_BASE_URL` unset)
