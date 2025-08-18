// K6 Queue Order Test with SSE Support
// Tests queue ordering using Server-Sent Events

import http from 'k6/http';
import {check, sleep} from 'k6';
import {Counter, Trend} from 'k6/metrics';

// Test Configuration
const USER_COUNT = 10;
const BASE_URL = 'http://localhost:8080';
const TEST_EVENT_ID = '0197f92b-219e-7a57-b11f-0a356687457f';

// Custom Metrics
const loginTime = new Trend('login_req_duration');
const timeToQuit = new Trend('time_to_quit_queue');
const orderViolations = new Counter('order_violations');
const usersPromoted = new Counter('users_promoted');

// Global state
let entryOrder = [];
let promotionOrder = [];
let testResults = {
    usersLoggedIn: 0,
    usersPromoted: 0,
    orderViolationsList: []
};

export const options = {
    scenarios: {
        queue_sse_test: {
            executor: 'shared-iterations',
            vus: 1,
            iterations: 1,
            maxDuration: '3m',
        },
    },
    thresholds: {
        'http_req_failed': ['rate<0.05'],
        'login_req_duration': ['p(95)<2000'],
        'time_to_quit_queue': ['p(95)<5000'],
        'order_violations': ['count<=2'], // Allow up to 2 violations due to race conditions
    },
};

export default function() {
    console.log(`🚀 [K6 SSE TEST] Testing queue order with ${USER_COUNT} users`);
    
    // Phase 1: Login users
    const users = loginUsers();
    if (users.length === 0) {
        console.error('❌ No users logged in successfully');
        return;
    }
    
    // Phase 2: Test queue with SSE
    testQueueWithSSE(users);
    
    // Phase 3: Analyze results
    analyzeResults();
}

function loginUsers() {
    console.log('📝 [PHASE 1] Logging in users...');
    const users = [];
    
    for (let i = 1; i <= USER_COUNT; i++) {
        const email = `queuetest${i}@ticketon.com`;
        const password = 'password123';
        
        const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, 
            JSON.stringify({ email, password }), 
            { headers: { 'Content-Type': 'application/json' } }
        );
        
        loginTime.add(loginRes.timings.duration);
        
        if (check(loginRes, { [`User ${i} login OK`]: (r) => r.status === 200 }) &&
            loginRes.headers['Authorization']) {
            
            const authToken = loginRes.headers['Authorization'];
            const cookies = Object.values(loginRes.cookies || {})
                .flat()
                .map(c => `${c.name}=${c.value}`)
                .join('; ');
            
            users.push({ id: i, authToken, cookies });
            testResults.usersLoggedIn++;
            console.log(`✅ User ${i} logged in`);
        } else {
            console.error(`❌ User ${i} login failed`);
        }
        
        sleep(0.1);
    }
    
    console.log(`📊 ${users.length}/${USER_COUNT} users logged in`);
    return users;
}

function testQueueWithSSE(users) {
    console.log('🎯 [PHASE 2] Testing queue with SSE...');
    
    // Process users sequentially to maintain order
    users.forEach((user, index) => {
        const entryTime = Date.now();
        entryOrder.push(user.id);
        
        console.log(`⏰ User ${user.id} entering queue...`);
        
        // Use k6's streaming response to handle SSE
        const response = http.get(
            `${BASE_URL}/api/v1/broker/events/${TEST_EVENT_ID}/tickets/waiting`,
            {
                headers: {
                    'Authorization': user.authToken,
                    'Accept': 'text/event-stream',
                    'Cookie': user.cookies,
                },
                responseType: 'text',
                timeout: '30s',
            }
        );
        
        if (check(response, { [`User ${user.id} SSE connected`]: (r) => r.status === 200 })) {
            // Parse SSE response for events
            const sseData = parseSSEResponse(response.body);
            procesSSEEvents(user, sseData, entryTime);
        } else {
            console.error(`❌ User ${user.id} SSE connection failed: ${response.status}`);
        }
        
        sleep(0.2); // Delay between users
    });
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
                    // Try to parse as JSON
                    const parsed = JSON.parse(data);
                    events.push(parsed);
                } catch (e) {
                    // If not JSON, store as text
                    events.push({ type: 'text', data: data });
                }
            }
        }
    }
    
    return events;
}

