import http from 'k6/http';
import {check} from 'k6';
import {Trend} from 'k6/metrics';

// --- 테스트 환경 설정 ---
const USER_COUNT = 1000; // 시나리오의 최대 유저 수에 맞춰 설정
const BASE_URL_BROKER = 'http://localhost:8080';

// --- 시나리오 시간 설정 (분 단위) ---
const RAMP_UP_1_M = 2;
const RAMP_DOWN_1_M = 2;
const RAMP_UP_2_M = 2;
const RAMP_DOWN_2_M = 2;

const eventIds = open('./../created_event_ids.txt').split('\n').filter(id => id);

// --- 사용자 정의 메트릭 ---
const loginTime = new Trend('login_req_duration');
const sseConnectionTime = new Trend('sse_connection_req_duration');

// --- k6 실행 옵션 ---
export const options = {
    scenarios: {
        sse_load_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: `${RAMP_UP_1_M}m`, target: 1000 },    // 2분 동안 1000명으로 증가
                { duration: `${RAMP_DOWN_1_M}m`, target: 500 },   // 2분 동안 500명으로 감소
                { duration: `${RAMP_UP_2_M}m`, target: 1000 },    // 2분 동안 다시 1000명으로 증가
                { duration: `${RAMP_DOWN_2_M}m 30s`, target: 0 },     // 2분 동안 0명으로 감소
            ],
            gracefulRampDown: '5s',
        },
    },
    thresholds: {
        'http_req_failed': ['rate<0.01'],
        'http_req_duration': ['p(95)<2000'],
    },
};

// ==================================================================================
// 1. Setup 단계
// ==================================================================================
export function setup() {
    if (eventIds.length === 0) {
        throw new Error("created_event_ids.txt 파일이 비어있거나 찾을 수 없습니다.");
    }

    console.log(`[Setup] ${eventIds.length}개의 이벤트를 사용하여 ${USER_COUNT}명의 로그인을 시작합니다...`);

    const users = [];
    const params = { headers: { 'Content-Type': 'application/json' } };
    for (let i = 1; i <= USER_COUNT; i++) {
        const email = `user${i}@ticketon.com`;
        const password = 'password123';
        const loginPayload = JSON.stringify({ email, password });
        const loginRes = http.post(`${BASE_URL_BROKER}/api/v1/auth/login`, loginPayload, params);
        check(loginRes, { '로그인 성공': (r) => r.status === 200 });
        loginTime.add(loginRes.timings.duration);

        if (loginRes.status === 200 && loginRes.headers['Authorization']) {
            const authToken = loginRes.headers['Authorization'];

            // ✅ 모든 유저에게 여러 이벤트 ID를 순환하며 할당
            const eventId = eventIds[(i - 1) % eventIds.length];

            let cookieString = Object.values(loginRes.cookies || {})
                .flat()
                .map(c => `${c.name}=${c.value}`)
                .join('; ');
            users.push({ id: email, authToken, eventId, cookie: cookieString });
        } else {
            console.error(`[Setup] 유저 ${i}의 로그인 또는 토큰 획득에 실패했습니다. Status: ${loginRes.status}`);
        }
    }
    console.log(`[Setup] 총 ${users.length}명의 유저 데이터 생성 완료.`);
    return { users };
}

// ==================================================================================
// 2. Execution 단계
// ==================================================================================
export default function (data) {
    // __VU는 1부터 시작하고, 배열 인덱스는 0부터 시작합니다.
    // VU 수가 생성된 유저 수보다 많아도 문제 없도록 modulo 연산 사용
    const userIndex = (__VU - 1) % data.users.length;
    const user = data.users[userIndex];
    if (!user) {
        return;
    }

    const sseUrl = `${BASE_URL_BROKER}/api/v1/broker/events/${user.eventId}/tickets/waiting`;
    const totalDurationM = RAMP_UP_1_M + RAMP_DOWN_1_M + RAMP_UP_2_M + RAMP_DOWN_2_M;
    const params = {
        headers: {
            'Authorization': user.authToken,
            'Accept': 'text/event-stream',
            'Cookie': user.cookie,
        },
        // 타임아웃은 전체 테스트 시간보다 넉넉하게 설정
        timeout: `${totalDurationM + 1}m`,
    };

    const res = http.get(sseUrl, params);

    // k6 실행기가 VU를 종료시킬 때, 연결이 정상적으로 닫혔는지 확인합니다.
    check(res, { 'SSE 연결이 정상적으로 종료됨 (status 200)': (r) => r.status === 200 });
    sseConnectionTime.add(res.timings.duration);
}