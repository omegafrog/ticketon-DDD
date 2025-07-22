# 티켓팅 사용자 여정: 대기열 진입부터 결제 완료까지

## 개요

본 문서는 Ticketon 시스템에서 사용자가 이벤트 티켓을 구매하는 전체 과정을 기술적 관점에서 상세히 분석합니다. 단순한 기능 구현을 넘어서, 대규모 트래픽 상황에서의 성능 최적화, 데이터 일관성 보장, 사용자 경험 개선을 위한 기술적 의사결정들을 중심으로 설명합니다.

---

## 1단계: 이벤트 접근 및 대기열 진입

### 사용자 시나리오
사용자가 인기 콘서트 티켓팅 페이지에 접근하면, 시스템은 현재 서버 부하와 좌석 가용성을 판단하여 대기열 시스템으로 자동 라우팅합니다.

### 기술적 고민: 대기열 진입 시점 결정

**고민점**: 언제 사용자를 대기열에 진입시킬 것인가?
- 이벤트 페이지 접근 시 즉시?
- 실제 구매 버튼 클릭 시?
- 서버 부하에 따라 동적으로?

**해결책**: 하이브리드 접근 방식
```java
@RestController
@RequestMapping("/api/v1/events")
public class EventController {
    
    private final EventService eventService;
    private final LoadBalancer loadBalancer;
    
    /**
     * 이벤트 상세 조회 시 대기열 필요성 판단
     * 기술적 고민: 실시간 부하 측정과 예측적 라우팅의 균형
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<?> getEventDetails(@PathVariable String eventId) {
        Event event = eventService.findById(eventId);
        
        // 1. 실시간 서버 부하 체크
        ServerLoad currentLoad = loadBalancer.getCurrentLoad();
        
        // 2. 이벤트별 동시 접속자 수 확인
        int currentConcurrentUsers = eventService.getCurrentConcurrentUsers(eventId);
        
        // 3. 좌석 잔여량 기반 예측
        int remainingSeats = event.getRemainingSeats();
        int expectedDemand = eventService.getExpectedDemand(eventId);
        
        /**
         * 대기열 진입 조건 (복합 판단)
         * - CPU 사용률 > 70% OR
         * - 동시 접속자 > 좌석 수 * 3 OR  
         * - 예상 수요 > 잔여 좌석 * 10
         */
        boolean shouldUseQueue = currentLoad.getCpuUsage() > 0.7 || 
                                currentConcurrentUsers > remainingSeats * 3 ||
                                expectedDemand > remainingSeats * 10;
        
        if (shouldUseQueue) {
            return ResponseEntity.ok(EventDetailsWithQueue.builder()
                .event(event)
                .queueRequired(true)
                .queueUrl("/api/v1/broker/events/" + eventId + "/tickets/waiting")
                .estimatedWaitTime(calculateEstimatedWaitTime(currentConcurrentUsers))
                .build());
        }
        
        return ResponseEntity.ok(EventDetailsResponse.from(event));
    }
    
    /**
     * 기술적 고민: 대기 시간 예측 알고리즘의 정확성
     * 단순 계산 vs 머신러닝 기반 예측
     */
    private Duration calculateEstimatedWaitTime(int queueLength) {
        // 과거 데이터 기반 처리율 계산
        double averageProcessingRate = metricsService.getAverageProcessingRate();
        
        // 현재 시간대별 가중치 적용
        double timeBasedMultiplier = getTimeBasedMultiplier();
        
        long estimatedSeconds = Math.round(queueLength / (averageProcessingRate * timeBasedMultiplier));
        return Duration.ofSeconds(estimatedSeconds);
    }
}
```

### 대기열 진입 처리

**기술적 고민**: 대기열 진입 시 발생할 수 있는 동시성 문제들
- 같은 사용자의 중복 진입
- 대기 순번 할당의 원자성
- 대기열 상태 정보의 일관성

