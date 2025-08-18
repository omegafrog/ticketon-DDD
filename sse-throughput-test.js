#!/usr/bin/env node

// Node.js v18+ has built-in fetch, fallback to node-fetch for older versions
const fetch = globalThis.fetch || require('node-fetch');
const EventSource = require('eventsource').EventSource || require('eventsource');

// 설정
const BASE_URL = 'http://localhost:8080';
const DEFAULT_VU_COUNT = 50;
const DEFAULT_EVENT_ID = '0197f92b-219e-7a57-b11f-0a356687457f';
const LOGIN_TIMEOUT = 10000; // 10초
const SSE_TIMEOUT = 120000; // 120초
const IN_PROGRESS_DELAY = 0; // IN_PROGRESS 후 1초 대기

// 통계 수집용
const stats = {
    totalUsers: 0,
    successfulLogins: 0,
    successfulConnections: 0,
    inProgressReached: 0,
    completedUsers: 0,
    errors: {
        login: 0,
        connection: 0,
        timeout: 0,
        other: 0
    },
    timing: {
        testStart: null,
        testEnd: null,
        firstInProgress: null,
        lastInProgress: null,
        connectionTimes: [],
        processingTimes: []
    }
};

/**
 * 사용자 로그인
 * @param {number} userId - 사용자 ID
 * @returns {Promise<{accessToken: string, refreshToken: string}|null>}
 */
