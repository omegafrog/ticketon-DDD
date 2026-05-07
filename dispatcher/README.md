# Dispatcher Service

The Dispatcher service is the queue promotion engine that efficiently moves users from waiting queues to entry queues using sophisticated multithreaded processing and atomic Redis operations.

## Admission Control

Dispatcher promotes waiting users into active shopper slots, not seat inventory. Seat count remains business inventory. Active shopper slots protect purchase/payment resources such as Tomcat threads, DB connections, Redis locks, payment preparation, and payment approval.

`ENTRY_QUEUE_SLOTS` remains for compatibility. Its meaning is available active shopper admission capacity per event.

Promotion per tick is bounded by all limits:

```text
effectivePromotionCount = min(availableSlots, promotionBatchSize, configuredRateBudgetForThisTick)
configuredRateBudgetForThisTick = newUsersPerMinute * promotionIntervalMs / 60000
```

Recommended defaults:

```yaml
queue:
  max-active-shoppers: 500
  new-users-per-minute: 3000
  promotion-batch-size: 50
  promotion-interval-ms: 1000
  entry-token-ttl-minutes: 10
  polling-interval-seconds: 5
  max-polling-interval-seconds: 15
```

Load reduction:

```text
loadReduction = 1 - maxActiveShoppers / concurrentUsers
1 - 500 / 5000 = 0.9
```

For 5,000 concurrent users and `maxActiveShoppers = 500`, purchase-flow direct load is reduced by 90%, roughly 10x isolation.

Load test guidance:

```bash
QUEUE_MAX_ACTIVE_SHOPPERS=500 \
QUEUE_PROMOTION_BATCH_SIZE=50 \
QUEUE_NEW_USERS_PER_MINUTE=3000 \
EVENT_ID=<event-id> \
k6 run k6/admission-control-5000.js
```

Validate active shoppers never exceed 500, promotion rate is about 50/sec, purchase service request volume is about 90% lower than no admission control, and p95 purchase/payment API latency improves against baseline.

## 🎯 Purpose

- **Queue Promotion Engine**: Automated user promotion from waiting to entry queues
- **High-Throughput Processing**: Multi-threaded parallel processing for scalability
- **Atomic Operations**: Consistent queue state management using Redis Lua scripts
- **Real-time Monitoring**: Performance tracking and throughput metrics

## 🏗️ Architecture

### Data Flow Architecture

```sequence
title: Queue Promotion Processing Flow

Scheduler->Redis: SCAN WAITING:* (find active queues)
note over Scheduler: Every 1 Second
Redis-->Scheduler: [WAITING:event1, WAITING:event2, ...]

Scheduler->Redis: Create temporary task list with UUID
Scheduler->Redis: LPUSH temp:{uuid} event1 event2 event3...

par Multi-threaded Processing (10 threads)
    Worker->Redis: RPOP temp:{uuid} (get next event)
    Redis-->Worker: eventId
    Worker->Redis: Execute promote_all_waiting_for_event.lua
    
    note over Redis: Atomic Operations in Lua Script
    Redis->Redis: Check ENTRY queue capacity
    Redis->Redis: Move users from WAITING to ENTRY
    Redis->Redis: Update queue counters
    Redis->Redis: XADD to ENTRY stream
    
    Worker->ThreadPool: Task completed
end

Consumer->Redis: XREAD ENTRY stream (consumer group)
Redis-->Consumer: Promoted user messages
Consumer->Redis: XADD DISPATCH stream
Redis-->Broker: DISPATCH stream notification

Consumer->Redis: XACK ENTRY stream (acknowledge)
```

## ⚙️ Core Components

### Thread Management

#### EntryPromoteThread (Main Processing Engine)

```java
@Component
@Slf4j  
public class EntryPromoteThread {
    
    @Scheduled(cron = "* * * * * *")  // Every second
    @Async("singleThreadExecutor")
    public void promote() {
        // 1. Discovery: Find all waiting queues
        // 2. Task Distribution: Create temporary task list
        // 3. Parallel Execution: Submit to thread pool
        // 4. Cleanup: Remove temporary structures
    }
}
```