```java
@Service
@RequiredArgsConstructor
public class WaitingQueueEntryService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final EventService eventService;
    
    /**
     * 대기열 진입 - 동시성과 정확성을 모두 보장
     * 기술적 고민: Redis 연산의 원자성 vs 성능 트레이드오프
     */
    @Transactional
    public QueueEntryResult enterWaitingQueue(String eventId, String userId, String instanceId) {
        
        // 1. 중복 진입 방지 - 원자적 연산 사용
        String duplicateCheckKey = "WAITING_USER_ID:" + eventId;
        Boolean isFirstEntry = redisTemplate.opsForHash().putIfAbsent(duplicateCheckKey, userId, "true");
        
        if (!isFirstEntry) {
            // 기존 진입 정보 반환 (사용자에게 현재 상태 제공)
            return getExistingQueueStatus(eventId, userId);
        }
        
        try {
            // 2. 이벤트 유효성 검증 (캐시 우선 조회)
            EventInfo eventInfo = getEventInfoWithCache(eventId);
            validateEventForQueue(eventInfo);
            
            // 3. 대기열 진입 시간 기록 (마이크로초 정밀도)
            long preciseTimestamp = System.currentTimeMillis() * 1000 + System.nanoTime() % 1000000;
            
            // 4. 대기 큐에 추가 (ZSet의 score로 정확한 순서 보장)
            String waitingKey = "WAITING:" + eventId;
            redisTemplate.opsForZSet().add(waitingKey, userId, preciseTimestamp);
            
            // 5. 사용자 메타데이터 저장 (빠른 조회를 위한 해시 구조)
            String recordKey = "WAITING_QUEUE_RECORD:" + eventId;
            QueueUserRecord userRecord = QueueUserRecord.builder()
                .userId(userId)
                .eventId(eventId)
                .joinedAt(preciseTimestamp)
                .instanceId(instanceId)
                .seatCount(eventInfo.getTotalSeats())
                .userAgent(getCurrentUserAgent())
                .build();
                
            redisTemplate.opsForHash().put(recordKey, userId, 
                objectMapper.writeValueAsString(userRecord));
            
            // 6. 현재 대기 순번 계산
            Long currentPosition = redisTemplate.opsForZSet().rank(waitingKey, userId);
            Long totalWaiting = redisTemplate.opsForZSet().count(waitingKey, 0, System.currentTimeMillis() * 1000);
            
            // 7. 대기열 진입 이벤트 발행 (모니터링 목적)
            applicationEventPublisher.publishEvent(
                QueueEntryEvent.builder()
                    .eventId(eventId)
                    .userId(userId)
                    .position(currentPosition + 1)
                    .totalWaiting(totalWaiting)
                    .timestamp(Instant.now())
                    .build()
            );
            
            return QueueEntryResult.builder()
                .success(true)
                .position(currentPosition + 1)
                .totalWaiting(totalWaiting)
                .estimatedWaitTime(calculateWaitTime(currentPosition, eventId))
                .build();
                
        } catch (Exception e) {
            // 실패 시 중복 방지 키 정리 (보상 트랜잭션)
            redisTemplate.opsForHash().delete(duplicateCheckKey, userId);
            log.error("대기열 진입 실패 - userId: {}, eventId: {}", userId, eventId, e);
            throw new QueueEntryException("대기열 진입 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 기술적 고민: 캐시 전략 - 이벤트 정보의 일관성 vs 성능
     * 해결: 캐시 aside 패턴 + TTL 기반 무효화
     */
    @Cacheable(value = "eventInfo", key = "#eventId", unless = "#result == null")
    private EventInfo getEventInfoWithCache(String eventId) {
        EventInfo eventInfo = eventService.getEventInfo(eventId);
        if (eventInfo == null) {
            throw new EventNotFoundException("이벤트를 찾을 수 없습니다: " + eventId);
        }
        return eventInfo;
    }
}
```

---

## 2단계: SSE 연결 및 실시간 상태 업데이트

### 사용자 시나리오
대기열 진입 후 사용자는 실시간으로 자신의 대기 순번과 예상 대기 시간을 확인할 수 있습니다.

### 기술적 고민: SSE vs WebSocket vs 폴링

**고민점**: 실시간 통신 방식 선택
- **SSE**: 단방향 통신, HTTP/2 호환, 자동 재연결
- **WebSocket**: 양방향 통신, 오버헤드 적음, 복잡한 상태 관리
- **폴링**: 구현 간단, 높은 지연시간, 서버 부하

**선택**: SSE + 하이브리드 접근
- 대기열은 서버→클라이언트 단방향 정보 전달이 주목적
- HTTP/2의 멀티플렉싱 활용
- 브라우저 호환성 우수

