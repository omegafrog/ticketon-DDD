import sse from 'k6/x/sse';
import http from 'k6/http';
import {check, sleep} from 'k6';
import {Counter, Trend} from 'k6/metrics';

// Test Configuration
const USER_COUNT = 10;
const BASE_URL = 'http://localhost:8080';
const TEST_EVENT_ID = '0197f92b-219e-7a57-b11f-0a356687457f';

// Custom Metrics
const sseConnectionTime = new Trend('sse_connection_duration');
const eventReceiveTime = new Trend('event_receive_duration');
const connectionErrors = new Counter('sse_connection_errors');
const eventsReceived = new Counter('sse_events_received');

export const options = {
    scenarios: {
        sse_load_test: {
            executor: 'shared-iterations',
            vus: 1,
            iterations: 1,
            maxDuration: '5m',
        },
    },
    thresholds: {
        'sse_connection_duration': ['p(95)<3000'],
        'event_receive_duration': ['p(95)<1000'],
        'sse_connection_errors': ['count<5'],
        'sse_events_received': ['count>0'],
    },
};

export default function() {
    console.log(`🚀 [K6 SSE Experimental] Testing SSE connections with ${USER_COUNT} users`);
    
    // Phase 1: Login users and get authentication tokens
    const users = loginUsers();
    if (users.length === 0) {
        console.error('❌ No users logged in successfully');
        return;
    }
    
    // Phase 2: Test SSE connections
    testSSEConnections(users);
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
        
        if (check(loginRes, { [`User ${i} login OK`]: (r) => r.status === 200 }) &&
            loginRes.headers['Authorization']) {
            
            const authToken = loginRes.headers['Authorization'];
            const cookies = Object.values(loginRes.cookies || {})
                .flat()
                .map(c => `${c.name}=${c.value}`)
                .join('; ');
            
            users.push({ id: i, authToken, cookies });
            console.log(`✅ User ${i} logged in`);
        } else {
            console.error(`❌ User ${i} login failed`);
        }
        
        sleep(0.1);
    }
    
    console.log(`📊 ${users.length}/${USER_COUNT} users logged in`);
    return users;
}

function testSSEConnections(users) {
    console.log('🎯 [PHASE 2] Testing SSE connections...');
    
    // Process users sequentially instead of parallel to avoid blocking
    users.forEach((user, index) => {
        console.log(`🔗 User ${user.id} attempting SSE connection...`);
        
        // Create SSE connection for each user
        const startTime = Date.now();
        testUserSSEConnection(user, startTime);
        
        // Short delay between users
        sleep(0.3);
    });
    
    // Wait for events to be received
    console.log('⏳ Waiting for SSE events...');
    sleep(15); // Shorter wait time
}

function testUserSSEConnection(user, startTime) {
    const url = `${BASE_URL}/api/v1/broker/events/${TEST_EVENT_ID}/tickets/waiting`;
    const params = {
        headers: {
            'Authorization': user.authToken,
            'Cookie': user.cookies,
        },
        tags: { user_id: user.id.toString() }
    };
    
    const response = sse.open(url, params, function (client) {
        client.on('open', function () {
            const connectionTime = Date.now() - startTime;
            sseConnectionTime.add(connectionTime);
            console.log(`✅ User ${user.id} SSE connected (${connectionTime}ms)`);
        });

        client.on('event', function (event) {
            const eventTime = Date.now();
            eventsReceived.add(1);
            eventReceiveTime.add(eventTime - startTime);
            
            // Skip empty events
            if (!event.data || event.data.trim() === '') {
                return;
            }
            
            console.log(`📨 User ${user.id} received event: data=${event.data}`);
            
            // Parse event data if it's JSON
            if (event.data.startsWith('{')) {
                try {
                    const eventData = JSON.parse(event.data);
                    handleEventData(user, eventData);
                } catch (e) {
                    console.log(`📝 User ${user.id} received malformed JSON: ${event.data}`);
                }
            } else {
                console.log(`📝 User ${user.id} received text: ${event.data}`);
            }
        });

        client.on('error', function (e) {
            connectionErrors.add(1);
            console.log(`❌ User ${user.id} SSE error: ${e.error()}`);
        });
        
        // Auto-close connection after 10 seconds
        setTimeout(() => {
            console.log(`⏰ User ${user.id} SSE connection closing...`);
            client.close();
        }, 10000);
    });

    check(response, {
        [`User ${user.id} SSE status OK`]: (r) => r && r.status === 200
    });
    
    if (!response || response.status !== 200) {
        connectionErrors.add(1);
        console.error(`❌ User ${user.id} SSE connection failed with status: ${response ? response.status : 'unknown'}`);
    }
}

function handleEventData(user, eventData) {
    if (eventData.status === 'WAITING') {
        console.log(`⏳ User ${user.id} is in waiting queue - position: ${eventData.position || 'unknown'}`);
    } else if (eventData.status === 'IN_ENTRY') {
        console.log(`🎫 User ${user.id} is in entry queue - order: ${eventData.order || 'unknown'}`);
    } else if (eventData.status === 'IN_PROGRESS') {
        console.log(`🎉 User ${user.id} promoted out of queue! Can now purchase tickets.`);
    } else if (eventData.status === 'EXPIRED') {
        console.log(`⏰ User ${user.id} queue entry expired`);
    } else {
        console.log(`📋 User ${user.id} received status update: ${eventData.status}`);
    }
}

export function teardown(data) {
    console.log('\n🏁 [K6 SSE Experimental Test Complete]');
    console.log('========================================');
    
    console.log('📊 Test Summary:');
    console.log(`- Total users tested: ${USER_COUNT}`);
    console.log('- Check metrics above for detailed performance data');
    console.log('- SSE connection times, event receive times, and error counts are tracked');
    
    console.log('\n💡 Next Steps:');
    console.log('- Review k6 metrics for performance bottlenecks');
    console.log('- Check server logs for any SSE connection issues');
    console.log('- Consider scaling test with more concurrent users');
}