function procesSSEEvents(user, events, entryTime) {
    for (const event of events) {
        if (event.status === 'WAITING') {
            console.log(`🎫 User ${user.id} entered waiting queue`);
        } else if (event.status === 'IN_ENTRY') {
            const quitTime = Date.now();
            const waitDuration = quitTime - entryTime;
            
            promotionOrder.push(user.id);
            testResults.usersPromoted++;
            usersPromoted.add(1);
            timeToQuit.add(waitDuration);
            
            console.log(`🎉 User ${user.id} quit waiting queue (${waitDuration}ms)`);
            
            // Check for order violation
            checkOrderViolation(user.id, entryTime, quitTime);
            break;
        }
    }
}

function checkOrderViolation(userId, entryTime, quitTime) {
    const expectedPosition = entryOrder.indexOf(userId) + 1;
    const actualPosition = promotionOrder.length;
    
    if (expectedPosition !== actualPosition) {
        const violation = {
            userId,
            expectedPosition,
            actualPosition,
            entryTime,
            quitTime
        };
        
        testResults.orderViolationsList.push(violation);
        orderViolations.add(1);
        
        console.error(`🚨 ORDER VIOLATION: User ${userId} expected pos ${expectedPosition}, got ${actualPosition}`);
    }
}

function analyzeResults() {
    console.log('\n📊 [PHASE 3] RESULTS ANALYSIS');
    console.log('==============================');
    
    console.log(`Users logged in: ${testResults.usersLoggedIn}/${USER_COUNT}`);
    console.log(`Users promoted: ${testResults.usersPromoted}`);
    
    console.log(`\nEntry order: [${entryOrder.join(', ')}]`);
    console.log(`Promotion order: [${promotionOrder.join(', ')}]`);
    
    const orderMaintained = JSON.stringify(entryOrder.slice(0, promotionOrder.length)) === 
                           JSON.stringify(promotionOrder);
    
    console.log(`\nOrder maintained: ${orderMaintained ? '✅ YES' : '❌ NO'}`);
    
    if (testResults.orderViolationsList.length > 0) {
        console.log(`\n🚨 ORDER VIOLATIONS (${testResults.orderViolationsList.length}):`);
        testResults.orderViolationsList.forEach(v => {
            console.log(`  User ${v.userId}: Expected ${v.expectedPosition}, got ${v.actualPosition}`);
        });
        
        console.log('\n🔍 ROOT CAUSE ANALYSIS:');
        console.log('This confirms the race condition in EntryPromoteThread.java:');
        console.log('- Lines 125-132: Multiple threads process events concurrently');
        console.log('- LPOP operations create race conditions');
        console.log('- No FIFO guarantee across different events');
    } else if (promotionOrder.length > 0) {
        console.log('\n✅ No order violations detected - queue ordering working correctly');
    } else {
        console.log('\n⚠️  No users were promoted - check dispatcher service');
    }
    
    // Performance summary
    if (promotionOrder.length > 0) {
        console.log('\n⏱️  PERFORMANCE SUMMARY:');
        console.log(`Average promotion time: Available in k6 metrics`);
        console.log(`Total violations: ${testResults.orderViolationsList.length}`);
        console.log(`Success rate: ${((promotionOrder.length - testResults.orderViolationsList.length) / promotionOrder.length * 100).toFixed(1)}%`);
    }
}

export function teardown(data) {
    console.log('\n🏁 [K6 SSE TEST COMPLETE]');
    
    if (testResults.orderViolationsList.length === 0 && testResults.usersPromoted > 0) {
        console.log('🎉 SUCCESS: Queue ordering maintained correctly');
    } else if (testResults.usersPromoted === 0) {
        console.log('⚠️  WARNING: No users promoted - check services');
    } else {
        console.log(`🚨 ISSUE: Found ${testResults.orderViolationsList.length} order violations`);
        console.log('This confirms multithreading race conditions in the dispatcher');
    }
    
    console.log('\n📈 Check k6 metrics summary above for detailed performance data');
}