```java
@Service
@RequiredArgsConstructor
public class SseEmitterService {
    
    private final ConcurrentHashMap<String, SseConnection> connections = new ConcurrentHashMap<>();
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * SSE 연결 생성 및 최적화
     * 기술적 고민: 메모리 효율성 vs 응답성 트레이드오프
     */
    public SseEmitter createSseConnection(String userId, String eventId) {
        
        // 1. 기존 연결 정리 (메모리 누수 방지)
        cleanupExistingConnection(userId);
        
        // 2. SSE Emitter 설정
        // 무한 타임아웃 설정: 클라이언트가 연결을 유지하는 동안 계속 유지
        SseEmitter emitter = new SseEmitter(0L);
        
        // 3. 연결 메타데이터 생성
        SseConnection connection = SseConnection.builder()
            .emitter(emitter)
            .status(QueueStatus.IN_ENTRY)
            .eventId(eventId)
            .connectedAt(System.currentTimeMillis())
            .lastHeartbeat(System.currentTimeMillis())
            .build();
            
        connections.put(userId, connection);
        
        // 4. 이벤트 핸들러 설정 - 리소스 정리 자동화
        emitter.onCompletion(() -> handleConnectionClose(userId, "completed"));
        emitter.onTimeout(() -> handleConnectionClose(userId, "timeout"));
        emitter.onError(throwable -> {
            log.warn("SSE 연결 오류 - userId: {}, error: {}", userId, throwable.getMessage());
            handleConnectionClose(userId, "error");
        });
        
        // 5. 초기 상태 전송 (즉시 피드백)
        sendInitialStatus(userId, eventId);
        
        log.info("SSE 연결 생성 - userId: {}, eventId: {}, 총 연결: {}", 
            userId, eventId, connections.size());
            
        return emitter;
    }
    
    /**
     * 대기 순번 정보 전송 - 배치 처리 최적화
     * 기술적 고민: 개별 전송 vs 배치 전송의 효율성
     */
    public void broadcastQueuePositions(String eventId) {
        
        // 1. 해당 이벤트의 모든 대기자 조회 (한 번의 Redis 호출)
        String waitingKey = "WAITING:" + eventId;
        Set<ZSetOperations.TypedTuple<Object>> waitingUsers = 
            redisTemplate.opsForZSet().rangeWithScores(waitingKey, 0, -1);
            
        if (waitingUsers == null || waitingUsers.isEmpty()) {
            return;
        }
        
        // 2. 배치 처리를 위한 데이터 준비
        List<QueuePositionUpdate> updates = new ArrayList<>();
        int totalWaiting = waitingUsers.size();
        int position = 1;
        
        for (ZSetOperations.TypedTuple<Object> user : waitingUsers) {
            String userId = (String) user.getValue();
            SseConnection connection = connections.get(userId);
            
            if (connection != null && eventId.equals(connection.getEventId())) {
                updates.add(QueuePositionUpdate.builder()
                    .userId(userId)
                    .position(position)
                    .totalWaiting(totalWaiting)
                    .estimatedWaitTime(calculateWaitTime(position - 1, totalWaiting))
                    .build());
            }
            position++;
        }
        
        // 3. 병렬 처리로 모든 사용자에게 동시 전송
        // 기술적 고민: 순차 처리 vs 병렬 처리
        // 해결: CompletableFuture를 통한 비블로킹 병렬 처리
        updates.parallelStream().forEach(this::sendQueuePositionUpdate);
    }
    
    /**
     * 개별 위치 업데이트 전송
     */
    private void sendQueuePositionUpdate(QueuePositionUpdate update) {
        SseConnection connection = connections.get(update.getUserId());
        if (connection == null) return;
        
        try {
            Map<String, Object> data = Map.of(
                "type", "QUEUE_POSITION",
                "position", update.getPosition(),
                "totalWaiting", update.getTotalWaiting(),
                "estimatedWaitTime", update.getEstimatedWaitTime().toMinutes(),
                "timestamp", System.currentTimeMillis()
            );
            
            connection.getEmitter().send(SseEmitter.event()
                .name("queueUpdate")
                .id(String.valueOf(System.currentTimeMillis()))
                .data(objectMapper.writeValueAsString(data)));
                
            // 마지막 하트비트 시간 업데이트
            connection.setLastHeartbeat(System.currentTimeMillis());
            
        } catch (IOException e) {
            log.error("순번 정보 전송 실패 - userId: {}", update.getUserId(), e);
            handleConnectionClose(update.getUserId(), "send_failed");
        }
    }
    
    /**
     * 하트비트 및 연결 상태 관리
     * 기술적 고민: 하트비트 주기 최적화
     * - 너무 자주: 불필요한 네트워크 트래픽
     * - 너무 늦게: 연결 끊김 감지 지연
     */
    @Scheduled(fixedRate = 5000) // 5초마다
    public void sendHeartbeatAndCleanup() {
        long currentTime = System.currentTimeMillis();
        List<String> disconnectedUsers = new ArrayList<>();
        
        connections.forEach((userId, connection) -> {
            try {
                // 1. 하트비트 전송
                connection.getEmitter().send(SseEmitter.event()
                    .name("heartbeat")
                    .data("{\"type\":\"heartbeat\",\"timestamp\":" + currentTime + "}"));
                    
                connection.setLastHeartbeat(currentTime);
                
                // 2. 오래된 연결 감지 (30초 이상 무응답)
                if (currentTime - connection.getLastHeartbeat() > 30000) {
                    disconnectedUsers.add(userId);
                }
                
            } catch (IOException e) {
                disconnectedUsers.add(userId);
            }
        });
        
        // 3. 끊어진 연결 정리
        disconnectedUsers.forEach(userId -> handleConnectionClose(userId, "heartbeat_failed"));
        
        if (!disconnectedUsers.isEmpty()) {
            log.info("하트비트 실패로 정리된 연결: {}개", disconnectedUsers.size());
        }
    }
}
```