**Thread Pool Configuration:**
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    10,                                    // Core threads
    10,                                    // Maximum threads  
    0L, TimeUnit.MILLISECONDS,            // Keep-alive time
    new LinkedBlockingQueue<>(100),       // Task queue capacity
    new ThreadPoolExecutor.CallerRunsPolicy()  // Backpressure policy
);
```

**Processing Flow:**
1. **Discovery Phase**: Scan Redis for `WAITING:*` keys
2. **Task Distribution**: Create temporary Redis list with UUID
3. **Parallel Execution**: 10 worker threads process events
4. **Atomic Promotion**: Execute Lua script per event
5. **Cleanup**: Remove temporary task lists

#### EntryQueueConsumer (Stream Processing)

```java
@Component
@RedisStreamListener
public class EntryQueueConsumer {
    
    @StreamListener(value = "ENTRY", group = "ENTRY_QUEUE")
    public void processPromotedUsers(Record<String, Object> message) {
        // Extract user data from stream message
        // Forward to DISPATCH channel for broker notification  
        // Acknowledge message processing
    }
}
```

**Consumer Configuration:**
- **Consumer Group**: `ENTRY_QUEUE`
- **Batch Size**: 10 messages
- **Poll Timeout**: 2 seconds
- **Auto-acknowledge**: Manual ACK after processing

#### PromotionCounterThread (Performance Monitoring)

```java
@Component
public class PromotionCounterThread {
    private final AtomicLong promotionCounter;
    
    @Scheduled(fixedRate = 1000)  // Every 1 second
    public void reportThroughput() {
        long currentCount = promotionCounter.getAndSet(0);
        if (currentCount > 0) {
            log.info("Promotions per second: {}", currentCount);
        }
    }
}
```

### Redis Configuration

#### Key Constants and Settings

```java
@Configuration
public class RedisConfig {
    // Queue naming
    public static final String WAITING_QUEUE_KEY_NAME = "WAITING";
    public static final String ENTRY_QUEUE_KEY_NAME = "ENTRY"; 
    public static final String DISPATCH_QUEUE_CHANNEL_NAME = "DISPATCH";
    
    // Consumer groups
    public static final String WAITING_QUEUE_GROUP_NAME = "WAITING_QUEUE";
    public static final String ENTRY_QUEUE_GROUP_NAME = "ENTRY_QUEUE";
    
    // Capacity limits
    public static final Integer ENTRY_QUEUE_CAPACITY = 1000;
}
```

#### Stream Container Configuration

```java
@Bean
public StreamMessageListenerContainer<String, Record<String, Object>> streamContainer() {
    StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, Record<String, Object>> options = 
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions
            .builder()
            .batchSize(10)                    // Process 10 messages at once
            .pollTimeout(Duration.ofSeconds(2)) // Wait 2s for messages
            .build();
            
    return StreamMessageListenerContainer.create(connectionFactory, options);
}
```

## 🔬 Atomic Promotion Logic

### Lua Script: promote_all_waiting_for_event.lua

The core promotion logic is implemented as a Redis Lua script for atomic execution:

```lua
-- Input parameters
local eventId = ARGV[1]
local capacity = tonumber(ARGV[2])

-- Key construction
local waitingKey = "WAITING:" .. eventId
local entryCountKey = "ENTRY_QUEUE_SLOTS:" .. eventId  
local recordKey = "WAITING_QUEUE_INDEX_RECORD:" .. eventId
local userIdKey = "WAITING_USER_IDS:" .. eventId

-- Check available capacity
local currentCount = redis.call('GET', entryCountKey) or 0
local availableCapacity = capacity - tonumber(currentCount)

if availableCapacity <= 0 then
    return {0, "No available capacity"}
end

