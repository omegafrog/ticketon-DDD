#!/usr/bin/env node

// Node.js Queue Order Test - Alternative to k6 test
// Tests 10 users entering queue sequentially and checks promotion order

import fetch from 'node-fetch';
import EventSource from 'eventsource';

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
    entryTimestamps: new Map(),
    promotionTimestamps: new Map(),
    orderViolations: []
};

console.log(`🚀 [TEST START] Testing queue order with ${USER_COUNT} users for event ${TEST_EVENT_ID}`);

async function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

async function loginUsers() {
    console.log('📝 [PHASE 1] Starting user login process...');
    const users = [];
    
    for (let i = 1; i <= USER_COUNT; i++) {
        const email = `queuetest${i}@ticketon.com`;
        const password = 'password123';
        
        try {
            console.log(`🔐 [LOGIN] Attempting login for user ${i}: ${email}`);
            
            const response = await fetch(`${BASE_URL}/api/v1/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });
            
            if (response.ok && response.headers.get('Authorization')) {
                const authToken = response.headers.get('Authorization');
                const cookies = response.headers.get('set-cookie') || '';
                
                users.push({
                    id: i,
                    email: email,
                    authToken: authToken,
                    cookies: cookies
                });
                
                testResults.usersLoggedIn++;
                console.log(`✅ [LOGIN SUCCESS] User ${i} logged in successfully`);
            } else {
                console.error(`❌ [LOGIN FAILED] User ${i} login failed. Status: ${response.status}`);
            }
        } catch (error) {
            console.error(`❌ [LOGIN ERROR] User ${i}: ${error.message}`);
        }
        
        await sleep(100); // Small delay between logins
    }
    
    console.log(`📝 [PHASE 1 COMPLETE] Successfully logged in ${users.length}/${USER_COUNT} users`);
    return users;
}

async function enterQueueAndMonitor(users) {
    console.log('🎯 [PHASE 2] Starting queue entry process...');
    
    const promises = users.map(async (user, index) => {
        const entryTimestamp = Date.now();
        entryOrder.push(user.id);
        testResults.entryTimestamps.set(user.id, entryTimestamp);
        
        console.log(`⏰ [QUEUE ENTRY] User ${user.id} entering queue at ${new Date(entryTimestamp).toISOString()}`);
        
        return new Promise((resolve) => {
            const sseUrl = `${BASE_URL}/api/v1/broker/events/${TEST_EVENT_ID}/tickets/waiting`;
            const eventSource = new EventSource(sseUrl, {
                headers: {
                    'Authorization': user.authToken,
                    'Cookie': user.cookies
                }
            });
            
            let hasEnteredQueue = false;
            let isResolved = false;
            
            // Set timeout to resolve after 60 seconds
            const timeout = setTimeout(() => {
                if (!isResolved) {
                    console.log(`⏱️  [TIMEOUT] User ${user.id} test timeout after 60s`);
                    eventSource.close();
                    isResolved = true;
                    resolve();
                }
            }, 60000);
            
            eventSource.onmessage = function(event) {
                try {
                    const message = JSON.parse(event.data);
                    
                    // User successfully entered waiting queue
                    if (message.status === 'WAITING' && !hasEnteredQueue) {
                        hasEnteredQueue = true;
                        testResults.usersEnteredQueue++;
                        console.log(`🎫 [QUEUE ENTERED] User ${user.id} entered waiting queue (position: ${message.position || 'unknown'})`);
                    }
                    
                    // User quit waiting queue (promoted to entry queue)
                    if (message.status === 'IN_ENTRY') {
                        const promotionTimestamp = Date.now();
                        promotionOrder.push(user.id);
                        testResults.promotionTimestamps.set(user.id, promotionTimestamp);
                        testResults.usersPromoted++;
                        
                        const waitTime = promotionTimestamp - entryTimestamp;
                        console.log(`🎉 [QUIT QUEUE] User ${user.id} quit waiting queue at ${new Date(promotionTimestamp).toISOString()} (waited ${waitTime}ms)`);
                        
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
                            console.error(`🚨 [ORDER VIOLATION] User ${user.id} expected position ${expectedPosition}, got ${actualPosition}`);
                        }
                        
                        clearTimeout(timeout);
                        eventSource.close();
                        if (!isResolved) {
                            isResolved = true;
                            resolve();
                        }
                    }
                } catch (e) {
                    // Ignore parsing errors for heartbeat/connect events
                }
            };
            
            eventSource.onerror = function(error) {
                console.error(`❌ [SSE ERROR] User ${user.id}:`, error);
                if (error.type === 'error' && error.status === 401) {
                    console.error(`🔐 [AUTH ERROR] User ${user.id} authentication failed`);
                    clearTimeout(timeout);
                    eventSource.close();
                    if (!isResolved) {
                        isResolved = true;
                        resolve();
                    }
                }
            };
        });
    });
    
    // Wait for all users to complete (or timeout)
    await Promise.all(promises);
    
    console.log(`🎯 [PHASE 2 COMPLETE] ${testResults.usersEnteredQueue} users entered queue`);
}

function analyzeResults() {
    console.log('\n📊 [PHASE 3] ANALYZING RESULTS...');
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
        console.log(`\n🚨 ORDER VIOLATIONS (${testResults.orderViolations.length}):`);
        testResults.orderViolations.forEach(violation => {
            console.log(`  User ${violation.userId}: Expected pos ${violation.expectedPosition}, got ${violation.actualPosition}`);
        });
    } else if (promotionOrder.length > 0) {
        console.log('\n✅ No order violations detected');
    }
    
    // Calculate promotion times
    if (promotionOrder.length > 0) {
        console.log('\n📈 PROMOTION TIMING:');
        promotionOrder.forEach(userId => {
            const entryTime = testResults.entryTimestamps.get(userId);
            const promotionTime = testResults.promotionTimestamps.get(userId);
            const waitTime = promotionTime - entryTime;
            console.log(`  User ${userId}: ${waitTime}ms wait time`);
        });
    }
    
    console.log('\n=====================================');
    console.log('🏁 [TEST COMPLETE]');
    
    // Final summary
    if (testResults.orderViolations.length === 0 && testResults.usersPromoted > 0) {
        console.log('🎉 RESULT: User queue ordering is working correctly');
    } else if (testResults.usersPromoted === 0) {
        console.log('⚠️  RESULT: No users were promoted during test');
    } else {
        console.log('🚨 RESULT: User queue ordering has violations - needs investigation');
    }
}

// Main test execution
async function runTest() {
    try {
        // Phase 1: Login all users
        const users = await loginUsers();
        if (users.length === 0) {
            console.error('❌ No users successfully logged in. Exiting test.');
            return;
        }
        
        // Phase 2: Enter queue and monitor
        await enterQueueAndMonitor(users);
        
        // Phase 3: Analyze results
        analyzeResults();
        
    } catch (error) {
        console.error('❌ [TEST ERROR]:', error);
    }
}

// Run the test
runTest();