---

## 3단계: 대기열 승격 및 구매 권한 획득

### 사용자 시나리오
Dispatcher가 매초마다 대기열을 스캔하고, 진입 가능한 사용자들을 승격시킵니다. 사용자는 승격 알림을 받고 제한된 시간 내에 좌석 선택 페이지로 이동합니다.

### 기술적 고민: 승격 알고리즘의 공정성과 효율성

**고민점**: 
- 선착순 vs 가중치 기반 선발
- 일괄 승격 vs 점진적 승격
- 승격 인원 결정 알고리즘

**해결책**: 동적 용량 관리 + 예측적 승격

```java
@Component
@Slf4j
public class EntryPromoteThread {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<List> promotionScript;
    private final ThreadPoolExecutor promotionExecutor;
    
    /**
     * 승격 처리 스케줄러 - 매초 실행
     * 기술적 고민: 승격 주기 최적화 (1초 vs 더 자주 vs 덜 자주)
     * 결론: 1초 - 사용자 체감 대기시간과 시스템 부하의 균형점
     */
    @Scheduled(cron = "* * * * * *")
    @Async("singleThreadExecutor")
    public void promoteWaitingUsers() {
        
        try {
            // 1. 활성 대기열 탐지
            Set<String> waitingQueueKeys = redisTemplate.keys("WAITING:*");
            if (waitingQueueKeys == null || waitingQueueKeys.isEmpty()) {
                return;
            }
            
            // 2. 이벤트별 처리 우선순위 결정
            List<EventPromotionTask> prioritizedTasks = waitingQueueKeys.stream()
                .map(this::createPromotionTask)
                .sorted(Comparator.comparingInt(EventPromotionTask::getPriority).reversed())
                .collect(Collectors.toList());
            
            // 3. 작업 분산을 위한 임시 큐 생성
            String taskQueueKey = "TEMP_PROMOTION_QUEUE:" + UUID.randomUUID();
            String[] eventIds = prioritizedTasks.stream()
                .map(EventPromotionTask::getEventId)
                .toArray(String[]::new);
                
            redisTemplate.opsForList().leftPushAll(taskQueueKey, eventIds);
            redisTemplate.expire(taskQueueKey, Duration.ofMinutes(5));
            
            // 4. 멀티스레드 병렬 처리
            int workerCount = Math.min(10, prioritizedTasks.size());
            for (int i = 0; i < workerCount; i++) {
                promotionExecutor.submit(new PromotionWorker(taskQueueKey));
            }
            
        } catch (Exception e) {
            log.error("승격 스케줄러 실행 중 오류", e);
        }
    }
    
    /**
     * 이벤트별 승격 우선순위 계산
     * 기술적 고민: 우선순위 결정 기준
     * - 대기자 수 vs 긴급도 vs 수익성
     */
    private EventPromotionTask createPromotionTask(String waitingKey) {
        String eventId = waitingKey.substring("WAITING:".length());
        
        // 대기자 수 조회
        Long waitingCount = redisTemplate.opsForZSet().count(waitingKey, 0, System.currentTimeMillis() * 1000);
        
        // 현재 진입 큐 여유 공간 확인
        String entryCountKey = "ENTRY_QUEUE_COUNT:" + eventId;
        Long currentEntryCount = Optional.ofNullable(
            (Long) redisTemplate.opsForValue().get(entryCountKey)
        ).orElse(0L);
        
        int availableCapacity = Math.max(0, ENTRY_QUEUE_CAPACITY - currentEntryCount.intValue());
        
        // 우선순위 계산 (대기자 많고, 진입 여유 공간 있는 순)
        int priority = waitingCount.intValue() * availableCapacity;
        
        return EventPromotionTask.builder()
            .eventId(eventId)
            .waitingCount(waitingCount.intValue())
            .availableCapacity(availableCapacity)
            .priority(priority)
            .build();
    }
    
    /**
     * 개별 워커 스레드 - 원자적 승격 처리
     */
    private class PromotionWorker implements Runnable {
        private final String taskQueueKey;
        
        @Override
        public void run() {
            while (true) {
                String eventId = (String) redisTemplate.opsForList().rightPop(taskQueueKey);
                if (eventId == null) break;
                
                processEventPromotion(eventId);
            }
        }
    }
    
    /**
     * Lua 스크립트 기반 원자적 승격
     * 기술적 고민: 정확성 vs 성능
     * 해결책: 모든 연산을 단일 Lua 스크립트로 원자화
     */
    private void processEventPromotion(String eventId) {
        
        // 현재 진입 가능 용량 동적 계산
        int dynamicCapacity = calculateDynamicCapacity(eventId);
        
        List<String> keys = Arrays.asList(
            "WAITING:" + eventId,                  // 대기 큐
            "ENTRY_QUEUE_COUNT:" + eventId,        // 진입 카운터
            "WAITING_QUEUE_RECORD:" + eventId,     // 메타데이터
            "WAITING_USER_ID:" + eventId,          // 중복 방지
            "ENTRY"                               // 진입 스트림
        );
        
        List<String> args = Arrays.asList(eventId, String.valueOf(dynamicCapacity));
        
        try {
            @SuppressWarnings("unchecked")
            List<Object> result = redisTemplate.execute(promotionScript, keys, args.toArray());
            
            if (result != null && result.size() >= 2) {
                Long promotedCount = (Long) result.get(0);
                @SuppressWarnings("unchecked")
                List<String> promotedUsers = (List<String>) result.get(1);
                
                if (promotedCount > 0) {
                    promotionCounter.addAndGet(promotedCount);
                    
                    // 승격 이벤트 발행 (모니터링 및 알림)
                    applicationEventPublisher.publishEvent(
                        UsersPromotedEvent.builder()
                            .eventId(eventId)
                            .promotedUsers(promotedUsers)
                            .promotedCount(promotedCount.intValue())
                            .timestamp(Instant.now())
                            .build()
                    );
                    
                    log.info("이벤트 {} 승격 완료: {}명", eventId, promotedCount);
                }
            }
            
        } catch (Exception e) {
            log.error("이벤트 {} 승격 처리 실패", eventId, e);
            
            // 실패 시 재시도 로직 (최대 3회)
            scheduleRetryPromotion(eventId);
        }
    }
    
    /**
     * 동적 용량 계산 - 실시간 상황에 맞춰 조정
     * 기술적 고민: 고정 용량 vs 동적 용량
     */
    private int calculateDynamicCapacity(String eventId) {
        
        // 1. 기본 용량
        int baseCapacity = ENTRY_QUEUE_CAPACITY; // 1000
        
        // 2. 현재 시간대 고려 (피크 시간 vs 오프피크)
        double timeMultiplier = getTimeBasedMultiplier();
        
        // 3. 서버 부하 고려
        double loadMultiplier = getLoadBasedMultiplier();
        
        // 4. 이벤트별 과거 전환율 고려
        double conversionRate = getEventConversionRate(eventId);
        
        int dynamicCapacity = (int) Math.round(
            baseCapacity * timeMultiplier * loadMultiplier * conversionRate
        );
        
        // 최소/최대 제한
        return Math.max(100, Math.min(2000, dynamicCapacity));
    }
}
```

