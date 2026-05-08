import http from 'k6/http';
import { check, fail, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { SharedArray } from 'k6/data';
import exec from 'k6/execution';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const DATA_FILE = __ENV.PAYMENT_DATA_FILE || './payment-test-data.json';
const EXPECTED_CONFIRM_STATUS = parseInt(__ENV.EXPECTED_CONFIRM_STATUS || '202', 10);
const VUS = parseInt(__ENV.VUS || '100', 10);
const RATE = parseInt(__ENV.RATE || String(VUS), 10);
const WARMUP = __ENV.WARMUP || '3m';
const DURATION = __ENV.DURATION || '5m';
const COOLDOWN = __ENV.COOLDOWN || '1m';
const TIMEOUT = __ENV.CONFIRM_TIMEOUT || '5s';
const INIT_PURCHASES = (__ENV.INIT_PURCHASES || '0') === '1';
const LOGIN_IN_SETUP = (__ENV.LOGIN_IN_SETUP || '0') === '1';
const DEFAULT_PASSWORD = __ENV.DEFAULT_PASSWORD || 'password123';
const PROVIDER = __ENV.PAYMENT_PROVIDER || 'TOSS';
const ALLOW_DATA_REUSE = (__ENV.ALLOW_DATA_REUSE || '0') === '1';
const MAX_VUS = Math.max(VUS, RATE * 2);
const ROW_BLOCK_SIZE = parseInt(__ENV.ROW_BLOCK_SIZE || String(Math.ceil(estimatedIterations() / MAX_VUS) + 10), 10);

const confirmLatency = new Trend('confirm_api_latency_ms', true);
const confirmThroughput = new Counter('confirm_api_requests');
const confirmErrors = new Rate('confirm_api_error_rate');
const confirmTimeouts = new Rate('confirm_api_timeout_rate');
const webOccupation = new Trend('web_request_occupation_ms', true);
const acceptedToDoneLatency = new Trend('accepted_to_done_latency_ms', true);
const e2eLatency = new Trend('end_to_end_latency_ms', true);
const completionTimeouts = new Rate('payment_completion_timeout_rate');
const statusPollRequests = new Counter('status_poll_requests');

export const options = {
  scenarios: {
    warmup: {
      executor: 'constant-arrival-rate',
      rate: Math.max(1, Math.floor(RATE / 2)),
      timeUnit: '1s',
      duration: WARMUP,
      preAllocatedVUs: VUS,
      maxVUs: MAX_VUS,
      exec: 'confirmAsync',
      tags: { phase: 'warmup', flow: 'async' },
    },
    measurement: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: VUS,
      maxVUs: MAX_VUS,
      startTime: WARMUP,
      exec: 'confirmAsync',
      tags: { phase: 'measurement', flow: 'async' },
    },
    cooldown: {
      executor: 'constant-arrival-rate',
      rate: Math.max(1, Math.floor(RATE / 4)),
      timeUnit: '1s',
      duration: COOLDOWN,
      preAllocatedVUs: Math.max(1, Math.floor(VUS / 4)),
      maxVUs: VUS,
      startTime: `${durationSeconds(WARMUP) + durationSeconds(DURATION)}s`,
      exec: 'confirmAsync',
      tags: { phase: 'cooldown', flow: 'async' },
    },
  },
  thresholds: {
    'http_req_failed{endpoint:confirm}': ['rate<0.05'],
    'confirm_api_latency_ms{phase:measurement}': ['p(95)<1000'],
    'confirm_api_error_rate{phase:measurement}': ['rate<0.05'],
    'confirm_api_timeout_rate{phase:measurement}': ['rate<0.01'],
    'payment_completion_timeout_rate{phase:measurement}': ['rate<0.10'],
  },
};

const seedRows = new SharedArray('payment-seed-rows', () => loadRows());

export function setup() {
  const rows = seedRows.map((row, i) => normalizeRow(row, i));
  verifyEnoughRows(rows.length);
  if (LOGIN_IN_SETUP) {
    for (const row of rows) login(row);
  }
  if (INIT_PURCHASES) {
    for (const row of rows) initPurchase(row);
  }
  return { rows };
}

