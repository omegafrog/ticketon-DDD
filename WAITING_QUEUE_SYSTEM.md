# Ticketon 대기열 시스템 상세 분석

## 개요

Ticketon의 대기열 시스템은 고트래픽 이벤트 티켓팅 상황에서 안정적이고 공정한 사용자 처리를 위해 설계된 분산 시스템입니다. Redis를 기반으로 한 실시간 대기열 관리와 멀티스레드 승격 엔진을 통해 초당 수천 명의 사용자를 효율적으로 처리할 수 있습니다.

## 시스템 아키텍처

### 핵심 컴포넌트

```
[사용자] → [Gateway] → [Broker] ← Redis → [Dispatcher] → [Purchase]
```

1. **Broker Service (Port: 9000)**: SSE 기반 실시간 대기열 상태 관리
2. **Dispatcher Service (Port: 9002)**: 멀티스레드 승격 처리 엔진
3. **Redis**: 중앙화된 대기열 상태 저장소
4. **Gateway**: 통합 진입점 및 인증 처리

### 데이터 구조

Redis에서 사용되는 핵심 데이터 구조:

```redis
# 대기 큐 (시간순 정렬)
WAITING:{eventId} (ZSet) - score: timestamp, member: userId

# 사용자 메타데이터
WAITING_QUEUE_RECORD:{eventId} (Hash) - userId: {position, timestamp, instanceId}

# 중복 진입 방지
WAITING_USER_ID:{eventId} (Hash) - userId: "true"

# 진입 큐 카운터
ENTRY_QUEUE_COUNT:{eventId} (String) - 현재 진입 가능 인원수

# 스트림 메시지
ENTRY (Stream) - 승격된 사용자 정보
DISPATCH (Stream) - 브로커 알림 정보
```

## 대기열 진입 로직 (Broker Service)

