// K6 Queue Order Test - Verify user ordering in waiting queue promotion
// Tests 10 users entering queue sequentially and checks for order violations

import http from 'k6/http';
import {check, sleep} from 'k6';
import {Counter, Trend} from 'k6/metrics';

// Test Configuration
const USER_COUNT = 10;
const BASE_URL = 'http://localhost:8080';
const TEST_EVENT_ID = '0197f92b-219e-7a57-b11f-0a356687457f'; // First event from created_event_ids.txt

// Custom Metrics
const loginTime = new Trend('login_req_duration');
const queueEntryTime = new Trend('queue_entry_duration');
const timeToQuit = new Trend('time_to_quit_queue', true);
const orderViolations = new Counter('order_violations');

// Global variables to track results
let entryOrder = [];
let promotionOrder = [];
let testResults = {
    usersLoggedIn: 0,
    usersEnteredQueue: 0,
    usersPromoted: 0,
    entryTimestamps: {},
    promotionTimestamps: {},
    orderViolationsList: []
};

// K6 test options
export const options = {
    scenarios: {
        queue_order_test: {
            executor: 'shared-iterations',
            vus: 1, // Single VU to ensure sequential execution
            iterations: 1,
            maxDuration: '2m',
        },
    },
    thresholds: {
        'http_req_failed': ['rate<0.05'],
        'http_req_duration': ['p(95)<3000'],
        'order_violations': ['count==0'], // No order violations expected for proper queue
        'time_to_quit_queue': ['p(95)<5000'], // Users should quit queue within 5 seconds
    },
};

export default function() {
    console.log(`🚀 [K6 TEST START] Testing queue order with ${USER_COUNT} users for event ${TEST_EVENT_ID}`);
    
    // Phase 1: Login all users sequentially
    const users = loginUsers();
    if (users.length !== USER_COUNT) {
        console.error(`❌ [ERROR] Expected ${USER_COUNT} users, got ${users.length}`);
        return;
    }
    
    // Phase 2: Enter queue sequentially and monitor
    enterQueueAndMonitor(users);
    
    // Phase 3: Wait for all users to complete and analyze results
    sleep(30); // Wait for all queue events to complete
    analyzeResults();
}

function loginUsers() {
    console.log('📝 [PHASE 1] Starting user login process...');
    const users = [];
    const params = { headers: { 'Content-Type': 'application/json' } };
    
    for (let i = 1; i <= USER_COUNT; i++) {
        const email = `queuetest${i}@ticketon.com`;
        const password = 'password123';
        const loginPayload = JSON.stringify({ email, password });
        
        console.log(`🔐 [LOGIN] Attempting login for user ${i}: ${email}`);
        const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, loginPayload, params);
        
        // Record login time
        loginTime.add(loginRes.timings.duration);
        
        const loginSuccess = check(loginRes, {
            [`User ${i} login success`]: (r) => r.status === 200,
            [`User ${i} has auth token`]: (r) => r.headers['Authorization'] !== undefined
        });
        
        if (loginSuccess && loginRes.status === 200 && loginRes.headers['Authorization']) {
            const authToken = loginRes.headers['Authorization'];
            let cookieString = Object.values(loginRes.cookies || {})
                .flat()
                .map(c => `${c.name}=${c.value}`)
                .join('; ');
                
            users.push({
                id: i,
                email: email,
                authToken: authToken,
                cookie: cookieString
            });
            
            testResults.usersLoggedIn++;
            console.log(`✅ [LOGIN SUCCESS] User ${i} logged in successfully`);
        } else {
            console.error(`❌ [LOGIN FAILED] User ${i} login failed. Status: ${loginRes.status}`);
        }
        
        sleep(0.1); // Small delay between logins
    }
    
    console.log(`📝 [PHASE 1 COMPLETE] Successfully logged in ${users.length}/${USER_COUNT} users`);
    return users;
}

function enterQueueAndMonitor(users) {
    console.log('🎯 [PHASE 2] Starting queue entry process...');
    
    users.forEach((user, index) => {
        const entryTimestamp = Date.now();
        entryOrder.push(user.id);
        testResults.entryTimestamps[user.id] = entryTimestamp;
        testResults.usersEnteredQueue++;
        
        console.log(`⏰ [QUEUE ENTRY] User ${user.id} entering queue at ${new Date(entryTimestamp).toISOString()}`);
        
        const sseUrl = `${BASE_URL}/api/v1/broker/events/${TEST_EVENT_ID}/tickets/waiting`;
        const params = {
            headers: {
                'Authorization': user.authToken,
                'Accept': 'text/event-stream',
                'Cache-Control': 'no-cache',
                'Cookie': user.cookie,
            },
            timeout: '10s', // 10초 타임아웃으로 빠른 감지
        };
        
        // HTTP GET을 통한 SSE 연결 - k6는 스트리밍 응답을 한 번에 받음
        const response = http.get(sseUrl, params);
        
        const connectionSuccess = check(response, {
            [`User ${user.id} SSE connection success`]: (r) => r.status === 200,
            [`User ${user.id} has response body`]: (r) => r.body && r.body.length > 0
        });
        
        if (connectionSuccess && response.body) {
            console.log(`📡 [SSE CONNECTED] User ${user.id} connected successfully`);
            
            // SSE 응답 파싱
            const sseEvents = parseSSEResponse(response.body);
            console.log(`📨 [SSE EVENTS] User ${user.id} received ${sseEvents.length} events`);
            
            // 각 이벤트 처리
            sseEvents.forEach(event => {
                if (event.status === 'IN_ENTRY') {
                    const promotionTimestamp = Date.now();
                    promotionOrder.push(user.id);
                    testResults.promotionTimestamps[user.id] = promotionTimestamp;
                    testResults.usersPromoted++;
                    
                    const waitTime = promotionTimestamp - entryTimestamp;
                    timeToQuit.add(waitTime);
                    console.log(`🎉 [QUIT QUEUE] User ${user.id} quit waiting queue (waited ${waitTime}ms)`);
                    
                    // 순서 위반 체크
                    checkOrderViolation(user.id, entryTimestamp, promotionTimestamp);
                } else if (event.status === 'WAITING') {
                    console.log(`⏳ [WAITING] User ${user.id} entered waiting queue`);
                } else {
                    console.log(`📨 [EVENT] User ${user.id} received: ${JSON.stringify(event)}`);
                }
            });
        } else {
            console.error(`❌ [SSE FAILED] User ${user.id} connection failed (${response.status})`);
        }
        
        // 연결 간 간격
        sleep(0.5);
    });
    
    console.log(`🎯 [PHASE 2 COMPLETE] ${testResults.usersEnteredQueue} users entered queue`);
}