### Lua 스크립트 - 원자적 승격 처리

```lua
-- promote_all_waiting_for_event.lua
-- 기술적 고민: 복잡한 비즈니스 로직을 Lua로 구현할 때의 가독성 vs 성능
-- 해결: 주석과 모듈화로 가독성 향상, Redis 원자성으로 정확성 보장

local eventId = ARGV[1]
local capacity = tonumber(ARGV[2])

local waitingKey = KEYS[1]
local countKey = KEYS[2]
local recordKey = KEYS[3]
local userIdKey = KEYS[4]
local streamKey = KEYS[5]

-- 1. 현재 진입 큐 상태 확인
local currentCount = tonumber(redis.call('GET', countKey) or 0)
local availableCapacity = capacity - currentCount

if availableCapacity <= 0 then
    return {0, {}, "용량 부족"}
end

-- 2. 승격 대상자 선정 (선착순, 시간 기준 정렬)
local candidateUsers = redis.call('ZRANGE', waitingKey, 0, availableCapacity - 1, 'WITHSCORES')

if #candidateUsers == 0 then
    return {0, {}, "대기자 없음"}
end

-- 3. 사용자별 승격 처리
local cjson = require "cjson"
local promotedCount = 0
local promotedUserList = {}
local failedUsers = {}

for i = 1, #candidateUsers, 2 do
    local userId = candidateUsers[i]
    local timestamp = candidateUsers[i + 1]
    
    -- 사용자 메타데이터 조회
    local userDataJson = redis.call('HGET', recordKey, userId)
    if userDataJson then
        local userData = cjson.decode(userDataJson)
        
        -- 대기 구조에서 원자적 제거
        redis.call('ZREM', waitingKey, userId)
        redis.call('HDEL', recordKey, userId)
        redis.call('HDEL', userIdKey, userId)
        
        -- 진입 스트림에 추가
        local streamData = {
            'userId', userId,
            'eventId', eventId,
            'promotedAt', timestamp,
            'instanceId', userData.instanceId or 'unknown',
            'priority', userData.priority or 'normal',
            'waitedTime', redis.call('TIME')[1] * 1000 - tonumber(timestamp)
        }
        
        local streamId = redis.call('XADD', streamKey, '*', unpack(streamData))
        
        if streamId then
            promotedCount = promotedCount + 1
            table.insert(promotedUserList, userId)
        else
            -- 스트림 추가 실패 시 롤백 (데이터 일관성 보장)
            redis.call('ZADD', waitingKey, timestamp, userId)
            redis.call('HSET', recordKey, userId, userDataJson)
            redis.call('HSET', userIdKey, userId, 'true')
            table.insert(failedUsers, userId)
        end
    end
end

-- 4. 진입 카운터 업데이트
if promotedCount > 0 then
    redis.call('INCRBY', countKey, promotedCount)
    redis.call('EXPIRE', countKey, 3600) -- 1시간 TTL
end

-- 5. 결과 반환 (성공/실패 정보 포함)
return {
    promotedCount, 
    promotedUserList, 
    #failedUsers > 0 and "일부 실패: " .. table.concat(failedUsers, ",") or "성공"
}
```

