import http from 'k6/http';
import redis from 'k6/x/redis';
import { check, sleep } from 'k6';
import { Counter, Gauge, Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const REDIS_URL = __ENV.REDIS_URL || 'redis://127.0.0.1:6379';
const EVENT_ID = __ENV.EVENT_ID || '';
const VUS = Number(__ENV.VUS || 5000);
const MAX_ACTIVE_SHOPPERS = Number(__ENV.MAX_ACTIVE_SHOPPERS || 500);
const PROMOTION_BATCH_SIZE = Number(__ENV.PROMOTION_BATCH_SIZE || 50);
const USER_PREFIX = __ENV.USER_PREFIX || 'queue-load-user';
const USER_PASSWORD = __ENV.USER_PASSWORD || 'password123';

export const options = {
  scenarios: {
    waiting_queue_admission: {
      executor: 'per-vu-iterations',
      vus: VUS,
      iterations: 1,
      maxDuration: '15m',
    },
  },
  thresholds: {
    active_shoppers: [`max<=${MAX_ACTIVE_SHOPPERS}`],
    promotion_rate_ok: ['rate>0.95'],
  },
};

const redisClient = new redis.Client(REDIS_URL);
const entryTokenReceived = new Counter('entry_token_received');
const activeShoppers = new Gauge('active_shoppers');
const promotionRateOk = new Rate('promotion_rate_ok');

function userEmail(vu) {
  return `${USER_PREFIX}-${vu}@example.com`;
}

function login(vu) {
  const res = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({
    email: userEmail(vu),
    password: USER_PASSWORD,
  }), {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'login' },
  });
  check(res, { 'login ok': (r) => r.status === 200 });
  const header = res.headers.Authorization || res.headers.authorization || '';
  return header.toLowerCase().startsWith('bearer ') ? header.substring(7) : header;
}

function countEntryTokens() {
  const keys = redisClient.sendCommand('KEYS', 'ENTRY_TOKEN:*') || [];
  return Array.isArray(keys) ? keys.length : 0;
}

export default function () {
  if (!EVENT_ID) {
    throw new Error('EVENT_ID is required');
  }

  const token = login(__VU);
  const enter = http.get(`${BASE_URL}/api/v1/broker/polling/events/${EVENT_ID}/waiting`, {
    headers: { Authorization: `Bearer ${token}` },
    tags: { name: 'queue_enter' },
  });
  check(enter, { 'queue enter ok': (r) => r.status === 200 });

  let lastTokenCount = 0;
  for (let i = 0; i < 180; i++) {
    const current = http.get(`${BASE_URL}/api/v1/broker/polling/events/${EVENT_ID}/current`, {
      headers: { Authorization: `Bearer ${token}` },
      tags: { name: 'queue_current' },
    });
    const body = current.json('data') || {};
    const active = countEntryTokens();
    activeShoppers.add(active);
    promotionRateOk.add(active <= MAX_ACTIVE_SHOPPERS);

    const promotedThisTick = Math.max(0, active - lastTokenCount);
    if (promotedThisTick > 0) {
      promotionRateOk.add(promotedThisTick <= PROMOTION_BATCH_SIZE);
    }
    lastTokenCount = active;

    if (body.state === 'ENTRY' && body.entryAuthToken) {
      entryTokenReceived.add(1);
      break;
    }
    sleep(Math.min(Number(body.pollAfterMs || 5000), 15000) / 1000);
  }
}
