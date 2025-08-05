// K6 Queue Order Test - HTTP-based version (without SSE extension)
// Tests 10 users login performance and queue connection capability

import http from 'k6/http';
import {check, sleep} from 'k6';
import {Rate, Trend} from 'k6/metrics';

// Test Configuration
const USER_COUNT = 10;
const BASE_URL = 'http://localhost:8080';
const TEST_EVENT_ID = '0197f92b-219e-7a57-b11f-0a356687457f';

// Custom Metrics
const loginTime = new Trend('login_req_duration');
const queueConnectionTime = new Trend('queue_connection_duration');
const loginSuccessRate = new Rate('login_success_rate');
const queueConnectionRate = new Rate('queue_connection_success_rate');

// K6 test options
export const options = {
    scenarios: {
        queue_load_test: {
            executor: 'shared-iterations',
            vus: 1, // Single VU for sequential testing
            iterations: 1,
            maxDuration: '2m',
        },
    },
    thresholds: {
        'http_req_failed': ['rate<0.05'],
        'http_req_duration': ['p(95)<3000'],
        'login_success_rate': ['rate>0.95'],
        'queue_connection_success_rate': ['rate>0.95'],
        'login_req_duration': ['p(95)<2000'],
        'queue_connection_duration': ['p(95)<5000'],
    },
};

export default function() {
    console.log(`🚀 [K6 HTTP TEST] Testing queue system with ${USER_COUNT} users`);
    console.log(`Event ID: ${TEST_EVENT_ID}`);
    
    // Phase 1: Login Performance Test
    const users = testLoginPerformance();
    
    // Phase 2: Queue Connection Test  
    testQueueConnections(users);
    
    // Phase 3: Summary
    printSummary();
}

function testLoginPerformance() {
    console.log('\n📝 [PHASE 1] Testing login performance...');
    const users = [];
    const params = { headers: { 'Content-Type': 'application/json' } };
    
    for (let i = 1; i <= USER_COUNT; i++) {
        const email = `queuetest${i}@ticketon.com`;
        const password = 'password123';
        const loginPayload = JSON.stringify({ email, password });
        
        const startTime = Date.now();
        const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, loginPayload, params);
        const loginDuration = Date.now() - startTime;
        
        // Record metrics
        loginTime.add(loginDuration);
        
        const loginSuccess = check(loginRes, {
            [`User ${i} login status 200`]: (r) => r.status === 200,
            [`User ${i} has auth token`]: (r) => r.headers['Authorization'] !== undefined,
            [`User ${i} response time < 3s`]: (r) => r.timings.duration < 3000,
        });
        
        loginSuccessRate.add(loginSuccess);
        
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
            
            console.log(`✅ [LOGIN] User ${i}: ${loginDuration}ms - ${loginRes.status}`);
        } else {
            console.error(`❌ [LOGIN FAILED] User ${i}: ${loginRes.status} - ${loginDuration}ms`);
        }
        
        sleep(0.1); // Small delay between requests
    }
    
    console.log(`📊 [PHASE 1 RESULT] ${users.length}/${USER_COUNT} users logged in successfully`);
    return users;
}

function testQueueConnections(users) {
    console.log('\n🎯 [PHASE 2] Testing queue connections...');
    
    users.forEach((user, index) => {
        const queueUrl = `${BASE_URL}/api/v1/broker/events/${TEST_EVENT_ID}/tickets/waiting`;
        const params = {
            headers: {
                'Authorization': user.authToken,
                'Accept': 'text/event-stream',
                'Cookie': user.cookie,
            },
            timeout: '10s', // Shorter timeout for HTTP test
        };
        
        const startTime = Date.now();
        
        // Since we can't use SSE in standard k6, we'll test the initial connection
        const queueRes = http.get(queueUrl, params);
        const connectionDuration = Date.now() - startTime;
        
        // Record metrics
        queueConnectionTime.add(connectionDuration);
        
        const connectionSuccess = check(queueRes, {
            [`User ${user.id} queue connection status 200`]: (r) => r.status === 200,
            [`User ${user.id} queue response time < 5s`]: (r) => r.timings.duration < 5000,
            [`User ${user.id} queue response has data`]: (r) => r.body && r.body.length > 0,
        });
        
        queueConnectionRate.add(connectionSuccess);
        
        if (connectionSuccess) {
            console.log(`✅ [QUEUE] User ${user.id}: Connected in ${connectionDuration}ms`);
            
            // Check if response contains queue status
            if (queueRes.body && queueRes.body.includes('IN_ENTRY')) {
                console.log(`🎉 [STATUS] User ${user.id}: Found IN_ENTRY status (quit waiting queue)`);
            } else if (queueRes.body && queueRes.body.includes('WAITING')) {
                console.log(`⏳ [STATUS] User ${user.id}: Found WAITING status (in queue)`);
            } else {
                console.log(`📡 [STATUS] User ${user.id}: SSE connection established`);
            }
        } else {
            console.error(`❌ [QUEUE FAILED] User ${user.id}: ${queueRes.status} - ${connectionDuration}ms`);
        }
        
        sleep(0.2); // Delay between connections
    });
    
    console.log(`📊 [PHASE 2 RESULT] Queue connection test completed`);
}

function printSummary() {
    console.log('\n📋 [PHASE 3] TEST SUMMARY');
    console.log('=========================');
    console.log('This HTTP-based test validates:');
    console.log('✅ User authentication performance');
    console.log('✅ Queue endpoint connectivity');  
    console.log('✅ Basic SSE connection establishment');
    console.log('');
    console.log('🔍 TO TEST FULL QUEUE ORDERING:');
    console.log('Use the Node.js version with full SSE support:');
    console.log('  node queue-order-test.mjs');
    console.log('');
    console.log('🎯 OR use k6 with SSE extension:');
    console.log('  k6 run --out json=results.json k6-queue-http-test.js');
    console.log('');
    console.log('📊 Check k6 metrics above for performance results');
}

export function teardown(data) {
    console.log('\n🧹 [TEARDOWN] K6 HTTP test completed');
    console.log('Check the metrics summary above for performance insights');
}