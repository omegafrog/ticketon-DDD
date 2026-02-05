import http from 'k6/http';
import redis from 'k6/x/redis';
import {check, fail, sleep} from 'k6';
import {Counter, Trend} from 'k6/metrics';

// 환경 변수 (없으면 기본값 사용)
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const BROKER_BASE_URL = __ENV.BROKER_BASE_URL || BASE_URL;
const SCENARIO = __ENV.SCENARIO || '';
const SCENARIOS = {
    baseline: { vus: 2000, holdMs: 300000, hotRatio: 0.5 },
    target: { vus: 10000, holdMs: 600000, hotRatio: 0.6 },
    stress: { vus: 30000, holdMs: 60000, hotRatio: 0.7 },
};
const scenario = SCENARIOS[SCENARIO] || {};
const DEFAULT_VUS = parseInt(__ENV.VUS || String(scenario.vus || 1), 10);
// const DEFAULT_EVENT_ID = __ENV.EVENT_ID || '0197f92b-219e-7a57-b11f-0a356687457f';
const HOLD_MS = parseInt(__ENV.HOLD_MS || String(scenario.holdMs || 0), 10); // 연결 후 유지 시간 (ms)
const BURST_OFFSET_MS = parseInt(__ENV.BURST_OFFSET_MS || '3000', 10); // Burst start alignment offset
const ADMIN_OPEN_DELAY_MS = parseInt(__ENV.ADMIN_OPEN_DELAY_MS || '15000', 10);
const WAIT_FOR_IN_ENTRY = (__ENV.WAIT_FOR_IN_ENTRY || '1') === '1';
const LOGIN_TIMEOUT = `${parseInt(__ENV.LOGIN_TIMEOUT_MS || '10000', 10)}ms`;
const POLLING_ENTER_TIMEOUT = `${parseInt(__ENV.POLLING_ENTER_TIMEOUT_MS || '10000', 10)}ms`;
const POLLING_CURRENT_TIMEOUT = `${parseInt(__ENV.POLLING_CURRENT_TIMEOUT_MS || '5000', 10)}ms`;
const MAX_POLL_MS = parseInt(__ENV.MAX_POLL_MS || String(HOLD_MS || 120000), 10);
const MIN_POLL_AFTER_MS = Math.max(parseInt(__ENV.MIN_POLL_AFTER_MS || '200', 10), 0);
const MAX_POLL_AFTER_MS = Math.max(parseInt(__ENV.MAX_POLL_AFTER_MS || '5000', 10), 0);
const LOGIN_EMAIL_DOMAIN = __ENV.LOGIN_EMAIL_DOMAIN || 'example.com';
const ADMIN_EMAIL = __ENV.ADMIN_EMAIL || 'admin@example.com';
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD || 'password123';
// const CONTROL_EVENT_ID = __ENV.CONTROL_EVENT_ID || DEFAULT_EVENT_ID; // Use DEFAULT_EVENT_ID as fallback for control
const HOT_EVENT_ID = __ENV.HOT_EVENT_ID || '';
const HOT_RATIO = normalizeRatio(__ENV.HOT_RATIO || String(scenario.hotRatio || 0));
const EVENT_IDS = parseEventIds(__ENV.EVENT_IDS);
const VERBOSE_LOGS = (__ENV.VERBOSE_LOGS || '0') === '1';
const REDIS_URL = __ENV.REDIS_URL || 'redis://127.0.0.1:6379';
const BARRIER_TIMEOUT_MS = parseInt(__ENV.BARRIER_TIMEOUT_MS || '300000', 10);
const BARRIER_POLL_INTERVAL_MS = parseInt(__ENV.BARRIER_POLL_INTERVAL_MS || '200', 10);
const WRITE_OPEN_AT_REDIS = (__ENV.WRITE_OPEN_AT_REDIS || '1') === '1';

const DISCONNECT_TIMEOUT = `${parseInt(__ENV.DISCONNECT_TIMEOUT_MS || '2000', 10)}ms`;

// Login burst control (avoid too many simultaneous logins)
const LOGIN_BATCH_SIZE = parseInt(__ENV.LOGIN_BATCH_SIZE || '100', 10);
const LOGIN_BATCH_INTERVAL_MS = parseInt(__ENV.LOGIN_BATCH_INTERVAL_MS || '1000', 10);
const LOGIN_BATCH_JITTER_MS = parseInt(__ENV.LOGIN_BATCH_JITTER_MS || '0', 10);

