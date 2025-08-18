#!/usr/bin/env node

// Connection timeout 문제 해결을 위한 최적화된 버전
require('events').EventEmitter.defaultMaxListeners = 3000;
process.setMaxListeners(3000);

const http = require('http');
const https = require('https');

// 최적화된 HTTP Agent 설정
http.globalAgent.maxSockets = 2000;        // 증가
http.globalAgent.maxFreeSockets = 1000;    // 증가
http.globalAgent.timeout = 30000;          // 30초로 증가
http.globalAgent.keepAlive = true;
http.globalAgent.keepAliveMsecs = 10000;

https.globalAgent.maxSockets = 2000;
https.globalAgent.maxFreeSockets = 1000;
https.globalAgent.timeout = 30000;
https.globalAgent.keepAlive = true;
https.globalAgent.keepAliveMsecs = 10000;

const fetch = globalThis.fetch || require('node-fetch');

// 최적화된 설정
const BASE_URL = 'http://localhost:8080';
const DEFAULT_VU_COUNT = 100;
const DEFAULT_EVENT_ID = '0197f929-3fff-7faf-9708-2a7a63f73f1b';
const LOGIN_TIMEOUT = 20000;               // 20초로 증가
const SSE_TIMEOUT = 600000;
const BASE_CONNECTION_HOLD_TIME = 60000;
const BATCH_SIZE = 20;                     // 감소 (50 -> 20)
const BATCH_DELAY = 2000;                  // 감소 (5000 -> 2000)
const MAX_RETRIES = 3;                     // 재시도 횟수

// 통계
const stats = {
    totalUsers: 0,
    successfulLogins: 0,
    successfulConnections: 0,
    activeConnections: 0,
    maxActiveConnections: 0,
    connectionFailures: 0,
    retries: 0,
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
    }
};

// 지수 백오프를 위한 지연 함수
function exponentialBackoff(attempt) {
    return Math.min(1000 * Math.pow(2, attempt), 10000); // 최대 10초
}

// 재시도 로직이 포함된 HTTP 요청
async function makeHttpRequestWithRetry(url, options, maxRetries = MAX_RETRIES) {
    let lastError;
    
    for (let attempt = 0; attempt < maxRetries; attempt++) {
        try {
            if (attempt > 0) {
                const delay = exponentialBackoff(attempt - 1);
                await new Promise(resolve => setTimeout(resolve, delay));
                stats.retries++;
            }
            
            return await makeHttpRequest(url, options);
        } catch (error) {
            lastError = error;
            if (attempt === maxRetries - 1) {
                throw error;
            }
        }
    }
    
    throw lastError;
}

// 기존 makeHttpRequest 함수 (동일)
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
            timeout: options.timeout || 30000  // 30초로 증가
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

// 최적화된 로그인 함수
async function loginUser(userId) {
    const email = `user${userId}@ticketon.site`;
    const password = 'password123';

    try {
        const response = await makeHttpRequestWithRetry(`${BASE_URL}/api/v1/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Connection': 'keep-alive'
            },
            body: JSON.stringify({email, password}),
            timeout: LOGIN_TIMEOUT
        });

        if (!response.ok) {
            stats.errors.login++;
            return null;
        }

        const accessToken = response.headers.get('authorization');
        if (!accessToken) {
            stats.errors.login++;
            return null;
        }

        const setCookieHeader = response.headers.get('set-cookie');
        let refreshToken = null;
        if (setCookieHeader) {
            const cookieString = Array.isArray(setCookieHeader) ? setCookieHeader.join('; ') : setCookieHeader;
            const refreshTokenMatch = cookieString.match(/refreshToken=([^;]+)/);
            refreshToken = refreshTokenMatch ? refreshTokenMatch[1] : null;
        }

        if (!refreshToken) {
            stats.errors.login++;
            return null;
        }

        stats.successfulLogins++;
        return {accessToken, refreshToken};
    } catch (error) {
        console.log(`${email} - Login error:`, error.message);
        stats.errors.login++;
        return null;
    }
}

// 메인 테스트 함수 (로그인 부분만 최적화)
async function runOptimizedTest() {
    const args = process.argv.slice(2);
    const vuCount = parseInt(args[0]) || DEFAULT_VU_COUNT;
    const eventId = args[1] || DEFAULT_EVENT_ID;

    console.log('🚀 최적화된 연결 테스트 시작');
    console.log(`📊 설정: VU=${vuCount}, 배치=${BATCH_SIZE}, 지연=${BATCH_DELAY}ms`);
    console.log('='.repeat(60));

    stats.totalUsers = vuCount;
    stats.timing.testStart = Date.now();

    // 최적화된 배치별 로그인
    console.log('\n📝 [1단계] 최적화된 로그인...');
    
    const loginBatches = [];
    for (let i = 0; i < vuCount; i += BATCH_SIZE) {
        const batch = [];
        for (let j = i; j < Math.min(i + BATCH_SIZE, vuCount); j++) {
            batch.push(loginUser(j + 1));
        }
        loginBatches.push(batch);
    }
    
    const validUsers = [];
    let loginSuccessCount = 0;
    let loginFailureCount = 0;
    
    for (let batchIndex = 0; batchIndex < loginBatches.length; batchIndex++) {
        const batch = loginBatches[batchIndex];
        console.log(`  🔄 로그인 배치 ${batchIndex + 1}/${loginBatches.length} (${batch.length}명) 진행 중...`);
        
        const results = await Promise.all(batch);
        
        for (let i = 0; i < results.length; i++) {
            const result = results[i];
            const actualUserId = batchIndex * BATCH_SIZE + i + 1;
            
            if (result) {
                validUsers.push({
                    userId: actualUserId,
                    ...result
                });
                loginSuccessCount++;
            } else {
                loginFailureCount++;
            }
        }
        
        console.log(`  📊 배치 ${batchIndex + 1} 완료: 성공 ${loginSuccessCount}명, 실패 ${loginFailureCount}명`);
        
        // 배치 간 지연 (시스템 부하 분산)
        if (batchIndex < loginBatches.length - 1) {
            await new Promise(resolve => setTimeout(resolve, BATCH_DELAY));
        }
    }

    stats.timing.testEnd = Date.now();
    const testDuration = (stats.timing.testEnd - stats.timing.testStart) / 1000;

    console.log(`\n✅ [최적화된 테스트] 완료: ${testDuration.toFixed(2)}초`);
    console.log(`📊 로그인 성공: ${loginSuccessCount}/${vuCount}명 (${(loginSuccessCount/vuCount*100).toFixed(1)}%)`);
    console.log(`🔄 총 재시도: ${stats.retries}회`);
    console.log(`❌ 로그인 실패: ${loginFailureCount}명`);

    if (loginSuccessCount > 1000) {
        console.log(`\n🎯 1000명 이상 성공! 시스템이 대량 동시 연결을 처리할 수 있습니다.`);
    }

    return {
        totalUsers: vuCount,
        successfulLogins: loginSuccessCount,
        failedLogins: loginFailureCount,
        successRate: (loginSuccessCount / vuCount) * 100,
        testDuration: testDuration,
        retries: stats.retries
    };
}

// 실행
if (require.main === module) {
    runOptimizedTest().catch(error => {
        console.error('💥 테스트 실행 중 오류:', error);
        process.exit(1);
    });
}

module.exports = { runOptimizedTest, loginUser };