---

## 4단계: 좌석 선택 및 임시 예약

### 사용자 시나리오
승격된 사용자는 제한된 시간(보통 10분) 내에 좌석 선택 페이지에서 원하는 좌석을 선택합니다.

### 기술적 고민: 좌석 선택의 동시성 제어

**문제점**: 여러 사용자가 동시에 같은 좌석을 선택하는 경우
**해결책**: 분산 락 + 낙관적 동시성 제어

```java
@Service
@RequiredArgsConstructor
public class SeatReservationService {
    
    private final RedisLockService redisLockService;
    private final SeatRepository seatRepository;
    
    /**
     * 좌석 선택 처리 - 분산 환경에서의 동시성 보장
     * 기술적 고민: 비관적 락 vs 낙관적 락 vs 분산 락
     * 선택: 분산 락 (Redis) + 재시도 메커니즘
     */
    @Transactional
    @Retryable(value = {SeatLockException.class}, maxAttempts = 3, 
               backoff = @Backoff(delay = 100, multiplier = 2))
    public SeatReservationResult reserveSeats(String userId, String eventId, 
                                           List<String> seatIds, String entryToken) {
        
        // 1. 진입 토큰 검증
        validateEntryToken(userId, eventId, entryToken);
        
        // 2. 좌석 선택 제한 시간 확인
        validateReservationTimeWindow(userId, eventId);
        
        List<String> successfullyLocked = new ArrayList<>();
        List<String> failedToLock = new ArrayList<>();
        
        try {
            // 3. 좌석별 분산 락 획득 시도
            for (String seatId : seatIds) {
                String lockKey = generateSeatLockKey(eventId, seatId);
                Duration lockTimeout = Duration.ofMinutes(10); // 결제 완료까지의 시간
                
                boolean lockAcquired = redisLockService.tryLock(
                    lockKey, userId, lockTimeout
                );
                
                if (lockAcquired) {
                    successfullyLocked.add(seatId);
                } else {
                    failedToLock.add(seatId);
                    
                    // 현재 락 소유자 확인 (디버깅 목적)
                    String currentOwner = redisLockService.getLockOwner(lockKey);
                    log.warn("좌석 락 획득 실패 - seatId: {}, currentOwner: {}, requestUser: {}", 
                        seatId, currentOwner, userId);
                }
            }
            
            // 4. 부분 실패 시 정책 결정
            if (!failedToLock.isEmpty()) {
                if (failedToLock.size() == seatIds.size()) {
                    // 모든 좌석 실패 - 대안 추천
                    return SeatReservationResult.allFailed(
                        failedToLock, 
                        recommendAlternativeSeats(eventId, seatIds.size())
                    );
                } else {
                    // 부분 실패 - 사용자에게 선택권 제공
                    return SeatReservationResult.partialSuccess(
                        successfullyLocked, 
                        failedToLock
                    );
                }
            }
            
            // 5. 임시 예약 정보 생성 및 저장
            SeatReservation reservation = SeatReservation.builder()
                .userId(userId)
                .eventId(eventId)
                .seatIds(successfullyLocked)
                .reservedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .status(ReservationStatus.TEMPORARY)
                .build();
                
            saveTempReservation(reservation);
            
            // 6. 가격 계산 및 결제 정보 준비
            PriceCalculationResult priceInfo = calculateTotalPrice(eventId, successfullyLocked);
            
            // 7. 예약 만료 스케줄링
            scheduleReservationExpiry(userId, eventId, reservation.getExpiresAt());
            
            return SeatReservationResult.success(
                successfullyLocked,
                priceInfo.getTotalPrice(),
                priceInfo.getItemizedPrices(),
                reservation.getExpiresAt()
            );
            
        } catch (Exception e) {
            // 실패 시 획득한 모든 락 해제
            releaseAllLocks(userId, successfullyLocked);
            throw new SeatReservationException("좌석 예약 중 오류 발생", e);
        }
    }
    
    /**
     * 좌석 락 해제 - 보상 트랜잭션
     */
    private void releaseAllLocks(String userId, List<String> seatIds) {
        seatIds.forEach(seatId -> {
            try {
                String lockKey = generateSeatLockKey(eventId, seatId);
                redisLockService.releaseLock(lockKey, userId);
            } catch (Exception e) {
                log.error("좌석 락 해제 실패 - seatId: {}, userId: {}", seatId, userId, e);
            }
        });
    }
    
    /**
     * 대안 좌석 추천 알고리즘
     * 기술적 고민: 단순 인접 좌석 vs ML 기반 추천
     */
    private List<AlternativeSeat> recommendAlternativeSeats(String eventId, int requestedCount) {
        
        // 1. 현재 가용 좌석 조회
        List<Seat> availableSeats = seatRepository.findAvailableSeatsByEvent(eventId);
        
        // 2. 추천 점수 계산 (위치, 가격, 시야 등)
        return availableSeats.stream()
            .map(seat -> {
                double score = calculateSeatScore(seat);
                return new AlternativeSeat(seat, score);
            })
            .sorted(Comparator.comparingDouble(AlternativeSeat::getScore).reversed())
            .limit(requestedCount * 3) // 3배수 추천
            .collect(Collectors.toList());
    }
}
```

