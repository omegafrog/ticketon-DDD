import http from 'k6/http';
import { check, fail, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const MODE = String(__ENV.MODE || 'queue').trim().toLowerCase();
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const BROKER_BASE_URL = __ENV.BROKER_BASE_URL || BASE_URL;
const SUMMARY_FILE = __ENV.SUMMARY_FILE || `queue-impact-${MODE}-summary.json`;

const VUS = parseInt(__ENV.VUS || '1000', 10);
const EVENT_IDS = parseEventIds(__ENV.EVENT_IDS);
const LOGIN_EMAIL_DOMAIN = __ENV.LOGIN_EMAIL_DOMAIN || 'example.com';
const USER_EMAIL_PREFIX = __ENV.USER_EMAIL_PREFIX || 'user';
const DEFAULT_PASSWORD = __ENV.DEFAULT_PASSWORD || 'password123!';
const VERBOSE_LOGS = (__ENV.VERBOSE_LOGS || '0') === '1';

const LOGIN_TIMEOUT = `${parseInt(__ENV.LOGIN_TIMEOUT_MS || '10000', 10)}ms`;
const TARGET_TIMEOUT = `${parseInt(__ENV.TARGET_TIMEOUT_MS || '10000', 10)}ms`;
const POLLING_ENTER_TIMEOUT = `${parseInt(__ENV.POLLING_ENTER_TIMEOUT_MS || '10000', 10)}ms`;
const POLLING_CURRENT_TIMEOUT = `${parseInt(__ENV.POLLING_CURRENT_TIMEOUT_MS || '5000', 10)}ms`;
const DISCONNECT_TIMEOUT = `${parseInt(__ENV.DISCONNECT_TIMEOUT_MS || '2000', 10)}ms`;
const MAX_POLL_MS = parseInt(__ENV.MAX_POLL_MS || '180000', 10);
const MIN_POLL_AFTER_MS = Math.max(parseInt(__ENV.MIN_POLL_AFTER_MS || '200', 10), 0);
const MAX_POLL_AFTER_MS = Math.max(parseInt(__ENV.MAX_POLL_AFTER_MS || '5000', 10), 0);

const BURST_OFFSET_MS = parseInt(__ENV.BURST_OFFSET_MS || '3000', 10);

const LOGIN_BATCH_SIZE = parseInt(__ENV.LOGIN_BATCH_SIZE || '100', 10);
const LOGIN_BATCH_INTERVAL_MS = parseInt(__ENV.LOGIN_BATCH_INTERVAL_MS || '1000', 10);
const LOGIN_BATCH_JITTER_MS = parseInt(__ENV.LOGIN_BATCH_JITTER_MS || '0', 10);
const MAX_LOGIN_RETRIES = Math.max(parseInt(__ENV.MAX_LOGIN_RETRIES || '10', 10) || 10, 1);
const LOGIN_RETRY_DELAY_MS = Math.max(parseInt(__ENV.LOGIN_RETRY_DELAY_MS || '0', 10) || 0, 0);

const CALL_TARGET = (__ENV.CALL_TARGET || '1') === '1';
const TARGET_METHOD = String(__ENV.TARGET_METHOD || 'GET').trim().toUpperCase();
const TARGET_PATH_TEMPLATE = __ENV.TARGET_PATH_TEMPLATE || '/api/v1/events/{eventId}/seats';
const TARGET_BODY_TEMPLATE = __ENV.TARGET_BODY_TEMPLATE || '';
const TARGET_CONTENT_TYPE = __ENV.TARGET_CONTENT_TYPE || 'application/json';
const TARGET_REQUIRES_ENTRY_TOKEN = (__ENV.TARGET_REQUIRES_ENTRY_TOKEN || '0') === '1';

if (!['queue', 'direct'].includes(MODE)) {
    fail(`Unsupported MODE=${MODE}. Use queue or direct.`);
}

if (EVENT_IDS.length === 0) {
    fail('EVENT_IDS is required (comma-separated)');
}

if (!['GET', 'POST', 'PUT', 'PATCH', 'DELETE'].includes(TARGET_METHOD)) {
    fail(`Unsupported TARGET_METHOD=${TARGET_METHOD}`);
}

const MAX_BATCH_INDEX = Math.floor(Math.max(VUS - 1, 0) / Math.max(LOGIN_BATCH_SIZE, 1));
const TOTAL_START_SKEW_MS =
    MAX_BATCH_INDEX * Math.max(LOGIN_BATCH_INTERVAL_MS, 0) +
    Math.max(LOGIN_BATCH_JITTER_MS, 0);

export const options = {
    thresholds: {
        http_req_failed: ['rate<0.05'],
        completed_users: [`count==${VUS}`],
        target_success_rate: ['rate>0.95'],
    },
    scenarios: buildScenarios(),
};

const loginFailures = new Counter('login_failures');
const loginFinalFailures = new Counter('login_final_failures');
const pollingFailures = new Counter('polling_failures');
const pollingTimeoutFailures = new Counter('polling_timeout_failures');
const pollingRequests = new Counter('polling_requests');
const targetRequests = new Counter('target_requests');
const completedUsers = new Counter('completed_users');
const queuePromotions = new Counter('queue_promotions');
const loginBatchDelayMs = new Trend('login_batch_delay_ms');
const pollingEnterTimeMs = new Trend('polling_enter_time_ms');
const pollingCurrentTimeMs = new Trend('polling_current_time_ms');
const queueWaitTimeMs = new Trend('queue_wait_time_ms');
const targetReqTimeMs = new Trend('target_req_time_ms');
const timeToTargetMs = new Trend('time_to_target_ms');
const targetSuccessRate = new Rate('target_success_rate');

function buildScenarios() {
    const maxDurationSeconds = Math.max(
        Math.ceil((MAX_POLL_MS + BURST_OFFSET_MS + TOTAL_START_SKEW_MS) / 1000),
        60,
    );

    return {
        main_scenario: {
            executor: 'per-vu-iterations',
            vus: VUS,
            iterations: 1,
            maxDuration: `${maxDurationSeconds}s`,
            gracefulStop: '30s',
            exec: 'default',
        },
    };
}

function parseEventIds(raw) {
    return [...new Set(
        String(raw || '')
            .split(',')
            .map((value) => value.trim())
            .filter((value) => value.length > 0),
    )];
}

function normalizeBearerToken(headerValue) {
    const raw = String(headerValue || '').trim();
    if (!raw) {
        return '';
    }
    return raw.toLowerCase().startsWith('bearer ') ? raw.substring(7).trim() : raw;
}

function selectEventId(vuNumber) {
    const index = (Math.max(vuNumber, 1) - 1) % EVENT_IDS.length;
    return EVENT_IDS[index];
}

function templateValue(raw, variables) {
    return String(raw).replace(/\{([a-zA-Z0-9_]+)\}/g, (_, key) => {
        const value = variables[key];
        return value == null ? '' : String(value);
    });
}

function loginUser(email, password, jar) {
    const response = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({ email, password }),
        {
            headers: { 'Content-Type': 'application/json' },
            timeout: LOGIN_TIMEOUT,
            tags: { name: 'login' },
            jar,
        },
    );

    const ok = check(response, {
        'login status 200': (res) => res.status === 200,
        'authorization header exists': (res) => !!(res.headers.Authorization || res.headers.authorization),
        'refresh token cookie exists': (res) => !!res.cookies.refreshToken?.[0]?.value,
    });

    if (!ok) {
        loginFailures.add(1);
        return null;
    }

    const accessTokenHeader = response.headers.Authorization || response.headers.authorization;
    const accessToken = normalizeBearerToken(accessTokenHeader);
    const refreshToken = response.cookies.refreshToken?.[0]?.value || '';

    if (!accessToken || !refreshToken) {
        loginFailures.add(1);
        return null;
    }

    if (jar) {
        try {
            jar.set(BASE_URL, 'refreshToken', refreshToken, { path: '/' });
            jar.set(BROKER_BASE_URL, 'refreshToken', refreshToken, { path: '/' });
        } catch (_) {
            // Keep the test running even if the cookie jar rejects cross-host writes.
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
                console.log(`[login retry] email=${email} attempt=${attempt}`);
            }
            sleep(LOGIN_RETRY_DELAY_MS / 1000);
        }
    }

    loginFinalFailures.add(1);
    return null;
}