// Login retry
const MAX_LOGIN_RETRIES = Math.max(parseInt(__ENV.MAX_LOGIN_RETRIES || '10', 10) || 10, 1);
const LOGIN_RETRY_DELAY_MS = Math.max(parseInt(__ENV.LOGIN_RETRY_DELAY_MS || '0', 10) || 0, 0);

const MAX_BATCH_INDEX = Math.floor(Math.max(DEFAULT_VUS - 1, 0) / Math.max(LOGIN_BATCH_SIZE, 1));
const TOTAL_START_SKEW_MS = MAX_BATCH_INDEX * Math.max(LOGIN_BATCH_INTERVAL_MS, 0) + Math.max(LOGIN_BATCH_JITTER_MS, 0);

const redisClient = new redis.Client(REDIS_URL);

export const options = {
    thresholds: {
        http_req_failed: ['rate<0.05'],
        polling_enter_time_ms: [`p(95)<${__ENV.POLLING_ENTER_P95_THRESHOLD || __ENV.SSE_P95_THRESHOLD || 5000}`],
        completed_users: [`count==${DEFAULT_VUS}`],
        barrier_wait_ms: [`max<${BARRIER_TIMEOUT_MS}`],
    },
    scenarios: {
        main_scenario: {
            executor: 'per-vu-iterations',
            vus: DEFAULT_VUS,
            iterations: 1,
            maxDuration: `${Math.max(Math.ceil((HOLD_MS + BURST_OFFSET_MS + TOTAL_START_SKEW_MS) / 1000), 60)}s`,
            gracefulStop: '30s',
            exec: 'default', // The function to execute for each iteration
        },
        admin_controller_scenario: {
            executor: 'shared-iterations',
            vus: 1,
            iterations: 1,
            startTime: '0s',
            // Safety net: never allow admin scenario to hang indefinitely.
            // Covers barrier wait + extra margin.
            maxDuration: `${Math.max(Math.ceil((BARRIER_TIMEOUT_MS + 60000) / 1000), 60)}s`,
            exec: 'adminController', // The function to execute for this scenario
        },
    },
};

const pollingEnterTimeMs = new Trend('polling_enter_time_ms');
const pollingCurrentTimeMs = new Trend('polling_current_time_ms');
const pollingPromoteTimeMs = new Trend('polling_promote_time_ms');
const loginFailures = new Counter('login_failures');
const loginFinalFailures = new Counter('login_final_failures');
const pollingFailures = new Counter('polling_failures');
const pollingTimeoutFailures = new Counter('polling_timeout_failures');
const pollingRequests = new Counter('polling_requests');
const disconnectFailures = new Counter('disconnect_failures');
const completedUsers = new Counter('completed_users');
const entryTokenReceivedCount = new Counter('entry_token_received');
const barrierWaitMs = new Trend('barrier_wait_ms');
const openToPromoteMs = new Trend('open_to_promote_ms');
const loginBatchDelayMs = new Trend('login_batch_delay_ms');

function parseEventIds(raw) {
    let ids = (raw || '')
        .split(',')
        .map((v) => v.trim())
        .filter((v) => v.length > 0);

    // Ensure uniqueness within provided IDs
    ids = [...new Set(ids)];

    return ids;
}

function normalizeBearerToken(headerValue) {
    const raw = String(headerValue || '').trim();
    if (!raw) {
        return '';
    }
    return raw.toLowerCase().startsWith('bearer ') ? raw.substring(7).trim() : raw;
}

function buildWaitingKey(eventId) {
    return `WAITING:${eventId}`;
}

function buildOpenAtKey(runId) {
    return `K6_OPEN_AT_MS:${runId}`;
}

async function getWaitingTotal() {
    let total = 0;
    for (const eventId of EVENT_IDS) {
        try {
            const count = await redisClient.sendCommand('ZCARD', buildWaitingKey(eventId));
            total += Number(count) || 0;
        } catch (_) {
            // treat missing keys/errors as 0
        }
    }
    return total;
}

async function getWaitingCountsByEvent() {
    const counts = {};
    for (const eventId of EVENT_IDS) {
        const key = buildWaitingKey(eventId);
        const count = Number(await redisClient.sendCommand('ZCARD', key)) || 0;
        counts[eventId] = count;
    }
    return counts;
}