async function loginUser(userId) {
    const email = `user${userId}@ticketon.com`;
    const password = 'password123';

    try {
        console.log(`🔐 [User ${userId}] 로그인 시도: ${email}`);

        const response = await fetch(`${BASE_URL}/api/v1/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({email, password}),
            timeout: LOGIN_TIMEOUT
        });

        if (!response.ok) {
            console.error(`❌ [User ${userId}] 로그인 실패: ${response.status} ${response.statusText}`);
            stats.errors.login++;
            return null;
        }

        // Authorization 헤더에서 accessToken 추출
        const accessToken = response.headers.get('authorization');
        if (!accessToken) {
            console.error(`❌ [User ${userId}] Authorization 헤더가 없음`);
            stats.errors.login++;
            return null;
        }

        // 쿠키에서 refreshToken 추출
        const setCookieHeader = response.headers.get('set-cookie');
        let refreshToken = null;
        if (setCookieHeader) {
            const refreshTokenMatch = setCookieHeader.match(/refreshToken=([^;]+)/);
            refreshToken = refreshTokenMatch ? refreshTokenMatch[1] : null;
        }

        if (!refreshToken) {
            console.error(`❌ [User ${userId}] refreshToken 쿠키가 없음`);
            stats.errors.login++;
            return null;
        }

        console.log(`✅ [User ${userId}] 로그인 성공`);
        stats.successfulLogins++;

        return {accessToken, refreshToken};
    } catch (error) {
        console.error(`❌ [User ${userId}] 로그인 오류:`, error.message);
        stats.errors.login++;
        return null;
    }
}

/**
 * SSE 연결 및 대기열 진입 (처리량 측정)
 * @param {number} userId - 사용자 ID
 * @param {string} eventId - 이벤트 ID
 * @param {string} accessToken - 액세스 토큰
 * @param {string} refreshToken - 리프레시 토큰
 * @returns {Promise<{success: boolean, connectionTime: number, processingTime: number}>}
 */
function connectToQueue(userId, eventId, accessToken, refreshToken) {
    return new Promise((resolve) => {
        const startTime = Date.now();
        let connectionTime = null;
        let inProgressTime = null;

        console.log(`🎫 [User ${userId}] 대기열 진입 시도...`);

        const sseUrl = `${BASE_URL}/api/v1/broker/events/${eventId}/tickets/waiting`;

        // 타임아웃 설정
        const timeout = setTimeout(() => {
            console.error(`⏰ [User ${userId}] SSE 연결 타임아웃`);
            stats.errors.timeout++;
            resolve({
                success: false,
                connectionTime: Date.now() - startTime,
                processingTime: 0
            });
        }, SSE_TIMEOUT);

        // fetch를 사용한 SSE 연결 (쿠키 지원)
        fetch(sseUrl, {
            method: 'GET',
            headers: {
                'Authorization': accessToken,
                'Accept': 'text/event-stream',
                'Cookie': `refreshToken=${refreshToken}`,
                'Cache-Control': 'no-cache'
            }
        }).then(response => {
            if (!response.ok) {
                const duration = Date.now() - startTime;
                console.error(`❌ [User ${userId}] SSE 연결 실패 (${duration}ms): ${response.status} ${response.statusText}`);
                stats.errors.connection++;
                clearTimeout(timeout);
                resolve({
                    success: false,
                    connectionTime: duration,
                    processingTime: 0
                });
                return;
            }

            connectionTime = Date.now() - startTime;
            console.log(`🔗 [User ${userId}] SSE 연결 성공 (${connectionTime}ms)`);
            stats.successfulConnections++;

            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let buffer = ''; // SSE 메시지 버퍼

            function readStream() {
                return reader.read().then(({done, value}) => {
                    if (done) {
                        console.log(`🏁 [User ${userId}] 스트림 종료`);
                        clearTimeout(timeout);
                        resolve({
                            success: false,
                            connectionTime: connectionTime,
                            processingTime: 0
                        });
                        return;
                    }

                    const chunk = decoder.decode(value, {stream: true});
                    buffer += chunk;
                    
                    // SSE 메시지는 \n\n으로 구분됨
                    const messages = buffer.split('\n\n');
                    
                    // 마지막 메시지는 불완전할 수 있으므로 버퍼에 보관
                    buffer = messages.pop() || '';
                    
                    for (const message of messages) {
                        if (message.trim()) {
                            processSSEMessage(message.trim());
                        }
                    }

                    return readStream();
                    
                    function processSSEMessage(message) {
                        console.log(`📨 [User ${userId}] 수신:`, message);
                        
                        // SSE 형식 파싱: data: 로 시작하는 라인 찾기
                        const lines = message.split('\n');
                        for (const line of lines) {
                            if (line.startsWith('data:')) {
                                const data = line.substring(5).trim();
                                
                                if (data && data !== '') {
                                    // 연결 성공 메시지 확인
                                    if (data.includes('sse 연결 성공')) {
                                        console.log(`✅ [User ${userId}] 대기열 진입 완료`);
                                        continue;
                                    }

                                    // JSON 메시지 파싱 시도
                                    try {
                                        const messageData = JSON.parse(data);
                                        console.log(`[User ${userId}] 파싱된 데이터:`, messageData);
                                        
                                        if (messageData.status) {
                                            console.log(`📊 [User ${userId}] 상태: ${messageData.status}`);

                                            if (messageData.status === 'IN_PROGRESS') {
                                                inProgressTime = Date.now();
                                                const processingTime = inProgressTime - startTime;

                                                console.log(`🎉 [User ${userId}] IN_PROGRESS 도달! (${processingTime}ms)`);
                                                console.log(`📞 [User ${userId}] disconnect API 호출 중...`);

                                                // 통계 업데이트
                                                stats.inProgressReached++;
                                                if (!stats.timing.firstInProgress) {
                                                    stats.timing.firstInProgress = inProgressTime;
                                                }
                                                stats.timing.lastInProgress = inProgressTime;

                                                // 즉시 disconnect API 호출
                                                fetch(`${BASE_URL}/api/v1/broker/events/${eventId}/tickets/disconnect`, {
                                                    method: 'POST',
                                                    headers: {
                                                        'Authorization': accessToken,
                                                        'Cookie': `refreshToken=${refreshToken}`
                                                    }
                                                }).then(disconnectResponse => {
                                                    if (disconnectResponse.ok) {
                                                        console.log(`✅ [User ${userId}] disconnect API 호출 성공`);
                                                    } else {
                                                        console.log(`⚠️ [User ${userId}] disconnect API 호출 실패: ${disconnectResponse.status}`);
                                                    }
                                                }).catch(disconnectError => {
                                                    console.log(`❌ [User ${userId}] disconnect API 오류:`, disconnectError.message);
                                                });

                                                // 설정된 지연 시간 후 연결 종료
                                                setTimeout(() => {
                                                    clearTimeout(timeout);
                                                    reader.cancel();
                                                    stats.completedUsers++;

                                                    const totalTime = Date.now() - startTime;
                                                    console.log(`🏁 [User ${userId}] 연결 종료 (총 ${totalTime}ms)`);

                                                    resolve({
                                                        success: true,
                                                        connectionTime: connectionTime,
                                                        processingTime: processingTime
                                                    });
                                                }, IN_PROGRESS_DELAY);
                                                return;

                                            } else if (messageData.status === 'IN_ENTRY') {
                                                console.log(`⏳ [User ${userId}] 대기 중...`);
                                            }
                                        }
                                    } catch (parseError) {
                                        console.log(`❌ [User ${userId}] JSON 파싱 오류:`, data);
                                    }
                                }
                            }
                        }
                    }
                }).catch(error => {
                    const duration = Date.now() - startTime;
                    console.error(`❌ [User ${userId}] 스트림 읽기 오류 (${duration}ms):`, error.message);
                    clearTimeout(timeout);
                    stats.errors.connection++;
                    resolve({
                        success: false,
                        connectionTime: connectionTime || duration,
                        processingTime: 0
                    });
                });
            }

            readStream();

        }).catch(error => {
            const duration = Date.now() - startTime;
            console.error(`❌ [User ${userId}] fetch 오류 (${duration}ms):`, error.message);
            clearTimeout(timeout);
            stats.errors.connection++;
            resolve({
                success: false,
                connectionTime: duration,
                processingTime: 0
            });
        });
    });
}

/**
 * 처리량 계산 및 분석
 */
function analyzeThroughput() {
    const testDuration = (stats.timing.testEnd - stats.timing.testStart) / 1000; // 초
    const processingDuration = stats.timing.lastInProgress && stats.timing.firstInProgress
        ? (stats.timing.lastInProgress - stats.timing.firstInProgress) / 1000
        : 0;

    console.log('\n📊 처리량 분석');
    console.log('=' * 50);

    // 전체 테스트 기준 처리량
    const overallThroughput = stats.completedUsers / testDuration;
    console.log(`전체 테스트 시간: ${testDuration.toFixed(2)}초`);
    console.log(`전체 처리량: ${overallThroughput.toFixed(2)} users/sec`);

    // IN_PROGRESS 도달 기간 기준 처리량
    if (processingDuration > 0) {
        const processingThroughput = stats.inProgressReached / processingDuration;
        console.log(`IN_PROGRESS 처리 시간: ${processingDuration.toFixed(2)}초`);
        console.log(`IN_PROGRESS 처리량: ${processingThroughput.toFixed(2)} users/sec`);
    }

    // 평균 시간 분석
    if (stats.timing.connectionTimes.length > 0) {
        const avgConnectionTime = stats.timing.connectionTimes.reduce((a, b) => a + b, 0) / stats.timing.connectionTimes.length;
        console.log(`평균 연결 시간: ${avgConnectionTime.toFixed(2)}ms`);
    }

    if (stats.timing.processingTimes.length > 0) {
        const avgProcessingTime = stats.timing.processingTimes.reduce((a, b) => a + b, 0) / stats.timing.processingTimes.length;
        console.log(`평균 처리 시간: ${avgProcessingTime.toFixed(2)}ms`);
    }

    return {
        overallThroughput,
        processingThroughput: processingDuration > 0 ? stats.inProgressReached / processingDuration : 0,
        testDuration,
        processingDuration
    };
}

/**
 * 메인 테스트 실행
 */
async function runThroughputTest() {
    // 명령행 인자 파싱
    const args = process.argv.slice(2);
    const vuCount = parseInt(args[0]) || DEFAULT_VU_COUNT;
    const eventId = args[1] || DEFAULT_EVENT_ID;

    console.log('🚀 SSE 대기열 처리량 테스트 시작');
    console.log(`📊 설정: VU=${vuCount}, EventID=${eventId}`);
    console.log(`⚙️  IN_PROGRESS 후 ${IN_PROGRESS_DELAY}ms 대기 후 연결 종료`);
    console.log('=' * 60);

    stats.totalUsers = vuCount;
    stats.timing.testStart = Date.now();

    // 1단계: 모든 사용자 로그인
    console.log('\n📝 [1단계] 사용자 로그인...');
    const loginPromises = [];
    for (let i = 1; i <= vuCount; i++) {
        loginPromises.push(loginUser(i));
    }

    const loginResults = await Promise.all(loginPromises);
    const validUsers = [];

    for (let i = 0; i < loginResults.length; i++) {
        const result = loginResults[i];
        if (result) {
            validUsers.push({
                userId: i + 1,
                ...result
            });
        }
    }

    console.log(`📊 로그인 결과: ${validUsers.length}/${vuCount} 성공`);

    if (validUsers.length === 0) {
        console.error('❌ 로그인한 사용자가 없어 테스트를 종료합니다.');
        return;
    }

    // 2단계: 동시 SSE 연결 및 처리량 측정
    console.log('\n🎫 [2단계] 대기열 동시 진입 및 처리량 측정...');
    console.log(`⚡ ${validUsers.length}명의 사용자가 동시에 대기열에 진입합니다.`);

    const queuePromises = validUsers.map(user =>
        connectToQueue(user.userId, eventId, user.accessToken, user.refreshToken)
    );

    const queueResults = await Promise.all(queuePromises);

    stats.timing.testEnd = Date.now();

    // 결과 수집
    queueResults.forEach(result => {
        if (result.success) {
            stats.timing.connectionTimes.push(result.connectionTime);
            if (result.processingTime > 0) {
                stats.timing.processingTimes.push(result.processingTime);
            }
        }
    });

    // 3단계: 결과 분석
    console.log('\n📊 [3단계] 결과 분석');
    console.log('=' * 50);

    const totalDuration = (stats.timing.testEnd - stats.timing.testStart) / 1000;
    console.log(`총 테스트 시간: ${totalDuration.toFixed(2)}초`);
    console.log(`총 사용자 수: ${stats.totalUsers}`);
    console.log(`성공한 로그인: ${stats.successfulLogins}`);
    console.log(`성공한 SSE 연결: ${stats.successfulConnections}`);
    console.log(`IN_PROGRESS 도달: ${stats.inProgressReached}`);
    console.log(`완료된 사용자: ${stats.completedUsers}`);

    console.log('\n❌ 오류 통계:');
    console.log(`  로그인 오류: ${stats.errors.login}`);
    console.log(`  연결 오류: ${stats.errors.connection}`);
    console.log(`  타임아웃: ${stats.errors.timeout}`);
    console.log(`  기타 오류: ${stats.errors.other}`);

    console.log('\n📈 성공률:');
    console.log(`  로그인 성공률: ${(stats.successfulLogins / stats.totalUsers * 100).toFixed(1)}%`);
    console.log(`  연결 성공률: ${(stats.successfulConnections / stats.successfulLogins * 100).toFixed(1)}%`);
    console.log(`  IN_PROGRESS 도달률: ${(stats.inProgressReached / stats.successfulConnections * 100).toFixed(1)}%`);

    // 처리량 분석
    const throughputAnalysis = analyzeThroughput();

    // 최종 결론
    console.log('\n🎯 최종 결과:');
    console.log(`⚡ 시스템 처리량: ${throughputAnalysis.overallThroughput.toFixed(2)} users/sec`);

    if (throughputAnalysis.processingThroughput > 0) {
        console.log(`🎫 대기열 처리량: ${throughputAnalysis.processingThroughput.toFixed(2)} users/sec`);
    }

    if (stats.inProgressReached > 0) {
        console.log(`✅ ${stats.inProgressReached}명이 성공적으로 IN_PROGRESS 상태에 도달했습니다.`);
        console.log(`🔄 평균적으로 초당 ${throughputAnalysis.overallThroughput.toFixed(2)}명의 사용자를 처리할 수 있습니다.`);
    } else {
        console.log('⚠️  IN_PROGRESS에 도달한 사용자가 없습니다. 서비스 상태를 확인해주세요.');
    }

    console.log('\n🏁 처리량 테스트 완료');

    return {
        throughput: throughputAnalysis.overallThroughput,
        successfulUsers: stats.completedUsers,
        totalUsers: stats.totalUsers,
        testDuration: totalDuration
    };
}

// 프로그램 시작
if (require.main === module) {
    runThroughputTest().catch(error => {
        console.error('💥 테스트 실행 중 치명적 오류:', error);
        process.exit(1);
    });
}

module.exports = {
    loginUser,
    connectToQueue,
    runThroughputTest,
    analyzeThroughput
};