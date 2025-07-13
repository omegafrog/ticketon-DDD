// C:/Users/jiwoo/workspace/ticketon-DDD/k6/todo-test.js

import http from 'k6/http';
import {check, sleep} from 'k6';
import {Trend} from 'k6/metrics';
import sse from 'k6/x/sse'; // ✅ 1. http 대신 sse 모듈을 import 합니다.

// --- 테스트 환경 설정 ---
const USER_COUNT = 3000; // 시나리오의 최대 유저 수에 맞춰 설정
const BASE_URL_BROKER = 'http://localhost:8080';

// --- 시나리오 시간 설정 (분 단위) ---
const RAMP_UP_M = 3;
const RAMP_DOWN_M = 3;

const eventIds = open('./../created_event_ids.txt').split('\n').filter(id => id);

// --- 사용자 정의 메트릭 ---
const loginTime = new Trend('login_req_duration');
// sse() 함수는 즉시 반환되므로, 전체 연결 시간 측정은 의미가 없어집니다.
// const sseConnectionTime = new Trend('sse_connection_req_duration');
const timeToPromotion = new Trend('time_to_promotion', true);

// --- k6 실행 옵션 ---
export const options = {
    scenarios: {
        sse_load_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                {duration: `${RAMP_UP_M}m`, target: USER_COUNT},
                {duration: `1m`, target: USER_COUNT},
                {duration: `${RAMP_DOWN_M}m`, target: 0},
            ],
            gracefulRampDown: '5s',
        },
    },
    thresholds: {
        'http_req_failed': ['rate<0.01'],
        'http_req_duration': ['p(95)<2000'],
        'time_to_promotion': ['p(95)<5000'], // 입장까지 5초 이내 목표 추가
    },
};

// ==================================================================================
// 1. Setup 단계 (기존과 동일)
// ==================================================================================
export function setup() {
    if (eventIds.length === 0) {
        throw new Error("created_event_ids.txt 파일이 비어있거나 찾을 수 없습니다.");
    }

    console.log(`[Setup] ${eventIds.length}개의 이벤트를 사용하여 ${USER_COUNT}명의 로그인을 시작합니다...`);

    const users = [];
    const params = {headers: {'Content-Type': 'application/json'}};
    for (let i = 1; i <= USER_COUNT; i++) {
        const email = `user${i}@ticketon.com`;
        const password = 'password123';
        const loginPayload = JSON.stringify({email, password});
        const loginRes = http.post(`${BASE_URL_BROKER}/api/v1/auth/login`, loginPayload, params);
        check(loginRes, {'로그인 성공': (r) => r.status === 200});
        loginTime.add(loginRes.timings.duration);

        if (loginRes.status === 200 && loginRes.headers['Authorization']) {
            const authToken = loginRes.headers['Authorization'];
            const eventId = eventIds[(i - 1) % eventIds.length];
            let cookieString = Object.values(loginRes.cookies || {})
                .flat()
                .map(c => `${c.name}=${c.value}`)
                .join('; ');
            users.push({id: email, authToken, eventId, cookie: cookieString});
        } else {
            console.error(`[Setup] 유저 ${i}의 로그인 또는 토큰 획득에 실패했습니다. Status: ${loginRes.status}`);
        }
    }
    console.log(`[Setup] 총 ${users.length}명의 유저 데이터 생성 완료.`);
    return {users};
}

// ==================================================================================
// 2. Execution 단계
// ==================================================================================
export default function (data) {
    const userIndex = (__VU-1);
    const user = data.users[userIndex];
    if (!user) {
        return;
    }

    const sseUrl = `${BASE_URL_BROKER}/api/v1/broker/events/${user.eventId}/tickets/waiting`;
    const params = {
        headers: {
            'Authorization': user.authToken,
            'Accept': 'text/event-stream',
            'Cookie': user.cookie,
        },
    };
    const startTime =Date.now();
    // ✅ 2. http.get 대신 sse() 함수를 사용합니다.
    const res = sse.open(sseUrl, params, function (client) {
        client.on('event', function (event) {
            // event.data가 비어있거나, connect 또는 heartbeat 이벤트는 무시
            if (!event || !event.data || event.name === 'connect' || event.event === 'comment') {
                return;
            }

            try {
                const message = JSON.parse(event.data);
                // console.log(message.status);

                // 'status'가 'IN_PROGRESS'인 이벤트가 바로 '입장' 이벤트
                if (message.status === 'IN_PROGRESS') {
                    // console.log("success");
                    // ✅ 3. this.startTime은 sse()가 내부적으로 기록해주는 연결 시작 시간입니다.
                    const promotionTime = Date.now() - startTime;
                    timeToPromotion.add(promotionTime);


                    // 역할이 끝났으므로 이 VU는 연결을 종료합니다.
                    client.close();
                }
            } catch (e) {
                // console.warn(`Failed to parse SSE data: ${event.data}`);
            }
        })

    });

    // console.log("sse closed");

    // sse() 함수가 성공적으로 연결을 시작했는지 확인합니다.
    check(res, {'SSE 연결 시작 성공 (status 200)': (r) => r && r.status === 200});
    sleep(60)
}