function pollingEnterWaiting(eventId, accessToken, jar) {
    const start = Date.now();
    const response = http.get(
        `${BROKER_BASE_URL}/api/v1/broker/polling/events/${eventId}/waiting`,
        {
            headers: {
                Authorization: `Bearer ${accessToken}`,
            },
            timeout: POLLING_ENTER_TIMEOUT,
            tags: { name: 'polling_enter_waiting' },
            jar,
        },
    );
    pollingEnterTimeMs.add(Date.now() - start);

    const ok = check(response, {
        'polling enter status 200': (res) => res.status === 200,
    });

    if (!ok) {
        pollingFailures.add(1);
        return false;
    }

    return true;
}

function pollingFetchCurrent(eventId, accessToken, jar) {
    const start = Date.now();
    const response = http.get(
        `${BROKER_BASE_URL}/api/v1/broker/polling/events/${eventId}/current`,
        {
            headers: {
                Authorization: `Bearer ${accessToken}`,
            },
            timeout: POLLING_CURRENT_TIMEOUT,
            tags: { name: 'polling_current' },
            jar,
        },
    );

    pollingCurrentTimeMs.add(Date.now() - start);
    pollingRequests.add(1);

    if (response.status !== 200) {
        pollingFailures.add(1);
        return null;
    }

    let parsed;
    try {
        parsed = response.json();
    } catch (_) {
        pollingFailures.add(1);
        return null;
    }

    const data = parsed?.data;
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
    let pollMs = Number(rawMs);
    if (!Number.isFinite(pollMs) || pollMs < 0) {
        pollMs = 0;
    }
    if (MAX_POLL_AFTER_MS > 0) {
        pollMs = Math.min(pollMs, MAX_POLL_AFTER_MS);
    }
    return Math.max(pollMs, MIN_POLL_AFTER_MS);
}