export function confirmAsync(data) {
  const row = data.rows[rowIndex(data.rows.length)];
  const confirmStarted = Date.now();
  const payload = {
    purchaseId: must(row.purchaseId, 'purchaseId'),
    paymentKey: row.paymentKey || `k6-payment-key-${row.orderId}`,
    orderId: must(row.orderId, 'orderId'),
    amount: Number(must(row.amount, 'amount')),
    provider: row.provider || PROVIDER,
  };

  const res = http.post(`${BASE_URL}/api/v1/payments/confirm`, JSON.stringify(payload), {
    headers: authHeaders(row),
    cookies: refreshCookie(row),
    timeout: TIMEOUT,
    tags: { endpoint: 'confirm', flow: 'async' },
  });
  const acceptedAt = Date.now();
  const acceptanceMs = acceptedAt - confirmStarted;
  confirmLatency.add(acceptanceMs);
  webOccupation.add(acceptanceMs);
  confirmThroughput.add(1);
  confirmTimeouts.add(res.error_code === 1050 || String(res.error || '').toLowerCase().includes('timeout'));

  const ok = check(res, {
    [`confirm status ${EXPECTED_CONFIRM_STATUS}`]: (r) => r.status === EXPECTED_CONFIRM_STATUS,
    'accepted response has status url': (r) => String(r.body || '').includes('/status'),
  });
  confirmErrors.add(!ok);
  if (!ok) {
    if ((__ENV.FAIL_FAST || '0') === '1') fail(`confirm failed status=${res.status} body=${String(res.body).slice(0, 300)}`);
    return;
  }

  if ((__ENV.POLL_UNTIL_DONE || '1') === '1') {
    const doneAt = waitUntilDone(row, acceptedAt);
    if (doneAt > 0) {
      acceptedToDoneLatency.add(doneAt - acceptedAt);
      e2eLatency.add(doneAt - confirmStarted);
      completionTimeouts.add(false);
    } else {
      completionTimeouts.add(true);
    }
  }
}

function waitUntilDone(row, acceptedAt) {
  const deadline = acceptedAt + parseInt(__ENV.STATUS_MAX_WAIT_MS || '30000', 10);
  const intervalMs = parseInt(__ENV.STATUS_POLL_INTERVAL_MS || '500', 10);
  while (Date.now() < deadline) {
    const res = http.get(`${BASE_URL}/api/v1/payments/confirm/${encodeURIComponent(row.purchaseId)}/status`, {
      headers: authHeaders(row),
      cookies: refreshCookie(row),
      timeout: __ENV.STATUS_TIMEOUT || '3s',
      tags: { endpoint: 'confirm_status', flow: 'async' },
    });
    statusPollRequests.add(1);
    if (res.status === 200) {
      const data = parseData(res);
      if (data.status === 'DONE' || data.paymentStatus === 'DONE') return Date.now();
      if (['FAILED', 'REJECTED', 'COMPENSATION_REQUIRED'].includes(data.status)) return -1;
    }
    sleep(intervalMs / 1000);
  }
  return 0;
}

function initPurchase(row) {
  const body = {
    eventId: must(row.eventId, 'eventId'),
    orderId: must(row.orderId, 'orderId'),
    amount: Number(must(row.amount, 'amount')),
  };
  const res = http.post(`${BASE_URL}/api/v1/payments/init`, JSON.stringify(body), {
    headers: authHeaders(row),
    cookies: refreshCookie(row),
    timeout: __ENV.INIT_TIMEOUT || '10s',
    tags: { endpoint: 'init', flow: 'async' },
  });
  if (res.status !== 201) {
    fail(`init failed orderId=${row.orderId} status=${res.status} body=${String(res.body).slice(0, 300)}`);
  }
  const data = parseData(res);
  row.purchaseId = data.purchaseId;
}

function login(row) {
  if (row.accessToken && row.refreshToken) return;
  const res = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({
    email: must(row.email, 'email'),
    password: row.password || DEFAULT_PASSWORD,
  }), {
    headers: { 'Content-Type': 'application/json' },
    timeout: __ENV.LOGIN_TIMEOUT || '10s',
    tags: { endpoint: 'login', flow: 'async' },
  });
  if (res.status !== 200) fail(`login failed email=${row.email} status=${res.status}`);
  row.accessToken = normalizeBearer(res.headers.Authorization || res.headers.authorization || parseData(res));
  row.refreshToken = res.cookies.refreshToken?.[0]?.value || row.refreshToken;
}