### WaitingQueueEntryService.java

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class WaitingQueueEntryService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final EventQueryService eventQueryService;
    
    /**
     * 대기열 진입 처리
     * - 중복 진입 검증
     * - 이벤트 유효성 검증  
     * - 대기 순번 원자적 할당
     */
    public void enterWaitingQueue(String eventId, String userId, String instanceId) {
        // 1. 중복 진입 검증
        String duplicateKey = RedisConfig.WAITING_USER_ID_KEY_NAME + ":" + eventId;
        Boolean isDuplicate = redisTemplate.opsForHash().putIfAbsent(duplicateKey, userId, "true");
        
        if (!isDuplicate) {
            throw new IllegalStateException("이미 대기열에 등록된 사용자입니다.");
        }

        try {
            // 2. 이벤트 좌석 수 조회
            EventInfoResponse eventInfo = eventQueryService.getEventInfo(eventId);
            if (eventInfo == null) {
                throw new IllegalArgumentException("존재하지 않는 이벤트입니다.");
            }

            // 3. 현재 시간으로 대기열 등록 (ZSet 사용으로 시간순 정렬)
            long timestamp = System.currentTimeMillis();
            String waitingKey = RedisConfig.WAITING_QUEUE_KEY_NAME + ":" + eventId;
            redisTemplate.opsForZSet().add(waitingKey, userId, timestamp);

            // 4. 사용자 메타데이터 저장
            String recordKey = RedisConfig.WAITING_QUEUE_RECORD_KEY_NAME + ":" + eventId;
            Map<String, Object> userRecord = Map.of(
                "userId", userId,
                "eventId", eventId,
                "joinedAt", timestamp,
                "instanceId", instanceId,
                "seatCount", eventInfo.getSeatCount()
            );
            
            redisTemplate.opsForHash().put(recordKey, userId, 
                new ObjectMapper().writeValueAsString(userRecord));

            log.info("사용자 {}가 이벤트 {} 대기열에 진입했습니다. 타임스탬프: {}", 
                userId, eventId, timestamp);

        } catch (Exception e) {
            // 실패 시 중복 방지 키 롤백
            redisTemplate.opsForHash().delete(duplicateKey, userId);
            throw new RuntimeException("대기열 진입 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 대기 순번 조회
     */
    public Long getWaitingPosition(String eventId, String userId) {
        String waitingKey = RedisConfig.WAITING_QUEUE_KEY_NAME + ":" + eventId;
        return redisTemplate.opsForZSet().rank(waitingKey, userId);
    }

    /**
     * 전체 대기 인원 조회
     */
    public Long getTotalWaitingCount(String eventId) {
        String waitingKey = RedisConfig.WAITING_QUEUE_KEY_NAME + ":" + eventId;
        return redisTemplate.opsForZSet().count(waitingKey, 0, System.currentTimeMillis());
    }
}
```

### WaitingQueueController.java

```java
@RestController
@RequestMapping("/api/v1/broker")
@RequiredArgsConstructor
@Slf4j
public class WaitingQueueController {
    private final WaitingQueueEntryService waitingQueueEntryService;
    private final SseEmitterService sseEmitterService;
    private final EntryAuthService entryAuthService;
    
    @Value("${broker.instance.id:default}")
    private String instanceId;

    /**
     * SSE 기반 대기열 진입 엔드포인트
     * - 사용자를 대기열에 등록하고 실시간 상태 업데이트 제공
     */
    @AuthNeeded
    @RoleRequired({Role.USER})
    @GetMapping(value = "/events/{eventId}/tickets/waiting", 
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> joinWaitingQueue(
            @PathVariable String eventId,
            @CurrentUser User user) {
        
        String userId = user.getUserId();
        
        try {
            // 1. 대기열 진입 처리
            waitingQueueEntryService.enterWaitingQueue(eventId, userId, instanceId);
            
            // 2. SSE 연결 생성
            SseEmitter emitter = sseEmitterService.createSseConnection(userId, eventId);
            
            // 3. 초기 상태 전송
            Long position = waitingQueueEntryService.getWaitingPosition(eventId, userId);
            Long totalCount = waitingQueueEntryService.getTotalWaitingCount(eventId);
            
            sseEmitterService.sendQueuePosition(userId, position.intValue(), totalCount.intValue());
            
            // 4. 메트릭 업데이트
            Metrics.counter("broker.queue.joins", "eventId", eventId).increment();
            
            return ResponseEntity.ok()
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
                .body(emitter);
                
        } catch (Exception e) {
            log.error("대기열 진입 실패 - userId: {}, eventId: {}, error: {}", 
                userId, eventId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
```

## SSE 연결 관리 (SseEmitterService)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class SseEmitterService {
    private final Map<String, SseConnection> connections = new ConcurrentHashMap<>();
    private final RedisLockService redisLockService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * SSE 연결 생성 및 관리
     */
    public SseEmitter createSseConnection(String userId, String eventId) {
        // 1. SSE Emitter 생성 (무한 타임아웃)
        SseEmitter emitter = new SseEmitter(0L);
        
        // 2. 연결 객체 생성
        SseConnection connection = new SseConnection(emitter, Status.IN_ENTRY, eventId);
        connections.put(userId, connection);
        
        // 3. 이벤트 핸들러 설정
        emitter.onCompletion(() -> handleConnectionClose(userId, "completed"));
        emitter.onTimeout(() -> handleConnectionClose(userId, "timeout"));
        emitter.onError(throwable -> handleConnectionClose(userId, "error: " + throwable.getMessage()));
        
        log.info("SSE 연결 생성 - userId: {}, eventId: {}, 활성 연결 수: {}", 
            userId, eventId, connections.size());
            
        return emitter;
    }

    /**
     * 대기 순번 정보 전송
     */
    public void sendQueuePosition(String userId, int position, int totalWaiting) {
        SseConnection connection = connections.get(userId);
        if (connection == null) return;
        
        try {
            Map<String, Object> data = Map.of(
                "type", "QUEUE_POSITION",
                "position", position + 1, // 1부터 시작
                "totalWaiting", totalWaiting,
                "timestamp", System.currentTimeMillis()
            );
            
            connection.getEmitter().send(SseEmitter.event()
                .name("position")
                .data(objectMapper.writeValueAsString(data)));
                
        } catch (IOException e) {
            log.error("순번 정보 전송 실패 - userId: {}", userId, e);
            handleConnectionClose(userId, "send_error");
        }
    }

    /**
     * 승격 알림 전송
     */
    public void sendPromotionNotification(String userId, String entryToken) {
        SseConnection connection = connections.get(userId);
        if (connection == null) return;
        
        try {
            // 상태 업데이트
            connection.setStatus(Status.IN_PROGRESS);
            
            Map<String, Object> data = Map.of(
                "type", "PROMOTED",
                "entryToken", entryToken,
                "message", "티켓 구매가 가능합니다!",
                "timestamp", System.currentTimeMillis()
            );
            
            connection.getEmitter().send(SseEmitter.event()
                .name("promotion")
                .data(objectMapper.writeValueAsString(data)));
                
            log.info("승격 알림 전송 완료 - userId: {}", userId);
            
        } catch (IOException e) {
            log.error("승격 알림 전송 실패 - userId: {}", userId, e);
            handleConnectionClose(userId, "promotion_error");
        }
    }

    /**
     * 연결 종료 처리
     */
    private void handleConnectionClose(String userId, String reason) {
        SseConnection connection = connections.remove(userId);
        if (connection != null) {
            String eventId = connection.getEventId();
            
            // Redis에서 사용자 정보 정리
            try {
                redisLockService.releaseAllLocks(userId);
                redisLockService.releaseAllEntryQueueLocks(userId);
                
                // 대기열에서 제거
                String waitingKey = "WAITING:" + eventId;
                String recordKey = "WAITING_QUEUE_RECORD:" + eventId;
                String userIdKey = "WAITING_USER_ID:" + eventId;
                
                redisTemplate.opsForZSet().remove(waitingKey, userId);
                redisTemplate.opsForHash().delete(recordKey, userId);
                redisTemplate.opsForHash().delete(userIdKey, userId);
                
            } catch (Exception e) {
                log.error("연결 종료 중 Redis 정리 실패 - userId: {}", userId, e);
            }
            
            log.info("SSE 연결 종료 - userId: {}, eventId: {}, reason: {}, 남은 연결: {}", 
                userId, eventId, reason, connections.size());
        }
    }

    /**
     * 하트비트 전송 (연결 유지)
     */
    public void sendHeartbeat() {
        connections.forEach((userId, connection) -> {
            try {
                Map<String, Object> heartbeat = Map.of(
                    "type", "HEARTBEAT",
                    "timestamp", System.currentTimeMillis()
                );
                
                connection.getEmitter().send(SseEmitter.event()
                    .name("heartbeat")
                    .data(objectMapper.writeValueAsString(heartbeat)));
                    
            } catch (IOException e) {
                log.warn("하트비트 전송 실패, 연결 제거 - userId: {}", userId);
                handleConnectionClose(userId, "heartbeat_failed");
            }
        });
    }
}
```

## 대기열 승격 로직 (Dispatcher Service)

### EntryPromoteThread.java

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class EntryPromoteThread {
    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<List> promotionScript;
    private final AtomicLong promotionCounter;
    
    // 10개 워커 스레드를 사용한 병렬 처리
    private final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
        10, 10, 0L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(100),
        new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /**
     * 매초마다 실행되는 승격 프로세스
     * 1. 활성 대기열 탐지
     * 2. 작업 분산
     * 3. 병렬 승격 처리
     */
    @Scheduled(cron = "* * * * * *")
    @Async("singleThreadExecutor")
    public void promote() {
        try {
            // 1. 대기 중인 이벤트 ID 스캔
            Set<String> waitingKeys = redisTemplate.keys("WAITING:*");
            if (waitingKeys == null || waitingKeys.isEmpty()) {
                return;
            }

            List<String> eventIds = waitingKeys.stream()
                .map(key -> key.substring("WAITING:".length()))
                .collect(Collectors.toList());

            // 2. 임시 작업 리스트 생성 (UUID 기반)
            String taskListKey = "TEMP_PROMOTE_TASKS:" + UUID.randomUUID();
            redisTemplate.opsForList().leftPushAll(taskListKey, eventIds.toArray());
            redisTemplate.expire(taskListKey, Duration.ofMinutes(5)); // 5분 후 자동 만료

            // 3. 워커 스레드에 작업 분산
            int totalEvents = eventIds.size();
            for (int i = 0; i < Math.min(10, totalEvents); i++) {
                threadPool.submit(new PromotionWorker(taskListKey));
            }

            log.debug("승격 프로세스 시작 - 처리 대상 이벤트: {}개", totalEvents);

        } catch (Exception e) {
            log.error("승격 프로세스 실행 중 오류 발생", e);
        }
    }

    /**
     * 개별 이벤트 승격 처리 워커
     */
    private class PromotionWorker implements Runnable {
        private final String taskListKey;

        public PromotionWorker(String taskListKey) {
            this.taskListKey = taskListKey;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    // 작업 큐에서 다음 이벤트 ID 가져오기
                    String eventId = (String) redisTemplate.opsForList().rightPop(taskListKey);
                    if (eventId == null) {
                        break; // 더 이상 처리할 작업 없음
                    }

                    // 해당 이벤트에 대한 승격 처리
                    processEventPromotion(eventId);

                } catch (Exception e) {
                    log.error("워커 스레드 처리 중 오류 발생", e);
                }
            }
        }
    }

    /**
     * 특정 이벤트의 승격 처리
     * Lua 스크립트를 사용한 원자적 연산
     */
    private void processEventPromotion(String eventId) {
        try {
            List<String> keys = Arrays.asList(
                "WAITING:" + eventId,                    // 대기 큐
                "ENTRY_QUEUE_COUNT:" + eventId,          // 진입 가능 인원
                "WAITING_QUEUE_RECORD:" + eventId,       // 사용자 메타데이터
                "WAITING_USER_ID:" + eventId,           // 중복 방지 키
                "ENTRY"                                 // 진입 스트림
            );

            List<String> args = Arrays.asList(
                eventId,
                String.valueOf(RedisConfig.ENTRY_QUEUE_CAPACITY) // 1000명
            );

            // Lua 스크립트 실행
            @SuppressWarnings("unchecked")
            List<Object> result = redisTemplate.execute(promotionScript, keys, args.toArray());

            if (result != null && result.size() >= 2) {
                Long promotedCount = (Long) result.get(0);
                if (promotedCount > 0) {
                    promotionCounter.addAndGet(promotedCount);
                    log.info("이벤트 {} 승격 완료: {}명", eventId, promotedCount);
                }
            }

        } catch (Exception e) {
            log.error("이벤트 {} 승격 처리 실패", eventId, e);
        }
    }

    @PreDestroy
    public void shutdown() {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }
    }
}
```

## Lua 스크립트 기반 원자적 승격 처리

### promote_all_waiting_for_event.lua

```lua
-- 이벤트의 모든 대기 사용자를 진입 큐로 원자적 승격
-- KEYS[1]: WAITING:{eventId}
-- KEYS[2]: ENTRY_QUEUE_COUNT:{eventId} 
-- KEYS[3]: WAITING_QUEUE_RECORD:{eventId}
-- KEYS[4]: WAITING_USER_ID:{eventId}
-- KEYS[5]: ENTRY (stream name)
-- ARGV[1]: eventId
-- ARGV[2]: capacity (1000)

local eventId = ARGV[1]
local capacity = tonumber(ARGV[2])

local waitingKey = KEYS[1]
local countKey = KEYS[2] 
local recordKey = KEYS[3]
local userIdKey = KEYS[4]
local streamKey = KEYS[5]

-- 현재 진입 큐 인원 확인
local currentCount = redis.call('GET', countKey) or 0
local availableCapacity = capacity - tonumber(currentCount)

if availableCapacity <= 0 then
    return {0, "진입 큐 용량 초과"}
end

-- 대기 중인 사용자 조회 (시간순으로 정렬됨)
local waitingUsers = redis.call('ZRANGE', waitingKey, 0, availableCapacity - 1, 'WITHSCORES')

if #waitingUsers == 0 then
    return {0, "대기 중인 사용자 없음"}
end

-- JSON 처리를 위한 cjson 라이브러리 사용
local cjson = require "cjson"
local promotedCount = 0
local promotedUsers = {}

-- 각 사용자를 순차적으로 처리
for i = 1, #waitingUsers, 2 do
    local userId = waitingUsers[i]
    local timestamp = waitingUsers[i + 1]
    
    -- 사용자 메타데이터 조회
    local userDataJson = redis.call('HGET', recordKey, userId)
    if userDataJson then
        local userData = cjson.decode(userDataJson)
        
        -- 대기 구조에서 사용자 제거
        redis.call('ZREM', waitingKey, userId)
        redis.call('HDEL', recordKey, userId) 
        redis.call('HDEL', userIdKey, userId)
        
        -- 진입 스트림에 승격 정보 추가
        local streamData = {
            'userId', userId,
            'eventId', eventId,
            'promotedAt', timestamp,
            'instanceId', userData.instanceId or 'unknown',
            'seatCount', userData.seatCount or 0
        }
        
        redis.call('XADD', streamKey, '*', unpack(streamData))
        
        promotedCount = promotedCount + 1
        table.insert(promotedUsers, userId)
    end
end

-- 진입 큐 카운터 증가
if promotedCount > 0 then
    redis.call('INCRBY', countKey, promotedCount)
    -- 카운터 만료 시간 설정 (1시간)
    redis.call('EXPIRE', countKey, 3600)
end

return {promotedCount, promotedUsers}
```

## 스트림 메시지 처리 (EntryQueueConsumer)

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class EntryQueueConsumer {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * ENTRY 스트림 메시지 처리
     * 승격된 사용자를 DISPATCH 스트림으로 전달
     */
    @StreamListener(value = RedisConfig.ENTRY_QUEUE_KEY_NAME, 
                   group = RedisConfig.ENTRY_QUEUE_GROUP_NAME)
    public void processPromotedUsers(Record<String, Object> message) {
        try {
            Map<String, Object> data = message.getValue();
            
            String userId = (String) data.get("userId");
            String eventId = (String) data.get("eventId");
            String instanceId = (String) data.get("instanceId");
            String promotedAt = (String) data.get("promotedAt");
            
            // DISPATCH 스트림으로 브로커 알림 전송
            Map<String, Object> dispatchData = Map.of(
                "userId", userId,
                "eventId", eventId,
                "instanceId", instanceId,
                "promotedAt", promotedAt,
                "dispatchedAt", System.currentTimeMillis()
            );
            
            redisTemplate.opsForStream().add(
                RedisConfig.DISPATCH_QUEUE_CHANNEL_NAME,
                dispatchData
            );
            
            log.debug("사용자 {}를 DISPATCH 스트림으로 전달 완료", userId);
            
            // 메시지 처리 완료 확인
            redisTemplate.opsForStream().acknowledge(
                RedisConfig.ENTRY_QUEUE_KEY_NAME,
                RedisConfig.ENTRY_QUEUE_GROUP_NAME,
                message.getId()
            );
            
        } catch (Exception e) {
            log.error("ENTRY 스트림 메시지 처리 실패: {}", message.getId(), e);
        }
    }
}
```

## 실시간 상태 업데이트 (QueueInfoScheduler)

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class QueueInfoScheduler {
    private final SseEmitterService sseEmitterService;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 매초마다 대기 순번 정보 브로드캐스트
     */
    @Scheduled(cron = "* * * * * *")
    public void broadcastQueueInfo() {
        try {
            Set<String> waitingKeys = redisTemplate.keys("WAITING:*");
            if (waitingKeys == null) return;

            for (String waitingKey : waitingKeys) {
                String eventId = waitingKey.substring("WAITING:".length());
                updateQueuePositions(eventId, waitingKey);
            }
            
        } catch (Exception e) {
            log.error("대기열 정보 브로드캐스트 실패", e);
        }
    }

    /**
     * 특정 이벤트의 모든 대기자에게 순번 정보 전송
     */
    private void updateQueuePositions(String eventId, String waitingKey) {
        try {
            // 모든 대기자 조회 (시간순)
            Set<ZSetOperations.TypedTuple<Object>> waitingUsers = 
                redisTemplate.opsForZSet().rangeWithScores(waitingKey, 0, -1);
                
            if (waitingUsers == null || waitingUsers.isEmpty()) {
                return;
            }

            int totalWaiting = waitingUsers.size();
            int position = 0;

            for (ZSetOperations.TypedTuple<Object> user : waitingUsers) {
                String userId = (String) user.getValue();
                sseEmitterService.sendQueuePosition(userId, position, totalWaiting);
                position++;
            }

        } catch (Exception e) {
            log.error("이벤트 {} 순번 업데이트 실패", eventId, e);
        }
    }

    /**
     * 5초마다 하트비트 전송 (연결 유지)
     */
    @Scheduled(cron = "*/5 * * * * *")
    public void sendHeartbeat() {
        try {
            sseEmitterService.sendHeartbeat();
        } catch (Exception e) {
            log.error("하트비트 전송 실패", e);
        }
    }
}
```

## 승격 알림 처리 (EntryStreamMessageListener)

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class EntryStreamMessageListener {
    private final SseEmitterService sseEmitterService;
    private final EntryAuthService entryAuthService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${broker.instance.id:default}")
    private String instanceId;

    /**
     * DISPATCH 스트림 메시지 수신 및 처리
     * 승격된 사용자에게 알림 전송
     */
    @StreamListener(value = "DISPATCH", group = "BROKER_GROUP")
    public void handlePromotionMessage(Record<String, Object> message) {
        try {
            Map<String, Object> data = message.getValue();
            
            String userId = (String) data.get("userId");
            String eventId = (String) data.get("eventId");
            String targetInstanceId = (String) data.get("instanceId");
            
            // 해당 인스턴스의 연결만 처리
            if (!instanceId.equals(targetInstanceId)) {
                return;
            }
            
            // 진입 인증 토큰 생성
            String entryToken = entryAuthService.generateEntryAuthToken(userId, eventId);
            
            // SSE를 통한 승격 알림 전송
            sseEmitterService.sendPromotionNotification(userId, entryToken);
            
            // 진입 큐 카운터 감소 (사용자가 진입 완료됨)
            String countKey = "ENTRY_QUEUE_COUNT:" + eventId;
            redisTemplate.opsForValue().decrement(countKey);
            
            log.info("승격 알림 처리 완료 - userId: {}, eventId: {}", userId, eventId);
            
            // 메시지 ACK
            redisTemplate.opsForStream().acknowledge("DISPATCH", "BROKER_GROUP", message.getId());
            
        } catch (Exception e) {
            log.error("승격 메시지 처리 실패: {}", message.getId(), e);
        }
    }

    /**
     * 진입 인증 토큰 생성 서비스
     */
    @Service
    public static class EntryAuthService {
        
        @Value("${broker.jwt.secret}")
        private String jwtSecret;
        
        @Value("${broker.jwt.expiration:300000}") // 5분
        private long tokenExpiration;

        public String generateEntryAuthToken(String userId, String eventId) {
            return Jwts.builder()
                .setSubject(userId)
                .claim("eventId", eventId)
                .claim("purpose", "QUEUE_ENTRY")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + tokenExpiration))
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
        }
    }
}
```

## 성능 모니터링

### PromotionCounterThread.java

```java
@Component
@RequiredArgsConstructor
@Slf4j  
public class PromotionCounterThread {
    private final AtomicLong promotionCounter;
    private final MeterRegistry meterRegistry;

    /**
     * 매초마다 승격 처리량(TPS) 모니터링
     */
    @Scheduled(fixedRate = 1000)
    public void reportThroughput() {
        long currentCount = promotionCounter.getAndSet(0);
        
        if (currentCount > 0) {
            // 메트릭 업데이트
            Metrics.gauge("dispatcher.promotions.per.second", currentCount);
            
            log.info("승격 처리량: {}명/초", currentCount);
            
            // 높은 처리량 알람
            if (currentCount > 500) {
                log.warn("높은 승격 처리량 감지: {}명/초", currentCount);
            }
        }
    }

    /**
     * 시스템 상태 모니터링
     */
    @Scheduled(fixedRate = 10000) // 10초마다
    public void monitorSystemHealth() {
        try {
            // Redis 연결 상태 확인
            String ping = redisTemplate.getConnectionFactory().getConnection().ping();
            Metrics.gauge("dispatcher.redis.health", "OK".equals(ping) ? 1 : 0);
            
            // 메모리 사용량 모니터링
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            Metrics.gauge("dispatcher.memory.used", usedMemory);
            
        } catch (Exception e) {
            log.error("시스템 상태 모니터링 실패", e);
            Metrics.gauge("dispatcher.redis.health", 0);
        }
    }
}
```

## 핵심 설계 원칙 및 특징

### 1. 원자적 연산 보장
- **Lua 스크립트**: 모든 승격 처리가 단일 원자적 연산으로 실행
- **롤백 메커니즘**: 실패 시 자동으로 이전 상태로 복구
- **일관성 보장**: Redis의 단일 스레드 특성을 활용한 상태 일관성

### 2. 확장성 설계
- **수평 확장**: 브로커 인스턴스 추가로 처리량 증대
- **멀티스레딩**: 디스패처의 10개 워커 스레드 병렬 처리
- **무상태 설계**: 모든 상태를 Redis에 저장하여 인스턴스 독립성 확보

### 3. 장애 복구 능력
- **우아한 종료**: 진행 중인 작업 완료 후 종료
- **연결 복구**: SSE 연결 실패 시 자동 정리 및 복구
- **데이터 정합성**: 임시 데이터 자동 만료로 메모리 누수 방지

### 4. 실시간 성능
- **1초 간격**: 승격 처리 및 순번 업데이트
- **5초 하트비트**: SSE 연결 유지
- **즉시 알림**: 승격 시 실시간 사용자 알림

이 시스템은 초당 수천 명의 동시 사용자를 안정적으로 처리할 수 있는 고성능 대기열 솔루션으로, 공정성과 확장성을 모두 보장하는 설계입니다.