function normalizeRatio(raw) {
    const value = parseFloat(raw);
    if (Number.isNaN(value) || value <= 0) {
        return 0;
    }
    if (value > 1) {
        return Math.min(value / 100, 1);
    }
    return Math.min(value, 1);
}

function selectEventId(userId) {
    if (!EVENT_IDS || EVENT_IDS.length === 0) {
        fail('EVENT_IDS is required (comma-separated)');
    }
    const idx = (Math.max(userId, 1) - 1) % EVENT_IDS.length;
    return EVENT_IDS[idx];
}

function updateEventStatusApi(accessToken, jar, eventId, status) {
    if (VERBOSE_LOGS) {
        console.log(`Updating event ${eventId} status to ${status}`);
    }
    // Event service status change: PATCH /api/v1/events/{eventId}?status=...
    // Run through API Gateway (BASE_URL).
    const res = http.patch(
        `${BASE_URL}/api/v1/events/${eventId}?status=${encodeURIComponent(status)}`,
        null,
        {
            headers: {
                Authorization: `Bearer ${accessToken}`,
            },
            tags: { name: 'update_event_status_api' },
            jar,
        }
    );

    check(res, {
        'update status 200': (r) => r.status === 200,
    });

    if (res.status !== 200) {
        console.error(`Failed to update event ${eventId} status to ${status}: ${res.status} - ${res.body}`);
    }
}

// Modified loginUser to accept email and password directly
function loginUser(email, password, jar) {
    // console.log(`Logging in user: ${email}`); // uncomment for debugging
    const res = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({ email, password }),
        {
            headers: { 'Content-Type': 'application/json' },
            timeout: LOGIN_TIMEOUT,
            tags: { name: 'login' },
            jar,
        },
    );

    const ok = check(res, {
        'login status 200': (r) => r.status === 200,
        'authorization header exists': (r) => !!(r.headers.Authorization || r.headers.authorization),
        'refresh token cookie exists': (r) => !!r.cookies.refreshToken?.[0]?.value,
    });

    if (!ok) {
        loginFailures.add(1);
        return null;
    }

    const accessTokenHeader = res.headers.Authorization || res.headers.authorization;
    const accessToken = normalizeBearerToken(accessTokenHeader);
    const refreshToken = res.cookies.refreshToken?.[0]?.value || '';

    if (!accessToken || !refreshToken) {
        loginFailures.add(1);
        return null;
    }

    if (VERBOSE_LOGS) {
        console.log(`Debug - refreshToken cookie: refreshToken=${refreshToken}`);
    }

    // Cross-host safety: ensure refreshToken exists for both origins.
    // (If BASE_URL === BROKER_BASE_URL, this is still safe.)
    if (jar) {
        try {
            jar.set(BASE_URL, 'refreshToken', refreshToken, { path: '/' });
            jar.set(BROKER_BASE_URL, 'refreshToken', refreshToken, { path: '/' });
        } catch (_) {
            // ignore
        }
    }

    return { accessToken, refreshToken };
}

function loginUserWithRetry(email, password, jar) {
    for (let attempt = 1; attempt <= MAX_LOGIN_RETRIES; attempt += 1) {
        const session = loginUser(email, password, jar);
        if (session) {
            return session;
        }

        if (attempt < MAX_LOGIN_RETRIES && LOGIN_RETRY_DELAY_MS > 0) {
            if (VERBOSE_LOGS) {
                console.log(
                    `[loginUserWithRetry] login failed; retrying ${attempt}/${MAX_LOGIN_RETRIES} after ${LOGIN_RETRY_DELAY_MS}ms (email=${email})`
                );
            }
            sleep(LOGIN_RETRY_DELAY_MS / 1000);
        }
    }

    loginFinalFailures.add(1);
    return null;
}
function pollingEnterWaiting(eventId, accessToken, jar) {
    const start = Date.now();
    const res = http.get(
        `${BROKER_BASE_URL}/api/v1/broker/polling/events/${eventId}/waiting`,
        {
            headers: {
                Authorization: `Bearer ${accessToken}`,
            },
            tags: { name: 'polling_enter_waiting' },
            timeout: POLLING_ENTER_TIMEOUT,
            jar,
        },
    );
    pollingEnterTimeMs.add(Date.now() - start);

    const ok = check(res, {
        'polling enter status 200': (r) => r && r.status === 200,
    });

    if (!ok) {
        pollingFailures.add(1);
        if (VERBOSE_LOGS) {
            console.log(`[pollingEnterWaiting] status=${res?.status} body=${res?.body}`);
        }
        return false;
    }

    return true;
}