-- Get waiting users (ordered by timestamp)
    local waitingUsers = redis.call('ZRANGE', waitingKey, 0, availableCapacity - 1, 'WITHSCORES')

if #waitingUsers == 0 then
    return {0, "No waiting users"}  
end

-- Process each user atomically
local promotedCount = 0
local promotedUsers = {}

for i = 1, #waitingUsers, 2 do
    local userId = waitingUsers[i]
    local timestamp = waitingUsers[i + 1]
    
    -- Get user metadata
    local userData = redis.call('HGET', recordKey, userId)
    if userData then
        local userInfo = cjson.decode(userData)
        
        -- Remove from waiting structures  
        redis.call('ZREM', waitingKey, userId)
        redis.call('HDEL', recordKey, userId)
        redis.call('HDEL', userIdKey, userId)
        
        -- Add to entry stream
        local streamData = {
            'userId', userId,
            'eventId', eventId,
            'promotedAt', timestamp,
            'instanceId', userInfo.instanceId or 'unknown'
        }
        
        redis.call('XADD', 'ENTRY', '*', unpack(streamData))
        promotedCount = promotedCount + 1
        table.insert(promotedUsers, userId)
    end
end

-- Update capacity counter  
redis.call('INCRBY', entryCountKey, promotedCount)

return {promotedCount, promotedUsers}
```

### 문제와 해결 방법

**문제(고부하 위험)**  
- 기존 스크립트가 `ZRANGE 0 -1`로 대기열 전체를 읽고 순회하여, 특정 이벤트 대기열이 커질수록 Redis 단일 스레드를 장시간 점유할 수 있습니다.  
- 중간에 자리가 부족해지면 `return 0`으로 종료하면서도 앞선 승격은 이미 반영되어, “리턴값 vs 실제 승격 수”가 어긋날 수 있습니다.

**해결 방법(현재 적용됨)**  
- `ENTRY_QUEUE_SLOTS[eventId]`를 읽어 **승격 가능한 수(capacity)** 만큼만 가져오고 처리합니다.  
- `ZRANGE 0 (capacity-1)`로 제한하여 **필요한 만큼만** 처리하며, 리턴값은 실제 승격 수와 일치합니다.  
- 결과적으로 이벤트 단위 처리이더라도 **대기열 크기에 비례한 선형 스캔**을 피하고, 고부하 상황에서 Redis 블로킹 시간을 줄입니다.

**문제(고부하 위험: 전수 키 스캔)**  
- 기존 구현은 `KEYS WAITING:*`로 모든 대기열 키를 조회하여, Redis 전체 키스페이스를 블로킹 스캔했습니다.

**해결 방법(현재 적용됨)**  
- `SCAN` 기반 조회로 전환하여 커서 방식의 비블로킹 스캔을 사용합니다.  
- 전체 조회는 유지하면서도 Redis 단일 스레드 점유 시간을 줄여 고부하 상황의 응답 지연을 완화합니다.

**Key Features:**
- **Atomic Execution**: All operations execute atomically
- **Capacity Management**: Respects entry queue limits  
- **Error Handling**: Graceful handling of edge cases
- **Rollback Safety**: Consistent state on any failure
- **Performance**: Bulk operations for efficiency

## 📊 Performance Characteristics

### Throughput Metrics

**Target Performance:**
- **Promotion Rate**: 1000+ users per second
- **Processing Latency**: <50ms per promotion batch
- **Thread Utilization**: 80-90% under normal load  
- **Memory Usage**: <2GB heap for 10,000 concurrent queues

**Monitoring:**
```java
// Metrics exposed via Micrometer
Counter.builder("dispatcher.promotions.total")
    .description("Total number of promotions processed")
    .register(meterRegistry);

Timer.Sample.start(meterRegistry)
    .stop(Timer.builder("dispatcher.promotion.duration")
        .description("Time taken to process promotions")
        .register(meterRegistry));