function pollingWaitForEntry(eventId, accessToken, jar) {
    const start = Date.now();

    while (true) {
        if (Date.now() - start > MAX_POLL_MS) {
            pollingTimeoutFailures.add(1);
            return null;
        }

        const current = pollingFetchCurrent(eventId, accessToken, jar);
        if (current?.state === 'ENTRY' && current.entryAuthToken) {
            const durationMs = Date.now() - start;
            queuePromotions.add(1);
            queueWaitTimeMs.add(durationMs);
            return {
                entryAuthToken: String(current.entryAuthToken),
                waitMs: durationMs,
            };
        }

        sleep(clampPollAfterMs(current?.pollAfterMs ?? 1000) / 1000);
    }
}

function pollingDisconnectWaiting(eventId, accessToken, jar) {
    const response = http.del(
        `${BROKER_BASE_URL}/api/v1/broker/polling/events/${eventId}/waiting`,
        null,
        {
            headers: {
                Authorization: `Bearer ${accessToken}`,
            },
            timeout: DISCONNECT_TIMEOUT,
            tags: { name: 'polling_disconnect_waiting' },
            jar,
        },
    );

    check(response, {
        'polling disconnect status 200': (res) => res.status === 200,
    });
}

function callTarget(eventId, accessToken, entryAuthToken, jar, variables) {
    const url = `${BASE_URL}${templateValue(TARGET_PATH_TEMPLATE, variables)}`;
    const headers = {
        Authorization: `Bearer ${accessToken}`,
    };

    if (TARGET_REQUIRES_ENTRY_TOKEN && entryAuthToken) {
        headers.entryAuthToken = entryAuthToken;
    }

    let body = null;
    if (TARGET_BODY_TEMPLATE) {
        body = templateValue(TARGET_BODY_TEMPLATE, variables);
        headers['Content-Type'] = TARGET_CONTENT_TYPE;
    }

    const params = {
        headers,
        timeout: TARGET_TIMEOUT,
        tags: {
            name: 'target_request',
            comparison_mode: MODE,
            target_method: TARGET_METHOD,
        },
        jar,
    };

    const start = Date.now();
    let response;
    switch (TARGET_METHOD) {
        case 'GET':
            response = http.get(url, params);
            break;
        case 'POST':
            response = http.post(url, body, params);
            break;
        case 'PUT':
            response = http.put(url, body, params);
            break;
        case 'PATCH':
            response = http.patch(url, body, params);
            break;
        case 'DELETE':
            response = http.del(url, body, params);
            break;
        default:
            fail(`Unsupported TARGET_METHOD=${TARGET_METHOD}`);
    }

    targetRequests.add(1);
    targetReqTimeMs.add(Date.now() - start);

    const success = response.status >= 200 && response.status < 300;
    targetSuccessRate.add(success);

    check(response, {
        'target request status 2xx': (res) => res.status >= 200 && res.status < 300,
    });

    return success;
}

export function setup() {
    return {
        burstStartMs: Date.now() + Math.max(BURST_OFFSET_MS, 0),
    };
}