function pollingFetchCurrent(eventId, accessToken, jar) {
    const start = Date.now();
    const res = http.get(
        `${BROKER_BASE_URL}/api/v1/broker/polling/events/${eventId}/current`,
        {
            headers: {
                Authorization: `Bearer ${accessToken}`,
            },
            tags: { name: 'polling_current' },
            timeout: POLLING_CURRENT_TIMEOUT,
            jar,
        },
    );
    pollingCurrentTimeMs.add(Date.now() - start);
    pollingRequests.add(1);

    if (!res || res.status !== 200) {
        pollingFailures.add(1);
        if (VERBOSE_LOGS) {
            console.log(`[pollingFetchCurrent] status=${res?.status} body=${res?.body}`);
        }
        return null;
    }

    let parsed;
    try {
        parsed = res.json();
    } catch (_) {
        pollingFailures.add(1);
        if (VERBOSE_LOGS) {
            console.log(`[pollingFetchCurrent] invalid json: ${res.body}`);
        }
        return null;
    }

    const data = parsed && parsed.data ? parsed.data : null;
    if (!data) {
        pollingFailures.add(1);
        return null;
    }

    return {
        state: String(data.state || ''),
        rank: data.rank,
        entryAuthToken: data.entryAuthToken,
        pollAfterMs: Number(data.pollAfterMs) || 0,
    };
}

function clampPollAfterMs(rawMs) {
    let ms = Number(rawMs);
    if (!Number.isFinite(ms) || ms < 0) {
        ms = 0;
    }
    if (MAX_POLL_AFTER_MS > 0) {
        ms = Math.min(ms, MAX_POLL_AFTER_MS);
    }
    ms = Math.max(ms, MIN_POLL_AFTER_MS);
    return ms;
}

function pollingWaitForEntry(eventId, accessToken, jar, runId) {
    const start = Date.now();

    while (true) {
        if (Date.now() - start > MAX_POLL_MS) {
            pollingTimeoutFailures.add(1);
            return null;
        }

        const current = pollingFetchCurrent(eventId, accessToken, jar);
        if (current && current.state === 'ENTRY' && current.entryAuthToken) {
            entryTokenReceivedCount.add(1);
            pollingPromoteTimeMs.add(Date.now() - start);
            completedUsers.add(1);

            // Promotion timing (requires adminController to write open timestamp to Redis)
            // NOTE: xk6-redis access is async in this script; keep disabled by default.
            // if (WRITE_OPEN_AT_REDIS && runId) { ... }

            return String(current.entryAuthToken);
        }

        const sleepMs = clampPollAfterMs(current ? current.pollAfterMs : 1000);
        sleep(sleepMs / 1000);
    }
}

function pollingDisconnectWaiting(eventId, accessToken, jar) {
    const res = http.del(
        `${BROKER_BASE_URL}/api/v1/broker/polling/events/${eventId}/waiting`,
        null,
        {
            headers: {
                Authorization: `Bearer ${accessToken}`,
            },
            tags: { name: 'polling_disconnect_waiting' },
            timeout: DISCONNECT_TIMEOUT,
            jar,
        },
    );

    const ok = check(res, {
        'polling disconnect status 200': (r) => r && r.status === 200,
    });

    if (!ok) {
        disconnectFailures.add(1);
        if (VERBOSE_LOGS) {
            console.log(`[pollingDisconnectWaiting] status=${res?.status} body=${res?.body}`);
        }
    }
}