```

### Scalability Features

**Thread Pool Tuning:**
```java
// Adjust based on CPU cores and load patterns  
int coreThreads = Runtime.getRuntime().availableProcessors() * 2;
int maxThreads = coreThreads;
int queueCapacity = 1000;  // Adjust based on memory constraints
```

**Backpressure Management:**
- **CallerRunsPolicy**: Prevents thread pool overflow
- **Queue Capacity**: Limits pending tasks to prevent OOM
- **Circuit Breaker**: Future enhancement for Redis failures

## 🔧 Configuration

### Application Properties

```yaml
server:
  port: 9002
  shutdown: graceful  # Graceful shutdown for in-flight processing

spring:
  application:
    name: dispatcher
  data:
    redis:
      host: localhost  
      port: 6379
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5

dispatcher:
  thread-pool:
    core-size: 10
    max-size: 10  
    queue-capacity: 100
  processing:
    batch-size: 10
    poll-timeout: 2000ms
  monitoring:
    metrics-interval: 1000ms
```

### Environment-Specific Configuration

**Development (`application-dev.yml`):**
```yaml  
spring:
  data:
    redis:
      host: localhost
      
logging:
  level:
    org.codenbug.messagedispatcher: DEBUG
```

**Production (`application-prod.yml`):**
```yaml
spring:
  data:
    redis:
      host: env-redis-1  # Docker Compose service name
      
logging:
  level:
    org.codenbug.messagedispatcher: INFO
    
management:
  endpoint:
    health:
      show-details: always
```

## 🚀 Development

### Running the Service

```bash
# Development mode
./gradlew :dispatcher:bootRun

# With specific profile  
./gradlew :dispatcher:bootRun --args='--spring.profiles.active=dev'

# Build Docker image
./gradlew :dispatcher:bootBuildImage

# Run with Docker
docker run -p 9002:9002 -e SPRING_PROFILES_ACTIVE=prod dispatcher:latest
```

### Testing Queue Promotion

#### Setup Test Data
```bash
# Add users to waiting queue
redis-cli ZADD WAITING:event123 1642845600 user1
redis-cli ZADD WAITING:event123 1642845601 user2  
redis-cli ZADD WAITING:event123 1642845602 user3

# Add metadata
redis-cli HSET WAITING_QUEUE_INDEX_RECORD:event123 user1 '{"instanceId":"test","joinedAt":"2025-01-22T10:00:00Z"}'
```

#### Monitor Processing
```bash
# Watch promotions in real-time
redis-cli MONITOR

# Check entry stream
redis-cli XREAD COUNT 10 STREAMS ENTRY 0-0

# Monitor thread pool metrics  
curl http://localhost:9002/actuator/metrics/executor.active
```

### Load Testing

```bash
# Generate test load
for i in {1..1000}; do
    redis-cli ZADD WAITING:event123 $((1642845600 + i)) user$i
    redis-cli HSET WAITING_QUEUE_INDEX_RECORD:event123 user$i "{\"instanceId\":\"test\",\"joinedAt\":\"2025-01-22T10:00:00Z\"}"
done

# Monitor promotion throughput
watch -n 1 'redis-cli XLEN ENTRY'
```

## 📈 Monitoring & Observability

### Health Checks

```http
GET /actuator/health
GET /actuator/metrics  
GET /actuator/prometheus
```

**Custom Health Indicators:**
```java
@Component  
public class DispatcherHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        // Check Redis connectivity
        // Verify thread pool status  
        // Monitor error rates
        return Health.up()
            .withDetail("threadPoolActive", threadPool.getActiveCount())
            .withDetail("queueSize", threadPool.getQueue().size())
            .build();
    }
}
```

### Metrics Exposed

| Metric | Description | Type |
|--------|-------------|------|
| `dispatcher_promotions_total` | Total promotions processed | Counter |
| `dispatcher_promotion_duration` | Time per promotion batch | Timer |  
| `dispatcher_thread_pool_active` | Active thread count | Gauge |
| `dispatcher_queue_size` | Pending task queue size | Gauge |
| `dispatcher_lua_script_duration` | Lua script execution time | Timer |
| `dispatcher_redis_errors_total` | Redis operation failures | Counter |

### Logging Configuration

```yaml
logging:
  level:
    org.codenbug.messagedispatcher: INFO
    org.springframework.data.redis: DEBUG
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level [%X{correlationId}] %logger{36} - %msg%n"
    file: "%d{ISO8601} [%thread] %-5level [%X{correlationId}] %logger{36} - %msg%n"
  file:
    name: logs/dispatcher.log
    max-size: 100MB
    max-history: 30