function parseSSEResponse(responseBody) {
    if (!responseBody) return [];
    
    const events = [];
    const lines = responseBody.split('\n');
    let currentEvent = {};
    
    for (const line of lines) {
        if (line.startsWith('data:')) {
            const data = line.substring(5).trim();
            if (data && data !== '') {
                try {
                    const parsed = JSON.parse(data);
                    events.push(parsed);
                } catch (e) {
                    // JSON이 아닌 경우 텍스트로 저장
                    events.push({ type: 'text', data: data });
                }
            }
        } else if (line.startsWith('event:')) {
            currentEvent.event = line.substring(6).trim();
        } else if (line.startsWith('id:')) {
            currentEvent.id = line.substring(3).trim();
        } else if (line === '') {
            // 빈 줄은 이벤트 끝을 의미
            if (Object.keys(currentEvent).length > 0) {
                events.push(currentEvent);
                currentEvent = {};
            }
        }
    }
    
    return events;
}

function checkOrderViolation(userId, entryTime, promotionTime) {
    const expectedPosition = entryOrder.indexOf(userId) + 1;
    const actualPosition = promotionOrder.length;
    
    if (expectedPosition !== actualPosition) {
        const violation = {
            userId: userId,
            expectedPosition: expectedPosition,
            actualPosition: actualPosition,
            entryTime: entryTime,
            promotionTime: promotionTime
        };
        testResults.orderViolationsList.push(violation);
        orderViolations.add(1);
        console.error(`🚨 [ORDER VIOLATION] User ${userId} expected position ${expectedPosition}, got ${actualPosition}`);
    }
}

function analyzeResults() {
    console.log('\n📊 [PHASE 3] ANALYZING RESULTS...');
    console.log('=====================================');
    
    console.log(`Users logged in: ${testResults.usersLoggedIn}/${USER_COUNT}`);
    console.log(`Users entered queue: ${testResults.usersEnteredQueue}/${USER_COUNT}`);
    console.log(`Users promoted: ${testResults.usersPromoted}/${testResults.usersEnteredQueue || 1}`);
    
    console.log(`\nEntry order: [${entryOrder.join(', ')}]`);
    console.log(`Promotion order: [${promotionOrder.join(', ')}]`);
    
    // Check if order is maintained
    const orderMaintained = JSON.stringify(entryOrder.slice(0, promotionOrder.length)) === JSON.stringify(promotionOrder);
    console.log(`\nOrder maintained: ${orderMaintained ? '✅ YES' : '❌ NO'}`);
    
    if (testResults.orderViolationsList.length > 0) {
        console.log(`\n🚨 ORDER VIOLATIONS (${testResults.orderViolationsList.length}):`);
        testResults.orderViolationsList.forEach(violation => {
            console.log(`  User ${violation.userId}: Expected pos ${violation.expectedPosition}, got ${violation.actualPosition}`);
        });
    } else if (promotionOrder.length > 0) {
        console.log('\n✅ No order violations detected');
    }
    
    // Calculate promotion times
    if (promotionOrder.length > 0) {
        console.log('\n📈 PROMOTION TIMING:');
        promotionOrder.forEach(userId => {
            const entryTime = testResults.entryTimestamps[userId];
            const promotionTime = testResults.promotionTimestamps[userId];
            const waitTime = promotionTime - entryTime;
            console.log(`  User ${userId}: ${waitTime}ms wait time`);
        });
    }
    
    console.log('\n=====================================');
    console.log('🏁 [K6 TEST COMPLETE]');
    
    // Final summary
    if (testResults.orderViolationsList.length === 0 && testResults.usersPromoted > 0) {
        console.log('🎉 RESULT: User queue ordering is working correctly');
    } else if (testResults.usersPromoted === 0) {
        console.log('⚠️  RESULT: No users were promoted during test');
    } else {
        console.log('🚨 RESULT: User queue ordering has violations - needs investigation');
        console.log(`   Found ${testResults.orderViolationsList.length} order violations`);
    }
}

export function teardown(data) {
    console.log('\n🧹 [TEARDOWN] K6 test completed');
    
    // Final summary for k6 metrics
    if (testResults.orderViolationsList.length === 0 && testResults.usersPromoted > 0) {
        console.log('🎉 K6 RESULT: User queue ordering is working correctly');
    } else if (testResults.usersPromoted === 0) {
        console.log('⚠️  K6 RESULT: No users were promoted during test - check dispatcher service');
    } else {
        console.log('🚨 K6 RESULT: User queue ordering has violations - multithreading race condition confirmed');
        console.log(`📊 K6 METRICS: ${testResults.orderViolationsList.length} order violations recorded`);
    }
}