#!/usr/bin/env node

// 전역 오류 처리
process.on('unhandledRejection', (reason, promise) => {
    if (reason && typeof reason === 'object') {
        if (reason.message?.includes('terminated') || 
            reason.cause?.code === 'UND_ERR_SOCKET' ||
            reason.message?.includes('socket')) {
            // 네트워크 연결 종료 관련 오류는 조용히 처리
            console.log('네트워크 연결 종료로 인한 unhandled rejection (정상 처리됨)');
            return;
        }
    }
    console.error('Unhandled Rejection at:', promise, 'reason:', reason);
});

process.on('uncaughtException', (error) => {
    if (error.message?.includes('terminated') || 
        error.cause?.code === 'UND_ERR_SOCKET' ||
        error.message?.includes('socket')) {
        console.log('네트워크 연결 종료로 인한 uncaught exception (정상 처리됨)');
        return;
    }
    console.error('Uncaught Exception:', error);
    process.exit(1);
});

// Node.js runtime optimizations
require('events').EventEmitter.defaultMaxListeners = 2000;
process.setMaxListeners(2000);

// HTTP 연결 풀 설정 - 대량 요청을 위한 최적화
const http = require('http');
const https = require('https');

// HTTP Agent 전역 설정 - 동시 연결 수 대폭 증가

https.globalAgent.maxSockets = 2000;
https.globalAgent.maxFreeSockets = 500;
https.globalAgent.timeout = 15000;
https.globalAgent.keepAlive = true;

// Node.js v18+ has built-in fetch, fallback to node-fetch for older versions
const fetch = globalThis.fetch || require('node-fetch');

// 설정
const BASE_URL = 'http://localhost:8080';
const DEFAULT_VU_COUNT = 100;
const DEFAULT_EVENT_ID = '0197f929-3fff-7faf-9708-2a7a63f73f1b';
const LOGIN_TIMEOUT = 10000; // 10초
const SSE_TIMEOUT = 600000; // 10분 (더 길게 설정)
const BASE_CONNECTION_HOLD_TIME = 60000; // 기본 60초간 연결 유지
const BATCH_SIZE = 30; // 배치별 동시 연결 수
const BATCH_DELAY = 5000; // 배치 간 5초 대기

// 통계 수집용
const stats = {
    totalUsers: 0,
    successfulLogins: 0,
    successfulConnections: 0,
    activeConnections: 0,
    maxActiveConnections: 0,
    connectionFailures: 0,
    errors: {
        login: 0,
        connection: 0,
        timeout: 0,
        other: 0
    },
    timing: {
        testStart: null,
        testEnd: null,
        connectionTimes: [],
        connectionDurations: []
    },
    connectionEvents: [] // 연결/해제 이벤트 로그
};

// 활성 연결 추적
const activeConnections = new Map();

// 실시간 모니터링을 위한 상태 출력 간격
const MONITORING_INTERVAL = 2000; // 2초마다 상태 출력
let monitoringTimer = null;

/**
 * HTTP 요청을 위한 Promise 래퍼 (Agent 사용)
 */
function makeHttpRequest(url, options) {
    return new Promise((resolve, reject) => {
        const urlObj = new URL(url);
        const isHttps = urlObj.protocol === 'https:';
        const client = isHttps ? https : http;
        
        const requestOptions = {
            hostname: urlObj.hostname,
            port: urlObj.port || (isHttps ? 443 : 80),
            path: urlObj.pathname + urlObj.search,
            method: options.method || 'GET',
            headers: options.headers || {},
            agent: isHttps ? https.globalAgent : http.globalAgent,
            timeout: options.timeout || 15000
        };

        const req = client.request(requestOptions, (res) => {
            let data = '';
            res.on('data', (chunk) => data += chunk);
            res.on('end', () => {
                resolve({
                    ok: res.statusCode >= 200 && res.statusCode < 300,
                    status: res.statusCode,
                    statusText: res.statusMessage,
                    headers: {
                        get: (name) => res.headers[name.toLowerCase()]
                    },
                    text: () => Promise.resolve(data),
                    json: () => Promise.resolve(JSON.parse(data))
                });
            });
        });

        req.on('error', reject);
        req.on('timeout', () => reject(new Error('Request timeout')));
        
        if (options.body) {
            req.write(options.body);
        }
        
        req.end();
    });
}

/**
 * 사용자 로그인
 */