export default function (data) {
    const vuNumber = __VU;
    const eventId = selectEventId(vuNumber);
    const jar = http.cookieJar();
    const batchSize = Math.max(LOGIN_BATCH_SIZE, 1);
    const batchIndex = Math.floor((Math.max(vuNumber, 1) - 1) / batchSize);

    let jitter = 0;
    if (LOGIN_BATCH_JITTER_MS > 0) {
        jitter = (vuNumber * 1103515245 + 12345) % LOGIN_BATCH_JITTER_MS;
    }

    const scheduledStartMs =
        (data?.burstStartMs || Date.now()) +
        (batchIndex * Math.max(LOGIN_BATCH_INTERVAL_MS, 0)) +
        jitter;

    const waitMs = Math.max(scheduledStartMs - Date.now(), 0);
    if (waitMs > 0) {
        loginBatchDelayMs.add(waitMs);
        sleep(waitMs / 1000);
    }

    const email = `${USER_EMAIL_PREFIX}${vuNumber}@${LOGIN_EMAIL_DOMAIN}`;
    const session = loginUserWithRetry(email, DEFAULT_PASSWORD, jar);
    if (!session) {
        fail(`login failed for ${email}`);
    }

    const startedAt = Date.now();
    const variables = {
        eventId,
        mode: MODE,
        userNumber: vuNumber,
        vu: vuNumber,
    };

    if (MODE === 'direct') {
        if (CALL_TARGET) {
            const success = callTarget(eventId, session.accessToken, '', jar, variables);
            timeToTargetMs.add(Date.now() - startedAt);
            if (!success) {
                fail(`target request failed in direct mode for eventId=${eventId}`);
            }
        }
        completedUsers.add(1);
        return;
    }

    const entered = pollingEnterWaiting(eventId, session.accessToken, jar);
    if (!entered) {
        fail(`polling enter failed for eventId=${eventId}`);
    }

    let entryAuthToken = '';
    try {
        const promoted = pollingWaitForEntry(eventId, session.accessToken, jar);
        if (!promoted?.entryAuthToken) {
            fail(`polling current timeout for eventId=${eventId}`);
        }

        entryAuthToken = promoted.entryAuthToken;

        if (CALL_TARGET) {
            const success = callTarget(eventId, session.accessToken, entryAuthToken, jar, variables);
            timeToTargetMs.add(Date.now() - startedAt);
            if (!success) {
                fail(`target request failed in queue mode for eventId=${eventId}`);
            }
        }

        completedUsers.add(1);
    } finally {
        pollingDisconnectWaiting(eventId, session.accessToken, jar);
    }
}

export function handleSummary(data) {
    const completed = data.metrics.completed_users?.values?.count || 0;
    const targetCount = data.metrics.target_requests?.values?.count || 0;
    const loginFailureCount = data.metrics.login_failures?.values?.count || 0;
    const loginFinalFailureCount = data.metrics.login_final_failures?.values?.count || 0;
    const pollingFailureCount = data.metrics.polling_failures?.values?.count || 0;
    const pollingTimeoutFailureCount = data.metrics.polling_timeout_failures?.values?.count || 0;
    const queuePromotionCount = data.metrics.queue_promotions?.values?.count || 0;
    const successRate = VUS > 0 ? completed / VUS : 0;

    const lines = [
        'Queue Impact Comparison Summary',
        `Mode: ${MODE}`,
        `BASE_URL: ${BASE_URL}`,
        `BROKER_BASE_URL: ${BROKER_BASE_URL}`,
        `Event IDs: ${EVENT_IDS.join(', ')}`,
        `Users: ${VUS}`,
        `Target: ${TARGET_METHOD} ${TARGET_PATH_TEMPLATE}`,
        `Target requires entry token: ${TARGET_REQUIRES_ENTRY_TOKEN}`,
        `Completed users: ${completed}`,
        `Success rate: ${(successRate * 100).toFixed(2)}%`,
        `Target requests: ${targetCount}`,
        `Queue promotions: ${queuePromotionCount}`,
        `Login failures: ${loginFailureCount}`,
        `Login final failures: ${loginFinalFailureCount}`,
        `Polling failures: ${pollingFailureCount}`,
        `Polling timeout failures: ${pollingTimeoutFailureCount}`,
        `P95 target request time: ${formatMetric(data, 'target_req_time_ms', 'p(95)')} ms`,
        `P95 time to target: ${formatMetric(data, 'time_to_target_ms', 'p(95)')} ms`,
        `P95 queue wait time: ${formatMetric(data, 'queue_wait_time_ms', 'p(95)')} ms`,
        `P95 polling enter time: ${formatMetric(data, 'polling_enter_time_ms', 'p(95)')} ms`,
        `P95 polling current time: ${formatMetric(data, 'polling_current_time_ms', 'p(95)')} ms`,
        `http_req_failed rate: ${formatMetric(data, 'http_req_failed', 'rate')}`,
        `http_req_duration p95: ${formatMetric(data, 'http_req_duration', 'p(95)')} ms`,
    ];

    return {
        stdout: lines.join('\n'),
        [SUMMARY_FILE]: JSON.stringify(data, null, 2),
    };
}

function formatMetric(data, metricName, field) {
    const value = data.metrics?.[metricName]?.values?.[field];
    if (value == null || Number.isNaN(value)) {
        return 'n/a';
    }
    return Number(value).toFixed(2);
}
