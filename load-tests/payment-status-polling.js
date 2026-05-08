import http from 'k6/http';
import { check, fail, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { SharedArray } from 'k6/data';
import exec from 'k6/execution';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const DATA_FILE = __ENV.PAYMENT_DATA_FILE || './payment-test-data.json';
const VUS = parseInt(__ENV.VUS || '100', 10);
const RATE = parseInt(__ENV.RATE || String(VUS), 10);
const DURATION = __ENV.DURATION || '5m';
const TIMEOUT = __ENV.STATUS_TIMEOUT || '3s';

const statusLatency = new Trend('status_api_latency_ms', true);
const statusRequests = new Counter('status_api_requests');
const statusErrors = new Rate('status_api_error_rate');
const doneSeen = new Counter('status_done_seen');

export const options = {
  scenarios: {
    polling: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: VUS,
      maxVUs: Math.max(VUS, RATE * 2),
      exec: 'poll',
      tags: { flow: 'status-polling' },
    },
  },
  thresholds: {
    'http_req_failed{endpoint:confirm_status}': ['rate<0.05'],
    status_api_error_rate: ['rate<0.05'],
    status_api_latency_ms: ['p(95)<1000'],
  },
};

const rows = new SharedArray('payment-status-rows', () => {
  if (__ENV.PAYMENT_DATA_JSON) return JSON.parse(__ENV.PAYMENT_DATA_JSON).payments;
  const parsed = JSON.parse(open(DATA_FILE));
  return parsed.payments || parsed;
});

export function poll() {
  const row = rows[exec.scenario.iterationInTest % rows.length];
  const started = Date.now();
  const res = http.get(`${BASE_URL}/api/v1/payments/confirm/${encodeURIComponent(must(row.purchaseId, 'purchaseId'))}/status`, {
    headers: authHeaders(row),
    cookies: row.refreshToken ? { refreshToken: row.refreshToken } : {},
    timeout: TIMEOUT,
    tags: { endpoint: 'confirm_status' },
  });
  statusLatency.add(Date.now() - started);
  statusRequests.add(1);
  const ok = check(res, { 'status 200': (r) => r.status === 200 });
  statusErrors.add(!ok);
  if (ok) {
    const data = JSON.parse(res.body || '{}').data || {};
    if (data.status === 'DONE' || data.paymentStatus === 'DONE') doneSeen.add(1);
  } else if ((__ENV.FAIL_FAST || '0') === '1') {
    fail(`status failed status=${res.status} body=${String(res.body).slice(0, 300)}`);
  }
  sleep(parseInt(__ENV.CLIENT_THINK_MS || '0', 10) / 1000);
}

function authHeaders(row) {
  const headers = {
    Authorization: `Bearer ${normalizeBearer(must(row.accessToken, 'accessToken'))}`,
    'Content-Type': 'application/json',
  };
  if (row.userId) headers['User-Id'] = row.userId;
  if (row.role || row.userRole) headers.Role = row.role || row.userRole;
  if (row.email) headers.Email = row.email;
  return headers;
}

function normalizeBearer(token) {
  const raw = String(token || '').trim();
  return raw.toLowerCase().startsWith('bearer ') ? raw.slice(7).trim() : raw;
}

function must(value, name) {
  if (value === undefined || value === null || value === '') fail(`${name} missing in payment test data`);
  return value;
}

export function handleSummary(data) {
  return {
    [__ENV.SUMMARY_FILE || 'payment-status-summary.json']: JSON.stringify(data, null, 2),
  };
}
