# Dispatcher Module Changes

## Overview
The dispatcher module has been modified to improve the performance and scalability of the queue promotion process. The changes implement a multithreaded approach with a temporary task list in Redis, allowing for parallel processing of waiting queue entries.

## Changes Made

### 1. Redis Temporary Task List Creation
- Added a constant `PROMOTION_TASK_LIST_KEY` for the task list key
- Created a unique task list key using UUID for each promotion cycle
- Added each event ID to the task list using Redis List data structure (RPUSH)
- Set an expiration time of 5 minutes for the task list to prevent stale data

```java
// Redis에 임시 작업 목록 생성
String taskListId = UUID.randomUUID().toString();
String taskListKey = PROMOTION_TASK_LIST_KEY + ":" + taskListId;

// 각 키를 작업 목록에 추가
for (int i = 0; i < keys.size(); i++) {
    String key = keys.get(i);
    String eventId = key.split(":")[1];
    // 작업 정보를 Redis 리스트에 저장
    redisTemplate.opsForList().rightPush(taskListKey, eventId);
}

// 작업 목록 만료 시간 설정 (5분)
redisTemplate.expire(taskListKey, 5, TimeUnit.MINUTES);
```

### 2. Multithreading for Task Processing
- Added an ExecutorService with a fixed thread pool size of 10
- Created a new method `processPromotionTasks` that each thread executes
- Each thread takes one task from the list using LPOP and processes it
- If there are no more tasks, the thread terminates

```java
// 멀티스레딩으로 작업 처리
for (int i = 0; i < THREAD_POOL_SIZE; i++) {
    executorService.submit(() -> processPromotionTasks(taskListKey));
}

// Task processing method
private void processPromotionTasks(String taskListKey) {
    try {
        while (true) {
            // 작업 목록에서 작업 가져오기 (LPOP 사용)
            String eventId = (String) redisTemplate.opsForList().leftPop(taskListKey);
            if (eventId == null) {
                // 더 이상 작업이 없으면 종료
                break;
            }
            
            // 작업 처리 (Lua 스크립트 실행)
            executePromotionScript(eventId);
        }
    } catch (Exception e) {
        log.error("Error processing promotion tasks: {}", e.getMessage(), e);
    }
}
```

### 3. Lua Script Execution for Queue Promotion
- Created a new method `executePromotionScript` that executes the Lua script for a given event ID
- The script execution logic is the same as before, but now it's executed by multiple threads in parallel

```java
private void executePromotionScript(String eventId) {
    try {
        String entryCountHashKey = ENTRY_QUEUE_SLOTS_KEY_NAME;
        String waitingRecordHash = "WAITING_QUEUE_INDEX_RECORD:" + eventId;
        String waitingZsetKey = WAITING_QUEUE_KEY_NAME + ":" + eventId;
        String waitingInUserHash = WAITING_USER_IDS_KEY_NAME + ":" + eventId;
        String entryStreamKey = ENTRY_QUEUE_KEY_NAME;

        List<String> scriptKeys = List.of(
            entryCountHashKey,
            waitingRecordHash,
            waitingZsetKey,
            waitingInUserHash,
            entryStreamKey
        );

        // Lua 스크립트 실행
        Long cnt = redisTemplate.execute(
            promoteAllScript,
            scriptKeys,
            eventId
        );
        
        if (cnt != null && cnt > 0) {
            promotionCounter.addAndGet(cnt);
            log.debug("Promoted {} users for event {}", cnt, eventId);
        }
    } catch (Exception e) {
        log.error("Error executing promotion script for event {}: {}", eventId, e.getMessage(), e);
    }
}
```

## Benefits
1. **Improved Performance**: Multiple events can be processed in parallel, reducing the overall processing time.
2. **Better Resource Utilization**: The system can utilize multiple CPU cores efficiently.
3. **Scalability**: The thread pool size can be adjusted based on the system's resources.
4. **Reliability**: The temporary task list ensures that each task is processed exactly once, even if a thread fails.

## Implementation Notes
- The thread pool size is set to 10 by default, but can be adjusted based on the system's resources.
- The task list expires after 5 minutes to prevent stale data in Redis.
- Error handling is implemented to ensure that exceptions in one thread don't affect others.
- Logging is added to track the progress and diagnose issues.