export async function setup() {
    console.log('--- K6 Setup: Initializing event statuses to CLOSED ---');
    if (!EVENT_IDS || EVENT_IDS.length === 0) {
        fail('EVENT_IDS is required (comma-separated)');
    }

    // Precondition: waiting queues must be empty
    for (const eventId of EVENT_IDS) {
        const waitingKey = buildWaitingKey(eventId);
        const userIdsKey = `WAITING_USER_IDS:${eventId}`;
        let waitingCount;
        let userIdsCount;
        try {
            waitingCount = Number(await redisClient.sendCommand('ZCARD', waitingKey)) || 0;
            userIdsCount = Number(await redisClient.hlen(userIdsKey)) || 0;
        } catch (e) {
            fail(`Redis precondition check failed: ${e}`);
        }
        if (waitingCount > 0 || userIdsCount > 0) {
            fail(
                `Precondition failed: leftover queue state for eventId=${eventId} ` +
                `(HLEN ${waitingKey}=${waitingCount}, HLEN ${userIdsKey}=${userIdsCount})`
            );
        }
    }

    const adminJar = http.cookieJar();
    const adminSession = loginUserWithRetry(ADMIN_EMAIL, ADMIN_PASSWORD, adminJar);
    if (!adminSession) {
        fail('Admin login failed during setup. Cannot proceed with event status control.');
    }
    if (VERBOSE_LOGS) {
        console.log(EVENT_IDS);
    }
    EVENT_IDS.forEach(eventId => {
        // Admin jar retains cookies (refreshToken + any stickiness)
        updateEventStatusApi(adminSession.accessToken, adminJar, eventId, 'CLOSED');
        sleep(0.5); // Add a small delay between updates to avoid flooding the API
    });
    console.log('--- K6 Setup: Event statuses set to CLOSED ---');
    const burstStartMs = Date.now() + Math.max(BURST_OFFSET_MS, 0);

    const runId = `${Date.now()}-${Math.floor(Math.random() * 1e9)}`;
    return {
        burstStartMs,
        runId,
    };
}

export function teardown(data) {
    console.log('--- K6 Teardown: Resetting event statuses to OPEN ---');
    const adminJar = http.cookieJar();
    const adminSession = loginUserWithRetry(ADMIN_EMAIL, ADMIN_PASSWORD, adminJar);
    if (!adminSession) {
        console.error('Admin login failed during teardown. Cannot reset event statuses.');
        return;
    }

    EVENT_IDS.forEach(eventId => {
        updateEventStatusApi(adminSession.accessToken, adminJar, eventId, 'OPEN');
        sleep(0.5); // Add a small delay between updates
    });
    console.log('--- K6 Teardown: Event statuses reset to OPEN ---');
}

export async function adminController(data) { // Explicitly exported
    console.log('--- Admin Controller: Waiting until all VUs entered WAITING:* in Redis ---');
    const adminJar = http.cookieJar();
    const adminSession = loginUserWithRetry(ADMIN_EMAIL, ADMIN_PASSWORD, adminJar);
    const adminAccessToken = adminSession?.accessToken;
    const runId = data?.runId;

    if (!adminAccessToken) {
        fail('Admin login failed in adminController');
    }

    const expected = DEFAULT_VUS;
    const start = Date.now();
    while (true) {
        const total = await getWaitingTotal();
        if (Number(total) >= expected) {
            break;
        }
        if (Date.now() - start > BARRIER_TIMEOUT_MS) {
            console.log('hi')
            const counts = await getWaitingCountsByEvent();
            fail(`Barrier timeout: totalWaiting=${total}, expected=${expected}, byEvent=${JSON.stringify(counts)}`);
        }
        sleep(Math.max(BARRIER_POLL_INTERVAL_MS, 50) / 1000);
    }
    const waited = Date.now() - start;
    barrierWaitMs.add(waited);

    const openAt = Date.now();
    if (WRITE_OPEN_AT_REDIS && runId) {
        try {
            await redisClient.set(buildOpenAtKey(runId), String(openAt));
        } catch (_) {
            // ignore
        }
    }

    console.log(`--- Admin Controller: Barrier met in ${waited}ms. Setting events to OPEN ---`);
    for (const eventId of EVENT_IDS) {
        updateEventStatusApi(adminAccessToken, adminJar, eventId, 'OPEN');
        sleep(0.05);
    }
    console.log('--- Admin Controller: All events set to OPEN ---');

    // IMPORTANT:
    // - Do not wait here. If this scenario doesn't return, the whole k6 run won't exit.
    // - Do not call teardown() manually; k6 will call teardown once at the end.
    return;
}