function authHeaders(row) {
  const headers = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${normalizeBearer(must(row.accessToken, 'accessToken'))}`,
    entryAuthToken: must(row.entryAuthToken, 'entryAuthToken'),
  };
  if (row.userId) headers['User-Id'] = row.userId;
  if (row.role || row.userRole) headers.Role = row.role || row.userRole;
  if (row.email) headers.Email = row.email;
  return headers;
}

function refreshCookie(row) {
  return row.refreshToken ? { refreshToken: row.refreshToken } : {};
}

function parseData(res) {
  const body = JSON.parse(res.body || '{}');
  return body.data || body;
}

function normalizeRow(row, i) {
  return {
    provider: PROVIDER,
    amount: Number(__ENV.AMOUNT || row.amount || 50000),
    paymentKey: row.paymentKey || `k6-async-payment-key-${i}`,
    ...row,
  };
}

function loadRows() {
  if (__ENV.PAYMENT_DATA_JSON) return JSON.parse(__ENV.PAYMENT_DATA_JSON).payments;
  const parsed = JSON.parse(open(DATA_FILE));
  return parsed.payments || parsed;
}

function normalizeBearer(token) {
  const raw = String(token || '').trim();
  return raw.toLowerCase().startsWith('bearer ') ? raw.slice(7).trim() : raw;
}

function must(value, name) {
  if (value === undefined || value === null || value === '') fail(`${name} missing in payment test data`);
  return value;
}

function durationSeconds(value) {
  const m = String(value).match(/^(\d+)(ms|s|m|h)$/);
  if (!m) return 0;
  const n = Number(m[1]);
  return m[2] === 'ms' ? Math.ceil(n / 1000) : m[2] === 's' ? n : m[2] === 'm' ? n * 60 : n * 3600;
}

function rowIndex(rowCount) {
  const index = ((exec.vu.idInTest - 1) * ROW_BLOCK_SIZE) + exec.vu.iterationInInstance;
  if (index >= rowCount) {
    if (ALLOW_DATA_REUSE) return index % rowCount;
    fail(`payment test data exhausted index=${index} rows=${rowCount}; add rows or set ALLOW_DATA_REUSE=1`);
  }
  return index;
}

function verifyEnoughRows(rowCount) {
  if (ALLOW_DATA_REUSE) return;
  const required = MAX_VUS * ROW_BLOCK_SIZE;
  if (rowCount < required) {
    fail(`payment test data rows=${rowCount}, required=${required}; generate deterministic rows for full run`);
  }
}

function estimatedIterations() {
  return scenarioCount('warmup') + scenarioCount('measurement') + scenarioCount('cooldown') + MAX_VUS;
}

function scenarioCount(name) {
  if (name === 'warmup') return Math.ceil(Math.max(1, Math.floor(RATE / 2)) * durationSeconds(WARMUP));
  if (name === 'measurement') return Math.ceil(RATE * durationSeconds(DURATION));
  if (name === 'cooldown') return Math.ceil(Math.max(1, Math.floor(RATE / 4)) * durationSeconds(COOLDOWN));
  return 0;
}

export function handleSummary(data) {
  return {
    [__ENV.SUMMARY_FILE || 'payment-async-summary.json']: JSON.stringify(data, null, 2),
    stdout: textSummary(data, 'async'),
  };
}

function textSummary(data, flow) {
  const m = data.metrics;
  return [
    `flow=${flow}`,
    `accept p50=${metric(m.confirm_api_latency_ms, 'p(50)')}ms p95=${metric(m.confirm_api_latency_ms, 'p(95)')}ms p99=${metric(m.confirm_api_latency_ms, 'p(99)')}ms`,
    `confirm rps=${metric(m.confirm_api_requests, 'rate')} errors=${metric(m.confirm_api_error_rate, 'rate')} timeouts=${metric(m.confirm_api_timeout_rate, 'rate')}`,
    `completion p95=${metric(m.accepted_to_done_latency_ms, 'p(95)')}ms e2e p95=${metric(m.end_to_end_latency_ms, 'p(95)')}ms`,
  ].join('\n') + '\n';
}

function metric(obj, key) {
  return obj?.values?.[key] ?? 'n/a';
}