```

## 🚨 Error Handling & Resilience

### Error Scenarios

#### Redis Connection Failures
```java
@Retryable(value = {RedisConnectionException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
public void promoteWithRetry(String eventId) {
    // Promotion logic with automatic retry
}
```

#### Thread Pool Exhaustion  
```java
// CallerRunsPolicy ensures no task rejection
// Monitor queue size to detect saturation
if (threadPool.getQueue().size() > maxQueueSize * 0.8) {
    log.warn("Thread pool queue approaching capacity: {}/{}", 
             threadPool.getQueue().size(), maxQueueSize);
}
```

#### Lua Script Failures
```java
try {
    Object result = redisTemplate.execute(promotionScript, keys, args);
    processPromotionResult(result);
} catch (RedisSystemException e) {
    log.error("Lua script execution failed for event {}: {}", eventId, e.getMessage());
    // Implement fallback logic or circuit breaker
}
```

### Circuit Breaker Pattern

```java
@Component
public class RedisCircuitBreaker {
    private final CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("redis");
    
    public <T> T executeWithCircuitBreaker(Supplier<T> operation) {
        return circuitBreaker.executeSupplier(operation);
    }
}
```

## 🔒 Security Considerations

### Internal Service Communication
- **Network Isolation**: Run in private network/VPC
- **Authentication**: Service-to-service auth via shared secrets
- **Rate Limiting**: Prevent abuse of internal APIs
- **Input Validation**: Sanitize Redis keys and values

### Resource Protection
- **Memory Limits**: JVM heap size constraints
- **CPU Throttling**: Container resource limits
- **Connection Pooling**: Prevent Redis connection exhaustion
- **Graceful Degradation**: Handle Redis outages gracefully

## 🔄 Integration Points

### Upstream Dependencies
- **Broker Service**: Provides waiting queue data via Redis
- **Redis**: Primary data store for queue state

### Downstream Dependencies
- **Broker Service**: Consumes DISPATCH stream messages
- **Redis Streams**: Message delivery to broker instances

### Message Contracts

**ENTRY Stream Messages:**
```json
{
  "userId": "user123",
  "eventId": "event456",
  "promotedAt": "1642845600",
  "instanceId": "dispatcher-1"
}
```

**DISPATCH Stream Messages:**
```json  
{
  "userId": "user123",
  "eventId": "event456",
  "instanceId": "broker-2", 
  "entryToken": "jwt_token_here"
}
```

## ⚡ Performance Tuning

### JVM Tuning
```bash
# Recommended JVM flags for production
java -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -Xms2g -Xmx4g \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/tmp/dispatcher-heap.dump \
     -jar dispatcher.jar
```

### Redis Optimization
```bash
# Redis configuration tuning
maxmemory 4gb
maxmemory-policy allkeys-lru
lua-time-limit 60000  # 60 seconds for Lua scripts
```

### Thread Pool Tuning
```java
// Adjust based on profiling results
ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
executor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 2);
executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 4);  
executor.setQueueCapacity(1000);
executor.setThreadNamePrefix("dispatcher-");
executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
executor.initialize();
```

The Dispatcher service is engineered for high-throughput, low-latency queue processing with robust error handling and comprehensive monitoring capabilities.
