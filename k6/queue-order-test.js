// Queue Order Test - Verify user ordering in waiting queue promotion
// Tests 10 users entering queue sequentially and checks promotion order

import http from 'k6/http';
import {check, sleep} from 'k6';
import sse from 'k6/x/sse';

// Test Configuration
const USER_COUNT = 10;
const BASE_URL = 'http://localhost:8080';
const TEST_EVENT_ID = '0197f92b-219e-7a57-b11f-0a356687457f'; // First event from created_event_ids.txt

// Global variables to track results
let entryOrder = [];
let promotionOrder = [];
let testResults = {
    usersLoggedIn: 0,
    usersEnteredQueue: 0,
    usersPromoted: 0,
    entryTimestamps: {},
    promotionTimestamps: {},
    orderViolations: []
};

export const options = {
    scenarios: {
        queue_order_test: {
            executor: 'shared-iterations',
            vus: 1, // Single VU to ensure sequential execution
            iterations: 1,
            maxDuration: '5m',
        },
    },
    thresholds: {
        'http_req_failed': ['rate<0.05'],
        'http_req_duration': ['p(95)<3000'],
    },
};

export default function() {
    console.log(`[TEST START] Testing queue order with ${USER_COUNT} users for event ${TEST_EVENT_ID}`);
    
    // Phase 1: Login all users sequentially
    const users = loginUsers();
    if (users.length !== USER_COUNT) {
        console.error(`[ERROR] Expected ${USER_COUNT} users, got ${users.length}`);
        return;
    }
    
    // Phase 2: Enter queue sequentially and monitor
    enterQueueAndMonitor(users);
    
    // Phase 3: Wait for promotions and analyze results
    sleep(30); // Wait for promotions to complete
    analyzeResults();
}

function loginUsers() {
    console.log('[PHASE 1] Starting user login process...');
    const users = [];
    const params = { headers: { 'Content-Type': 'application/json' } };
    
    for (let i = 1; i <= USER_COUNT; i++) {
        const email = `queuetest${i}@ticketon.com`;
        const password = 'password123';
        const loginPayload = JSON.stringify({ email, password });
        
        console.log(`[LOGIN] Attempting login for user ${i}: ${email}`);
        const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, loginPayload, params);
        
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
            console.log(`[LOGIN SUCCESS] User ${i} logged in successfully`);
        } else {
            console.error(`[LOGIN FAILED] User ${i} login failed. Status: ${loginRes.status}`);
        }
        
        sleep(0.1); // Small delay between logins
    }
    
    console.log(`[PHASE 1 COMPLETE] Successfully logged in ${users.length}/${USER_COUNT} users`);
    return users;
}