async function loginUser(userId) {
    const email = `user${userId}@ticketon.site`;
    const password = 'password123';

    try {
        const response = await makeHttpRequest(`${BASE_URL}/api/v1/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Connection': 'close'
            },
            body: JSON.stringify({email, password}),
            timeout: LOGIN_TIMEOUT
        });
        if (!response.ok) {
            console.log(`${email} - Response status: ${response.status} ${response.statusText}`);
            stats.errors.login++;
            return null;
        }

        const accessToken = response.headers.get('authorization');
        if (!accessToken) {
            console.log(`${email} - No authorization header found`);
            stats.errors.login++;
            return null;
        }

        const setCookieHeader = response.headers.get('set-cookie');
        let refreshToken = null;
        if (setCookieHeader) {
            // set-cookie 헤더가 배열이나 문자열일 수 있음
            const cookieString = Array.isArray(setCookieHeader) ? setCookieHeader.join('; ') : setCookieHeader;
            const refreshTokenMatch = cookieString.match(/refreshToken=([^;]+)/);
            refreshToken = refreshTokenMatch ? refreshTokenMatch[1] : null;
        }

        if (!refreshToken) {
            console.log(`${email} - No refresh token found`);
            stats.errors.login++;
            return null;
        }

        stats.successfulLogins++;
        return {accessToken, refreshToken};
    } catch (error) {
        console.log(`${email} - Login error:`, error.message);
        if (error.cause) {
            console.log(`${email} - Error cause:`, error.cause.message || error.cause);
        }
        stats.errors.login++;
        return null;
    }
}

/**
 * SSE 연결을 지속적으로 유지 (연결 한계 측정)
 * @param {number} userId - 사용자 ID
 * @param {string} eventId - 이벤트 ID
 * @param {string} accessToken - 액세스 토큰
 * @param {string} refreshToken - 리프레시 토큰
 * @param {number} connectionHoldTime - 동적으로 계산된 연결 유지 시간
 * @param {boolean} waitForInProgress - IN_PROGRESS 상태를 기다릴지 여부 (새 테스트용)
 */
function maintainConnection(userId, eventId, accessToken, refreshToken, connectionHoldTime, waitForInProgress = false) {
    return new Promise((resolve) => {
        const startTime = Date.now();
        let connectionTime = null;
        let reader = null;
        let isResolved = false;

        const sseUrl = `${BASE_URL}/api/v1/broker/events/${eventId}/tickets/waiting`;

        // 타임아웃 설정
        const timeout = setTimeout(() => {
            if (!isResolved) {
                stats.errors.timeout++;
                cleanupConnection(userId, reader);
                resolve({
                    success: false,
                    connectionTime: Date.now() - startTime,
                    duration: 0
                });
            }
        }, SSE_TIMEOUT);

        // 연결 유지 타이머 설정
        const holdTimer = setTimeout(() => {
            if (!isResolved) {
                const duration = Date.now() - startTime;
                cleanupConnection(userId, reader);
                clearTimeout(timeout);
                
                isResolved = true;
                resolve({
                    success: true,
                    connectionTime: connectionTime || 0,
                    duration: duration
                });
            }
        }, connectionHoldTime);

        // fetch를 사용한 SSE 연결 (오류 처리 강화)
        fetch(sseUrl, {
            method: 'GET',
            headers: {
                'Authorization': accessToken,
                'Accept': 'text/event-stream',
                'Cookie': `refreshToken=${refreshToken}`,
                'Cache-Control': 'no-cache'
            },
            signal: null // AbortController를 명시적으로 사용하지 않음
        }).then(response => {
            if (!response.ok) {
                const duration = Date.now() - startTime;
                stats.errors.connection++;
                stats.connectionFailures++;
                clearTimeout(timeout);
                clearTimeout(holdTimer);
                
                if (!isResolved) {
                    isResolved = true;
                    resolve({
                        success: false,
                        connectionTime: duration,
                        duration: 0
                    });
                }
                return;
            }

            connectionTime = Date.now() - startTime;
            
            // 활성 연결 추가
            stats.successfulConnections++;
            stats.activeConnections++;
            stats.maxActiveConnections = Math.max(stats.maxActiveConnections, stats.activeConnections);
            activeConnections.set(userId, { startTime: Date.now(), response });
            
            // 연결 이벤트 로그
            stats.connectionEvents.push({
                timestamp: Date.now(),
                userId: userId,
                event: 'CONNECTED',
                activeCount: stats.activeConnections
            });

            reader = response.body.getReader();
            const decoder = new TextDecoder();
            let buffer = '';

            function readStream() {
                if (isResolved) return;
                
                return reader.read().then(({done, value}) => {
                    if (done || isResolved) {
                        cleanupConnection(userId, reader);
                        return;
                    }

                    const chunk = decoder.decode(value, {stream: true});
                    buffer += chunk;
                    
                    // SSE 메시지는 \n\n으로 구분됨
                    const messages = buffer.split('\n\n');
                    buffer = messages.pop() || '';
                    
                    for (const message of messages) {
                        if (message.trim()) {
                            processSSEMessage(message.trim());
                        }
                    }

                    return readStream();
                    
                    function processSSEMessage(message) {
                        // 연결이 종료된 상태면 처리하지 않음
                        if (isResolved) return;
                        
                        // JSON 메시지 파싱 시도 (상태 확인용)
                        try {
                            const lines = message.split('\n');
                            for (const line of lines) {
                                if (line.startsWith('data:')) {
                                    const data = line.substring(5).trim();
                                    if (data && data !== '' && !data.includes('sse 연결 성공')) {
                                        const parsedData = JSON.parse(data);
                                        
                                        // 새 테스트용: IN_PROGRESS 상태 감지
                                        if (waitForInProgress && parsedData.status === 'IN_PROGRESS') {
                                            console.log(`user${userId} - IN_PROGRESS 상태 감지, 연결 종료`);
                                            
                                            const duration = Date.now() - startTime;
                                            cleanupConnection(userId, reader);
                                            clearTimeout(timeout);
                                            clearTimeout(holdTimer);
                                            
                                            if (!isResolved) {
                                                isResolved = true;
                                                resolve({
                                                    success: true,
                                                    connectionTime: connectionTime || 0,
                                                    duration: duration,
                                                    exitReason: 'IN_PROGRESS'
                                                });
                                            }
                                            return;
                                        }
                                    }
                                }
                            }
                        } catch (parseError) {
                            // JSON 파싱 실패는 무시 (연결 유지가 목적)
                        }
                    }
                }).catch(error => {
                    if (!isResolved) {
                        // 연결 종료 오류 처리 (terminated, socket closed 등)
                        if (error.message.includes('terminated') || 
                            error.message.includes('socket') || 
                            error.cause?.code === 'UND_ERR_SOCKET') {
                            console.log(`user${userId} - 연결이 서버에 의해 종료됨`);
                        } else {
                            console.log(`user${userId} - 스트림 읽기 오류:`, error.message);
                        }
                        
                        stats.errors.connection++;
                        cleanupConnection(userId, reader);
                        clearTimeout(timeout);
                        clearTimeout(holdTimer);
                        
                        isResolved = true;
                        resolve({
                            success: false,
                            connectionTime: connectionTime || (Date.now() - startTime),
                            duration: 0,
                            exitReason: 'CONNECTION_ERROR'
                        });
                    }
                });
            }

            readStream();

        }).catch(error => {
            const duration = Date.now() - startTime;
            stats.errors.connection++;
            stats.connectionFailures++;
            clearTimeout(timeout);
            clearTimeout(holdTimer);
            
            // fetch 레벨 오류 처리
            if (error.message.includes('terminated') || 
                error.message.includes('socket') || 
                error.cause?.code === 'UND_ERR_SOCKET') {
                console.log(`user${userId} - fetch 연결이 종료됨 (서버 연결 끊김)`);
            } else {
                console.log(`user${userId} - fetch 오류:`, error.message);
            }
            
            if (!isResolved) {
                isResolved = true;
                resolve({
                    success: false,
                    connectionTime: duration,
                    duration: 0,
                    exitReason: 'FETCH_ERROR'
                });
            }
        });

        async function cleanupConnection(userId, reader) {
            // 1. 먼저 백엔드에 대기열 종료 API 호출 (안전한 처리)
            try {
                await makeHttpRequest(`${BASE_URL}/api/v1/broker/events/${eventId}/tickets/disconnect`, {
                    method: 'POST',
                    headers: {
                        'Authorization': accessToken,
                        'Cookie': `refreshToken=${refreshToken}`,
                        'Connection': 'close'
                    },
                    timeout: 5000
                });
            } catch (error) {
                // 모든 종류의 네트워크 오류를 안전하게 처리
                const errorMsg = error.message || 'Unknown error';
                const errorCode = error.code || error.cause?.code || 'Unknown';
                
                if (errorMsg.includes('terminated') || 
                    errorMsg.includes('socket') || 
                    errorMsg.includes('ECONNRESET') ||
                    errorMsg.includes('EPIPE') ||
                    errorCode === 'UND_ERR_SOCKET' ||
                    errorCode === 'ECONNRESET') {
                    // 네트워크 연결 종료 관련 오류는 조용히 처리
                    console.log(`user${userId} - Disconnect API: 네트워크 연결 종료`);
                } else {
                    console.log(`user${userId} - Disconnect API error: ${errorMsg} (${errorCode})`);
                }
            }
            
            // 2. 로컬 연결 정리
            if (activeConnections.has(userId)) {
                activeConnections.delete(userId);
                stats.activeConnections--;
                
                // 연결 해제 이벤트 로그
                stats.connectionEvents.push({
                    timestamp: Date.now(),
                    userId: userId,
                    event: 'DISCONNECTED',
                    activeCount: stats.activeConnections
                });
            }
            
            // 3. Reader 정리
            if (reader) {
                try {
                    reader.cancel('Connection cleanup');
                } catch (e) {
                    // reader가 이미 closed되었거나 취소되었을 수 있음
                    // 이 오류는 정상적인 정리 과정에서 발생할 수 있음
                }
            }
        }
    });
}

/**
 * 연결 한계 분석
 */
function analyzeConnectionLimit() {
    console.log('\n📊 연결 한계 분석');
    console.log('='.repeat(50));
    
    const testDuration = (stats.timing.testEnd - stats.timing.testStart) / 1000;
    console.log(`총 테스트 시간: ${testDuration.toFixed(2)}초`);
    console.log(`최대 동시 연결수: ${stats.maxActiveConnections}`);
    console.log(`성공한 연결: ${stats.successfulConnections}`);
    console.log(`연결 실패: ${stats.connectionFailures}`);
    
    if (stats.timing.connectionTimes.length > 0) {
        const avgConnectionTime = stats.timing.connectionTimes.reduce((a, b) => a + b, 0) / stats.timing.connectionTimes.length;
        const minConnectionTime = Math.min(...stats.timing.connectionTimes);
        const maxConnectionTime = Math.max(...stats.timing.connectionTimes);
        
        console.log(`\n📈 연결 시간 통계:`);
        console.log(`  평균: ${avgConnectionTime.toFixed(2)}ms`);
        console.log(`  최소: ${minConnectionTime}ms`);
        console.log(`  최대: ${maxConnectionTime}ms`);
    }
    
    if (stats.timing.connectionDurations.length > 0) {
        const avgDuration = stats.timing.connectionDurations.reduce((a, b) => a + b, 0) / stats.timing.connectionDurations.length;
        console.log(`평균 연결 유지 시간: ${avgDuration.toFixed(2)}ms`);
    }
    
    // 연결 성공률 분석
    const connectionSuccessRate = (stats.successfulConnections / stats.totalUsers) * 100;
    console.log(`\n📊 연결 성공률: ${connectionSuccessRate.toFixed(1)}%`);
    
    if (stats.connectionFailures > 0) {
        console.log(`⚠️  연결 실패가 ${stats.connectionFailures}건 발생했습니다.`);
        console.log(`   이는 시스템의 동시 연결 한계에 근접했음을 의미할 수 있습니다.`);
    }
    
    return {
        maxConcurrentConnections: stats.maxActiveConnections,
        connectionSuccessRate,
        avgConnectionTime: stats.timing.connectionTimes.length > 0 
            ? stats.timing.connectionTimes.reduce((a, b) => a + b, 0) / stats.timing.connectionTimes.length 
            : 0,
        totalConnectionFailures: stats.connectionFailures
    };
}

/**
 * 실시간 연결 상태 모니터링 시작
 */
function startConnectionMonitoring() {
    if (monitoringTimer) return;
    
    monitoringTimer = setInterval(() => {
        const now = new Date().toLocaleTimeString();
        console.log(`[${now}] 🔗 활성 연결: ${stats.activeConnections}명 | 최대: ${stats.maxActiveConnections}명 | 실패: ${stats.connectionFailures}건`);
    }, MONITORING_INTERVAL);
}

/**
 * 실시간 연결 상태 모니터링 중지
 */
function stopConnectionMonitoring() {
    if (monitoringTimer) {
        clearInterval(monitoringTimer);
        monitoringTimer = null;
    }
}

/**
 * 배치별 연결 테스트 실행
 */
async function runConnectionLimitTest() {
    const args = process.argv.slice(2);
    const vuCount = parseInt(args[0]) || DEFAULT_VU_COUNT;
    const eventId = args[1] || DEFAULT_EVENT_ID;

    // 동적 연결 유지 시간 계산
    const totalBatches = Math.ceil(vuCount / BATCH_SIZE);
    const lastBatchStartTime = (totalBatches - 1) * BATCH_DELAY;
    const dynamicConnectionHoldTime = BASE_CONNECTION_HOLD_TIME + lastBatchStartTime + 10000; // 마지막 배치 + 여유시간 10초
    
    console.log('🚀 SSE 동시 연결 한계 테스트 시작');
    console.log(`📊 설정: VU=${vuCount}, EventID=${eventId}`);
    console.log(`⏱️  기본 연결 유지 시간: ${BASE_CONNECTION_HOLD_TIME / 1000}초`);
    console.log(`🎯 동적 연결 유지 시간: ${dynamicConnectionHoldTime / 1000}초 (모든 VU 동시 유지 보장)`);
    console.log(`📦 배치 크기: ${BATCH_SIZE}, 배치 간 대기: ${BATCH_DELAY / 1000}초, 총 ${totalBatches}개 배치`);
    console.log('\n💡 사용법:');
    console.log('  node sse-connection-limit-test.js [VU수] [이벤트ID] [테스트타입]');
    console.log('  테스트타입: 1=기존테스트(기본값), 2=점진적재연결테스트');
    console.log('='.repeat(60));

    stats.totalUsers = vuCount;
    stats.timing.testStart = Date.now();

    // 1단계: 모든 사용자 로그인 (순차 처리)
    console.log('\n📝 [1단계] 사용자 로그인 (순차 처리)...');
    
    const validUsers = [];
    let loginSuccessCount = 0;
    let loginFailureCount = 0;
    
    for (let userId = 1; userId <= vuCount; userId++) {
        const result = await loginUser(userId);
        
        if (result) {
            validUsers.push({
                userId: userId,
                ...result
            });
            loginSuccessCount++;
        } else {
            loginFailureCount++;
        }
        
        // 100명마다 진행 상황 출력
        if (userId % 100 === 0 || userId === vuCount) {
            console.log(`  📊 ${userId}/${vuCount} 완료: 성공 ${loginSuccessCount}명, 실패 ${loginFailureCount}명`);
        }
        
        // 1000명마다 1초 대기
        if (userId % 1000 === 0 && userId < vuCount) {
            console.log(`  ⏳ ${userId}명 완료, 1초 대기...`);
            await new Promise(resolve => setTimeout(resolve, 1000));
        }
    }

    console.log(`\n✅ [1단계] 로그인 완료: 성공 ${loginSuccessCount}/${vuCount}명 (실패: ${loginFailureCount}명)`);

    if (validUsers.length === 0) {
        console.error('❌ 로그인한 사용자가 없어 테스트를 종료합니다.');
        return;
    }

    // 2단계: 테스트 케이스 선택
    console.log('\n🔀 [2단계] 테스트 케이스 선택');
    console.log('1. 기존 테스트: 배치별 동시 연결 테스트 (지속 연결)');
    console.log('2. 새 테스트: 점진적 연결/해제 반복 테스트 (10초 간격)');
    
    // 인수로 테스트 타입 선택 (기본값: 1)
    const testType = parseInt(args[2]) || 1;
    console.log(`선택된 테스트: ${testType === 1 ? '기존 테스트' : '새 테스트'}`);
    
    if (testType === 1) {
        return await runTraditionalTest(validUsers, eventId, dynamicConnectionHoldTime);
    } else {
        return await runProgressiveReconnectTest(validUsers, eventId);
    }
}

/**
 * 기존 테스트: 배치별 동시 연결 테스트
 */
async function runTraditionalTest(validUsers, eventId, dynamicConnectionHoldTime) {
    console.log('\n🔗 [기존 테스트] 배치별 동시 연결 테스트...');
    
    const batches = [];
    for (let i = 0; i < validUsers.length; i += BATCH_SIZE) {
        batches.push(validUsers.slice(i, i + BATCH_SIZE));
    }
    
    console.log(`📦 총 ${batches.length}개 배치로 연결 테스트 진행`);
    console.log(`🎯 최종 목표: 모든 ${validUsers.length}명이 동시에 ${BASE_CONNECTION_HOLD_TIME / 1000}초간 연결 유지\n`);
    
    // 실시간 모니터링 시작
    startConnectionMonitoring();
    
    const allConnectionPromises = [];
    
    for (let batchIndex = 0; batchIndex < batches.length; batchIndex++) {
        const batch = batches[batchIndex];
        
        console.log(`🚀 [배치 ${batchIndex + 1}/${batches.length}] ${batch.length}명 연결 시작`);
        
        const batchPromises = batch.map(user =>
            maintainConnection(user.userId, eventId, user.accessToken, user.refreshToken, dynamicConnectionHoldTime)
        );
        
        allConnectionPromises.push(...batchPromises);
        
        // 마지막 배치가 아니라면 대기
        if (batchIndex < batches.length - 1) {
            await new Promise(resolve => setTimeout(resolve, BATCH_DELAY));
        }
    }
    
    console.log(`\n⏳ 모든 연결 완료까지 대기 중... (예상 시간: ${Math.ceil(dynamicConnectionHoldTime / 1000)}초)`);
    const connectionResults = await Promise.all(allConnectionPromises);
    
    // 모니터링 중지
    stopConnectionMonitoring();
    
    stats.timing.testEnd = Date.now();

    // 결과 수집
    connectionResults.forEach(result => {
        if (result.success) {
            stats.timing.connectionTimes.push(result.connectionTime);
            if (result.duration > 0) {
                stats.timing.connectionDurations.push(result.duration);
            }
        }
    });

    // 3단계: 결과 분석
    console.log('\n📊 [3단계] 결과 분석');
    console.log('='.repeat(50));

    const totalDuration = (stats.timing.testEnd - stats.timing.testStart) / 1000;
    console.log(`총 테스트 시간: ${totalDuration.toFixed(2)}초`);
    console.log(`총 사용자 수: ${stats.totalUsers}`);
    console.log(`성공한 로그인: ${stats.successfulLogins}`);
    console.log(`최종 활성 연결: ${stats.activeConnections}`);
    console.log(`최대 동시 연결: ${stats.maxActiveConnections}`);

    console.log('\n❌ 오류 통계:');
    console.log(`  로그인 오류: ${stats.errors.login}`);
    console.log(`  연결 오류: ${stats.errors.connection}`);
    console.log(`  타임아웃: ${stats.errors.timeout}`);
    console.log(`  기타 오류: ${stats.errors.other}`);

    console.log('\n📈 성공률:');
    console.log(`  로그인 성공률: ${(stats.successfulLogins / stats.totalUsers * 100).toFixed(1)}%`);
    const connectionSuccessRate = (stats.successfulConnections / stats.successfulLogins * 100);
    console.log(`  연결 성공률: ${connectionSuccessRate.toFixed(1)}%`);

    // 연결 한계 분석
    const limitAnalysis = analyzeConnectionLimit();

    // 최종 결론
    console.log('\n🎯 최종 결과:');
    console.log(`🔗 최대 동시 연결수: ${limitAnalysis.maxConcurrentConnections}`);
    console.log(`📊 연결 성공률: ${limitAnalysis.connectionSuccessRate.toFixed(1)}%`);
    
    if (limitAnalysis.totalConnectionFailures === 0) {
        console.log(`✅ ${vuCount}명 모두 성공적으로 연결되었습니다.`);
        console.log(`💡 더 높은 VU로 테스트하여 실제 한계를 찾아보세요.`);
    } else {
        console.log(`⚠️  ${limitAnalysis.totalConnectionFailures}건의 연결 실패가 발생했습니다.`);
        console.log(`📈 시스템의 동시 연결 한계에 근접한 것으로 보입니다.`);
    }
    
    console.log(`\n🕐 동시 유지 구간: 마지막 배치 연결 후 약 ${BASE_CONNECTION_HOLD_TIME / 1000}초간 모든 VU가 동시 연결 상태`);

    console.log('\n🏁 연결 한계 테스트 완료');

    return {
        maxConcurrentConnections: limitAnalysis.maxConcurrentConnections,
        connectionSuccessRate: limitAnalysis.connectionSuccessRate,
        totalUsers: stats.totalUsers,
        testDuration: totalDuration,
        connectionFailures: limitAnalysis.totalConnectionFailures
    };
}

/**
 * 새 테스트: 점진적 연결/해제 반복 테스트
 */
async function runProgressiveReconnectTest(validUsers, eventId) {
    console.log('\n🔄 [새 테스트] 점진적 연결/해제 반복 테스트...');
    console.log('📋 테스트 방식: 대기열 진입 → IN_PROGRESS 감지 → 해제 → 1초 대기 → 재진입 반복');
    console.log('📈 점진적 증가: 매 라운드마다 VU 수 증가');
    
    const MAX_TIMEOUT = 120000; // 최대 2분 대기 (IN_PROGRESS까지)
    const MAX_ROUNDS = 10; // 최대 10라운드
    const USERS_PER_ROUND = Math.max(1, Math.floor(validUsers.length / MAX_ROUNDS)); // 라운드당 사용자 수
    
    console.log(`⚙️  설정: 라운드당 ${USERS_PER_ROUND}명, 최대 ${MAX_ROUNDS}라운드`);
    console.log(`⏱️  각 연결: 대기열 진입 → IN_PROGRESS 감지 → 해제 → 1초 대기 → 재진입\n`);
    
    // 실시간 모니터링 시작
    startConnectionMonitoring();
    
    const reconnectStats = {
        totalReconnections: 0,
        successfulReconnections: 0,
        failedReconnections: 0,
        maxConcurrentInRound: 0,
        roundResults: []
    };
    
    for (let round = 1; round <= MAX_ROUNDS; round++) {
        const usersInThisRound = validUsers.slice(0, round * USERS_PER_ROUND);
        console.log(`\n🎯 [라운드 ${round}/${MAX_ROUNDS}] ${usersInThisRound.length}명 시작`);
        
        // 라운드 시작 시간
        const roundStartTime = Date.now();
        
        // 모든 사용자가 동시에 연결/해제를 3번 반복 (IN_PROGRESS 기반)
        const reconnectPromises = usersInThisRound.map(user => 
            performReconnectCycle(user, eventId, 3, MAX_TIMEOUT)
        );
        
        const roundResults = await Promise.all(reconnectPromises);
        
        // 라운드 결과 집계
        const roundDuration = (Date.now() - roundStartTime) / 1000;
        const successfulInRound = roundResults.filter(r => r.success).length;
        const totalConnectionsInRound = roundResults.reduce((sum, r) => sum + r.totalConnections, 0);
        const successfulConnectionsInRound = roundResults.reduce((sum, r) => sum + r.successfulConnections, 0);
        const totalInProgressInRound = roundResults.reduce((sum, r) => sum + (r.inProgressCount || 0), 0);
        
        reconnectStats.totalReconnections += totalConnectionsInRound;
        reconnectStats.successfulReconnections += successfulConnectionsInRound;
        reconnectStats.failedReconnections += (totalConnectionsInRound - successfulConnectionsInRound);
        reconnectStats.maxConcurrentInRound = Math.max(reconnectStats.maxConcurrentInRound, stats.maxActiveConnections);
        
        reconnectStats.roundResults.push({
            round: round,
            users: usersInThisRound.length,
            duration: roundDuration,
            successfulUsers: successfulInRound,
            totalConnections: totalConnectionsInRound,
            successfulConnections: successfulConnectionsInRound,
            maxConcurrent: stats.maxActiveConnections
        });
        
        console.log(`✅ 라운드 ${round} 완료: ${successfulInRound}/${usersInThisRound.length}명 성공`);
        console.log(`   총 연결: ${successfulConnectionsInRound}/${totalConnectionsInRound}, IN_PROGRESS: ${totalInProgressInRound}회, 시간: ${roundDuration.toFixed(1)}초`);
        
        // 라운드 간 5초 대기 (서버 부하 완화)
        if (round < MAX_ROUNDS) {
            console.log(`⏳ 다음 라운드까지 5초 대기 (서버 리소스 정리)...`);
            await new Promise(resolve => setTimeout(resolve, 5000));
        }
    }
    
    // 모니터링 중지
    stopConnectionMonitoring();
    
    stats.timing.testEnd = Date.now();
    
    // 최종 결과 출력
    console.log('\n📊 [점진적 재연결 테스트] 결과 분석');
    console.log('='.repeat(60));
    
    const totalTestDuration = (stats.timing.testEnd - stats.timing.testStart) / 1000;
    console.log(`총 테스트 시간: ${totalTestDuration.toFixed(2)}초`);
    console.log(`총 재연결 횟수: ${reconnectStats.totalReconnections}`);
    console.log(`성공한 재연결: ${reconnectStats.successfulReconnections}`);
    console.log(`실패한 재연결: ${reconnectStats.failedReconnections}`);
    console.log(`최대 동시 연결: ${stats.maxActiveConnections}`);
    console.log(`재연결 성공률: ${(reconnectStats.successfulReconnections / reconnectStats.totalReconnections * 100).toFixed(1)}%`);
    
    console.log('\n📈 라운드별 상세 결과:');
    reconnectStats.roundResults.forEach(result => {
        console.log(`  라운드 ${result.round}: ${result.users}명, 연결 ${result.successfulConnections}/${result.totalConnections}, 최대동시 ${result.maxConcurrent}`);
    });
    
    console.log('\n🎯 최종 결론:');
    if (reconnectStats.failedReconnections === 0) {
        console.log(`✅ 모든 재연결이 성공했습니다. 시스템이 안정적으로 작동합니다.`);
    } else {
        console.log(`⚠️  ${reconnectStats.failedReconnections}건의 재연결 실패가 발생했습니다.`);
        console.log(`📊 실패 발생 지점을 분석하여 시스템 한계를 파악하세요.`);
    }
    
    console.log('\n🏁 점진적 재연결 테스트 완료');
    
    return {
        testType: 'progressive_reconnect',
        maxConcurrentConnections: stats.maxActiveConnections,
        totalReconnections: reconnectStats.totalReconnections,
        successfulReconnections: reconnectStats.successfulReconnections,
        reconnectSuccessRate: (reconnectStats.successfulReconnections / reconnectStats.totalReconnections * 100),
        testDuration: totalTestDuration,
        roundResults: reconnectStats.roundResults
    };
}

/**
 * 개별 사용자의 연결/해제 반복 사이클 (IN_PROGRESS 기반)
 */
async function performReconnectCycle(user, eventId, cycles, maxTimeout) {
    let totalConnections = 0;
    let successfulConnections = 0;
    let inProgressCount = 0; // IN_PROGRESS 도달 횟수
    
    try {
        for (let cycle = 1; cycle <= cycles; cycle++) {
            totalConnections++;
            console.log(`user${user.userId} - 사이클 ${cycle}/${cycles} 시작: 대기열 진입`);
            
            // IN_PROGRESS 상태를 기다리는 연결 시도
            const connectionResult = await maintainConnection(
                user.userId, 
                eventId, 
                user.accessToken, 
                user.refreshToken, 
                maxTimeout,
                true // waitForInProgress = true
            );
            
            if (connectionResult.success) {
                successfulConnections++;
                
                if (connectionResult.exitReason === 'IN_PROGRESS') {
                    inProgressCount++;
                    console.log(`user${user.userId} - 사이클 ${cycle} 완료: IN_PROGRESS 도달 후 해제`);
                } else {
                    console.log(`user${user.userId} - 사이클 ${cycle} 완료: 시간 초과로 해제`);
                }
            } else {
                console.log(`user${user.userId} - 사이클 ${cycle} 실패: 연결 오류`);
            }
            
            // 사이클 간 2초 대기 (서버 리소스 정리 시간 확보)
            if (cycle < cycles) {
                console.log(`user${user.userId} - 2초 대기 후 재진입...`);
                await new Promise(resolve => setTimeout(resolve, 2000));
            }
        }
        
        console.log(`user${user.userId} - 모든 사이클 완료: ${inProgressCount}/${cycles}회 IN_PROGRESS 도달`);
        
        return {
            success: true,
            totalConnections,
            successfulConnections,
            inProgressCount,
            userId: user.userId
        };
    } catch (error) {
        console.log(`user${user.userId} - Reconnect cycle error:`, error.message);
        return {
            success: false,
            totalConnections,
            successfulConnections,
            inProgressCount: 0,
            userId: user.userId
        };
    }
}

// 프로그램 시작
if (require.main === module) {
    runConnectionLimitTest().catch(error => {
        console.error('💥 테스트 실행 중 치명적 오류:', error);
        process.exit(1);
    });
}

module.exports = {
    loginUser,
    maintainConnection,
    runConnectionLimitTest,
    analyzeConnectionLimit
};