---

## 5단계: 결제 처리 및 완료

### 사용자 시나리오
선택한 좌석에 대해 결제를 진행하고, 성공 시 정식 티켓이 발급됩니다.

### 기술적 고민: 결제 처리의 원자성과 복구 가능성

```java
@Service
@RequiredArgsConstructor
public class PurchaseService {
    
    private final PurchaseDomainService purchaseDomainService;
    private final PaymentValidationService paymentValidationService;
    private final TossPaymentPgApiService pgApiService;
    
    /**
     * 결제 확인 및 티켓 발급
     * 기술적 고민: 분산 트랜잭션 vs Saga 패턴 vs 보상 트랜잭션
     * 선택: 보상 트랜잭션 + 이벤트 소싱
     */
    @Transactional
    public ConfirmPaymentResponse confirmPayment(ConfirmPaymentRequest request, String userId) {
        
        // 1. 구매 정보 조회 및 검증
        Purchase purchase = purchaseRepository.findById(new PurchaseId(request.getPurchaseId()))
            .orElseThrow(() -> new PaymentNotFoundException("구매 정보를 찾을 수 없습니다."));
            
        purchase.validate(request.getOrderId(), request.getAmount(), userId);
        
        // 2. 좌석 락 상태 재확인 (결제 도중 만료 가능성)
        List<String> lockedSeats = redisLockService.getLockedSeatIdsByUserId(userId);
        if (lockedSeats.isEmpty()) {
            throw new SeatLockExpiredException("좌석 예약이 만료되었습니다.");
        }
        
        // 3. 외부 결제 게이트웨이 호출
        ConfirmedPaymentInfo paymentInfo;
        try {
            paymentInfo = pgApiService.confirmPayment(
                request.getPaymentKey(), 
                request.getOrderId(), 
                request.getAmount()
            );
        } catch (PaymentGatewayException e) {
            // 결제 게이트웨이 오류 시 좌석 락 연장 (사용자 재시도 기회 제공)
            extendSeatLockTimeout(userId, Duration.ofMinutes(5));
            throw new PaymentProcessingException("결제 처리 중 오류가 발생했습니다. 다시 시도해 주세요.", e);
        }
        
        try {
            // 4. 도메인 서비스를 통한 구매 확정 처리
            PurchaseDomainService.PurchaseConfirmationResult result = 
                purchaseDomainService.confirmPurchase(purchase, paymentInfo, userId);
            
            // 5. 구매 상태 업데이트
            purchase.markAsCompleted();
            
            // 6. 티켓 및 구매 정보 영속화
            List<Ticket> tickets = result.getTickets();
            ticketRepository.saveAll(tickets);
            purchaseRepository.save(purchase);
            
            // 7. 좌석 상태 업데이트 이벤트 발행
            publisher.publishSeatPurchasedEvent(
                purchase.getEventId(),
                result.getSeatLayout().getLayoutId(),
                result.getSeatIds(),
                userId
            );
            
            // 8. 대기열 관련 리소스 정리
            cleanupQueueResources(userId, purchase.getEventId());
            
            // 9. 성공 응답 생성
            return createSuccessResponse(paymentInfo, tickets);
            
        } catch (Exception e) {
            // 비즈니스 로직 실패 시 결제 취소 (보상 트랜잭션)
            log.error("구매 처리 실패, 결제 취소 시도 - userId: {}, paymentKey: {}", 
                userId, request.getPaymentKey(), e);
                
            compensateFailedPurchase(request.getPaymentKey(), userId, lockedSeats);
            throw new PurchaseProcessingException("구매 처리 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 실패한 구매에 대한 보상 처리
     * 기술적 고민: 자동 보상 vs 수동 처리 vs 하이브리드
     */
    @Async
    private void compensateFailedPurchase(String paymentKey, String userId, List<String> seatIds) {
        
        List<CompensationAction> actions = new ArrayList<>();
        
        try {
            // 1. 결제 취소
            CanceledPaymentInfo cancelInfo = pgApiService.cancelPayment(
                paymentKey, "시스템 오류로 인한 자동 취소"
            );
            actions.add(CompensationAction.paymentCanceled(paymentKey, cancelInfo));
            
        } catch (Exception e) {
            log.error("결제 취소 실패 - paymentKey: {}, 수동 처리 필요", paymentKey, e);
            actions.add(CompensationAction.paymentCancelFailed(paymentKey, e.getMessage()));
        }
        
        try {
            // 2. 좌석 락 해제
            seatIds.forEach(seatId -> {
                redisLockService.releaseLock(generateSeatLockKey(eventId, seatId), userId);
            });
            actions.add(CompensationAction.seatsReleased(seatIds));
            
        } catch (Exception e) {
            log.error("좌석 락 해제 실패 - userId: {}, seats: {}", userId, seatIds, e);
            actions.add(CompensationAction.seatReleaseFailed(seatIds, e.getMessage()));
        }
        
        // 3. 보상 결과 기록 (후속 처리 및 모니터링용)
        compensationEventPublisher.publishCompensationResult(
            CompensationResult.builder()
                .userId(userId)
                .paymentKey(paymentKey)
                .actions(actions)
                .timestamp(Instant.now())
                .build()
        );
    }
    
    /**
     * 대기열 리소스 정리
     */
    private void cleanupQueueResources(String userId, String eventId) {
        try {
            // 진입 큐 카운터 감소
            String entryCountKey = "ENTRY_QUEUE_COUNT:" + eventId;
            redisTemplate.opsForValue().decrement(entryCountKey);
            
            // 사용자별 락 정보 정리
            redisLockService.releaseAllLocks(userId);
            redisLockService.releaseAllEntryQueueLocks(userId);
            
        } catch (Exception e) {
            log.warn("대기열 리소스 정리 실패 - userId: {}, eventId: {}", userId, eventId, e);
        }
    }
}
```

## 결론

이 시스템은 대규모 동시 접속 상황에서도 안정적이고 공정한 티켓팅 서비스를 제공하기 위해 다음과 같은 기술적 의사결정을 내렸습니다:

**1. 성능 최적화**
- Redis 기반 분산 상태 관리로 수평 확장 가능
- Lua 스크립트를 통한 원자적 연산으로 정확성 보장
- 멀티스레딩과 비동기 처리로 처리량 극대화

**2. 사용자 경험**
- SSE를 통한 실시간 상태 업데이트
- 예측적 대기시간 제공
- 부분 실패 시에도 대안 제시

**3. 시스템 안정성**
- 보상 트랜잭션을 통한 장애 복구
- 분산 락을 통한 동시성 제어
- 포괄적인 모니터링과 알림

이러한 설계를 통해 초당 수천 명의 사용자가 동시에 접속하는 상황에서도 안정적인 서비스를 제공할 수 있습니다.