function enterQueueAndMonitor(users) {
    console.log('[PHASE 2] Starting queue entry process...');
    
    users.forEach((user, index) => {
        const entryTimestamp = Date.now();
        entryOrder.push(user.id);
        testResults.entryTimestamps[user.id] = entryTimestamp;
        
        console.log(`[QUEUE ENTRY] User ${user.id} entering queue at ${entryTimestamp}`);
        
        const sseUrl = `${BASE_URL}/api/v1/broker/events/${TEST_EVENT_ID}/tickets/waiting`;
        const params = {
            headers: {
                'Authorization': user.authToken,
                'Accept': 'text/event-stream',
                'Cookie': user.cookie,
            },
        };
        
        // Start SSE connection for this user
        const res = sse.open(sseUrl, params, function(client) {
            let hasEnteredQueue = false;
            
            client.on('event', function(event) {
                if (!event || !event.data) return;
                
                try {
                    const message = JSON.parse(event.data);
                    
                    // User successfully entered waiting queue
                    if (message.status === 'WAITING' && !hasEnteredQueue) {
                        hasEnteredQueue = true;
                        testResults.usersEnteredQueue++;
                        console.log(`[QUEUE ENTERED] User ${user.id} entered waiting queue (position: ${message.position || 'unknown'})`);
                    }
                    
                    // User quit waiting queue (promoted to entry queue)
                    if (message.status === 'IN_ENTRY') {
                        const promotionTimestamp = Date.now();
                        promotionOrder.push(user.id);
                        testResults.promotionTimestamps[user.id] = promotionTimestamp;
                        testResults.usersPromoted++;
                        
                        const waitTime = promotionTimestamp - entryTimestamp;
                        console.log(`[QUIT QUEUE] User ${user.id} quit waiting queue at ${promotionTimestamp} (waited ${waitTime}ms)`);
                        
                        // Check for order violation
                        const expectedPosition = entryOrder.indexOf(user.id) + 1;
                        const actualPosition = promotionOrder.length;
                        
                        if (expectedPosition !== actualPosition) {
                            const violation = {
                                userId: user.id,
                                expectedPosition: expectedPosition,
                                actualPosition: actualPosition,
                                entryTime: entryTimestamp,
                                promotionTime: promotionTimestamp
                            };
                            testResults.orderViolations.push(violation);
                            console.error(`[ORDER VIOLATION] User ${user.id} expected position ${expectedPosition}, got ${actualPosition}`);
                        }
                        
                        client.close();
                    }
                } catch (e) {
                    // Ignore parsing errors for heartbeat/connect events
                }
            });
            
            client.on('error', function(error) {
                console.error(`[SSE ERROR] User ${user.id}: ${error}`);
            });
        });
        
        check(res, {
            [`User ${user.id} SSE connection started`]: (r) => r && r.status === 200
        });
        
        // Small delay between queue entries to ensure order
        sleep(0.2);
    });
    
    console.log(`[PHASE 2 COMPLETE] ${testResults.usersEnteredQueue} users entered queue`);
}

function analyzeResults() {
    console.log('\n[PHASE 3] ANALYZING RESULTS...');
    console.log('=====================================');
    
    console.log(`Users logged in: ${testResults.usersLoggedIn}/${USER_COUNT}`);
    console.log(`Users entered queue: ${testResults.usersEnteredQueue}/${USER_COUNT}`);
    console.log(`Users promoted: ${testResults.usersPromoted}/${testResults.usersEnteredQueue}`);
    
    console.log(`\nEntry order: [${entryOrder.join(', ')}]`);
    console.log(`Promotion order: [${promotionOrder.join(', ')}]`);
    
    // Check if order is maintained
    const orderMaintained = JSON.stringify(entryOrder.slice(0, promotionOrder.length)) === JSON.stringify(promotionOrder);
    console.log(`\nOrder maintained: ${orderMaintained ? '✅ YES' : '❌ NO'}`);
    
    if (testResults.orderViolations.length > 0) {
        console.log(`\n❌ ORDER VIOLATIONS (${testResults.orderViolations.length}):`);
        testResults.orderViolations.forEach(violation => {
            console.log(`  User ${violation.userId}: Expected pos ${violation.expectedPosition}, got ${violation.actualPosition}`);
        });
    } else if (promotionOrder.length > 0) {
        console.log('\n✅ No order violations detected');
    }
    
    // Calculate promotion times
    if (promotionOrder.length > 0) {
        console.log('\n📊 PROMOTION TIMING:');
        promotionOrder.forEach(userId => {
            const entryTime = testResults.entryTimestamps[userId];
            const promotionTime = testResults.promotionTimestamps[userId];
            const waitTime = promotionTime - entryTime;
            console.log(`  User ${userId}: ${waitTime}ms wait time`);
        });
    }
    
    console.log('\n=====================================');
    console.log('[TEST COMPLETE]');
}

export function teardown(data) {
    console.log('\n[TEARDOWN] Test completed');
    
    // Final summary
    if (testResults.orderViolations.length === 0 && testResults.usersPromoted > 0) {
        console.log('🎉 RESULT: User queue ordering is working correctly');
    } else if (testResults.usersPromoted === 0) {
        console.log('⚠️  RESULT: No users were promoted during test');
    } else {
        console.log('🚨 RESULT: User queue ordering has violations - needs investigation');
    }
}