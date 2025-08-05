import sse from 'k6/x/sse';
import http from 'k6/http';
import {check} from 'k6';
import {Counter} from 'k6/metrics';

// Test Configuration
const BASE_URL = 'http://localhost:8080';
const TEST_EVENT_ID = '0197f92b-219e-7a57-b11f-0a356687457f';

// Custom Metrics
const sseEventsReceived = new Counter('sse_events_total');
const sseConnectionErrors = new Counter('sse_connection_errors');

export const options = {
    scenarios: {
        sse_test: {
            executor: 'per-vu-iterations',
            vus: 3, // 3 users simultaneously
            iterations: 1,
            maxDuration: '30s',
        },
    },
    thresholds: {
        'sse_events_total': ['count>0'],
        'sse_connection_errors': ['count<2'],
    },
};

export default function() {
    const userId = __VU; // Use VU number as user ID
    
    console.log(`🚀 [User ${userId}] Starting SSE test`);
    
    // Phase 1: Login
    const user = loginUser(userId);
    if (!user) {
        console.error(`❌ [User ${userId}] Login failed`);
        return;
    }
    
    // Phase 2: SSE Connection
    testSSEConnection(user);
}

function loginUser(userId) {
    const email = `queuetest${userId}@ticketon.com`;
    const password = 'password123';
    
    const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, 
        JSON.stringify({ email, password }), 
        { headers: { 'Content-Type': 'application/json' } }
    );
    
    if (check(loginRes, { [`User ${userId} login OK`]: (r) => r.status === 200 }) &&
        loginRes.headers['Authorization']) {
        
        const authToken = loginRes.headers['Authorization'];
        const cookies = Object.values(loginRes.cookies || {})
            .flat()
            .map(c => `${c.name}=${c.value}`)
            .join('; ');
        
        console.log(`✅ [User ${userId}] Logged in successfully`);
        return { id: userId, authToken, cookies };
    }
    
    return null;
}

function testSSEConnection(user) {
    const url = `${BASE_URL}/api/v1/broker/events/${TEST_EVENT_ID}/tickets/waiting`;
    const params = {
        headers: {
            'Authorization': user.authToken,
            'Cookie': user.cookies,
        },
        tags: { user_id: user.id.toString() }
    };
    
    console.log(`🔗 [User ${user.id}] Connecting to SSE...`);
    
    const response = sse.open(url, params, function (client) {
        client.on('open', function () {
            console.log(`✅ [User ${user.id}] SSE connected`);
        });

        client.on('event', function (event) {
            console.log(event)
            // Skip empty events
            if (!event.data || event.data.trim() === '') {
                return;
            }
            
            sseEventsReceived.add(1);
            console.log(`📨 [User ${user.id}] Event: ${event.data}`);
            
            // Handle JSON events
            if (event.data.startsWith('{')) {
                try {
                    const eventData = JSON.parse(event.data);
                    handleQueueEvent(user, eventData);
                } catch (e) {
                    console.log(`⚠️ [User ${user.id}] Invalid JSON: ${event.data}`);
                }
            }
        });

        client.on('error', function (e) {
            sseConnectionErrors.add(1);
            console.log(`❌ [User ${user.id}] SSE error: ${e.error()}`);
        });
        
        // Close after 20 seconds
        setTimeout(() => {
            console.log(`⏰ [User ${user.id}] Closing SSE connection`);
            client.close();
        }, 20000);
    });

    check(response, {
        [`User ${user.id} SSE connection OK`]: (r) => r && r.status === 200
    });
    
    if (!response || response.status !== 200) {
        sseConnectionErrors.add(1);
        console.error(`❌ [User ${user.id}] SSE failed with status: ${response ? response.status : 'unknown'}`);
    }
}

function handleQueueEvent(user, eventData) {
    const status = eventData.status;
    const order = eventData.order;
    
    if (status === 'WAITING') {
        console.log(`⏳ [User ${user.id}] Waiting in queue - position: ${eventData.position || 'unknown'}`);
    } else if (status === 'IN_ENTRY') {
        console.log(`🎫 [User ${user.id}] In entry queue - order: ${order}`);
    } else if (status === 'IN_PROGRESS') {
        console.log(`🎉 [User ${user.id}] Queue complete! Can purchase tickets`);
    } else {
        console.log(`📋 [User ${user.id}] Status: ${status}`);
    }
}