export default function (data) {
    const userId = __VU; // VU 번호로 사용자 식별
    const eventId = selectEventId(userId);
    const burstStartMs = data?.burstStartMs || Date.now();

    const jar = http.cookieJar();

    const batchSize = Math.max(LOGIN_BATCH_SIZE, 1);
    const batchIndex = Math.floor((Math.max(userId, 1) - 1) / batchSize);

    // Deterministic jitter (avoid using Math.random() for reproducibility)
    let jitter = 0;
    if (LOGIN_BATCH_JITTER_MS > 0) {
        jitter = (userId * 1103515245 + 12345) % LOGIN_BATCH_JITTER_MS;
    }

    const scheduledStartMs =
        burstStartMs +
        (batchIndex * Math.max(LOGIN_BATCH_INTERVAL_MS, 0)) +
        jitter;

    const waitMs = Math.max(scheduledStartMs - Date.now(), 0);
    if (waitMs > 0) {
        loginBatchDelayMs.add(waitMs);
        sleep(waitMs / 1000);
    }

    // Original call to loginUser needs to be updated for regular users
    const email = `user${userId}@${LOGIN_EMAIL_DOMAIN}`;
    const password = 'password123'; // Assuming a default password for generated users
    const session = loginUserWithRetry(email, password, jar);
    if (!session) {
        fail('login failed');
    }

    const entered = pollingEnterWaiting(eventId, session.accessToken, jar);
    if (!entered) {
        fail('polling enter failed');
    }

    try {
        if (WAIT_FOR_IN_ENTRY) {
            const token = pollingWaitForEntry(eventId, session.accessToken, jar, data?.runId);
            if (!token) {
                fail('polling current timeout');
            }
        } else {
            // For scenarios that only measure join throughput.
            completedUsers.add(1);
        }
    } finally {
        pollingDisconnectWaiting(eventId, session.accessToken, jar);
    }
}


export function handleSummary(data) {
    const completed = data.metrics.completed_users?.values?.count || 0;
    const entryTokens = data.metrics.entry_token_received?.values?.count || 0;
    const loginFailuresCount = data.metrics.login_failures?.values?.count || 0;
    const loginFinalFailuresCount = data.metrics.login_final_failures?.values?.count || 0;
    const pollingFailuresCount = data.metrics.polling_failures?.values?.count || 0;
    const pollingTimeoutFailuresCount = data.metrics.polling_timeout_failures?.values?.count || 0;
    const disconnectFailuresCount = data.metrics.disconnect_failures?.values?.count || 0;
    const barrierMax = data.metrics.barrier_wait_ms?.values?.max;
    const openToPromoteP95 = data.metrics.open_to_promote_ms?.values?.['p(95)'];
    const loginBatchDelayP95 = data.metrics.login_batch_delay_ms?.values?.['p(95)'];

    const total = DEFAULT_VUS;
    const okRate = total > 0 ? completed / total : 0;
    const summary = [
        'Polling Throughput Test Summary',
        `BASE_URL: ${BASE_URL}`,
        `Scenario: ${SCENARIO || 'custom'}`,
        `Event IDs: ${EVENT_IDS.length} (hotRatio=${(HOT_RATIO * 100).toFixed(0)}%)`,
        `Users (vus/iterations): ${DEFAULT_VUS}`,
        `Success rate: ${(okRate * 100).toFixed(2)}%`,
        `P95 enter time: ${data.metrics.polling_enter_time_ms?.values['p(95)']?.toFixed(2) || 'n/a'} ms`,
        `P95 current time: ${data.metrics.polling_current_time_ms?.values['p(95)']?.toFixed(2) || 'n/a'} ms`,
        `P95 promote time: ${data.metrics.polling_promote_time_ms?.values['p(95)']?.toFixed(2) || 'n/a'} ms`,
        `Completed users: ${completed}`,
        `Entry tokens: ${entryTokens}`,
        `Login failures: ${loginFailuresCount}`,
        `Login final failures: ${loginFinalFailuresCount}`,
        `Polling failures: ${pollingFailuresCount}`,
        `Polling timeout failures: ${pollingTimeoutFailuresCount}`,
        `Disconnect failures: ${disconnectFailuresCount}`,
        `Barrier wait max: ${barrierMax != null ? barrierMax.toFixed(0) : 'n/a'} ms`,
        `P95 OPEN->ENTRY: ${openToPromoteP95 != null ? openToPromoteP95.toFixed(2) : 'n/a'} ms`,
        `P95 login batch delay: ${loginBatchDelayP95 != null ? loginBatchDelayP95.toFixed(0) : 'n/a'} ms`,
    ];
    const throughputSummary = summary.join('\n');

    return {
        'stdout': throughputSummary,
        'summary.json': JSON.stringify(data, null, 2), // 모든 메트릭 데이터를 JSON 파일로 저장
    };
}
