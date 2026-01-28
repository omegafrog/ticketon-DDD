package org.codenbug.messagedispatcher.thread;

import static org.codenbug.messagedispatcher.redis.RedisConfig.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EntryPromoter {

  private final RedisTemplate<String, Object> redisTemplate;
  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;
  private final DefaultRedisScript<Long> promoteAllScript;
  private final AtomicLong promotionCounter;
  private final ExecutorService executorService;

  private static final String PROMOTION_TASK_LIST_KEY = "PROMOTION_TASK_LIST";
  private static final String EVENT_STATUSES_HASH_KEY = "event_statuses";
  private static final int THREAD_POOL_SIZE = 10; // 스레드 풀 크기 설정
  private static final int QUEUE_CAPACITY = 100; // 작업 큐 용량 설정

  public EntryPromoter(RedisTemplate<String, Object> redisTemplate,
      StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper,
      AtomicLong promotionCounter) {
    this.redisTemplate = redisTemplate;
    this.stringRedisTemplate = stringRedisTemplate;
    this.objectMapper = objectMapper;
    this.promotionCounter = promotionCounter;

    // ThreadPoolExecutor with CallerRunsPolicy for backpressure
    this.executorService = new ThreadPoolExecutor(THREAD_POOL_SIZE, // corePoolSize
        THREAD_POOL_SIZE, // maximumPoolSize
        0L, TimeUnit.MILLISECONDS, // keepAliveTime
        new LinkedBlockingQueue<>(QUEUE_CAPACITY), // workQueue with capacity limit
        new ThreadPoolExecutor.CallerRunsPolicy() // rejection policy for backpressure
    );

    promoteAllScript = new DefaultRedisScript<>();
    promoteAllScript.setScriptText(loadLuaScriptFromResource("promote_all_waiting_for_event.lua"));
    promoteAllScript.setResultType(Long.class);
  }

  @PreDestroy
  public void shutdown() {
    log.info("Shutting down EntryPromoteThread executor service...");
    executorService.shutdown();
    try {
      if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
        log.warn("Executor did not terminate within 30 seconds, forcing shutdown...");
        executorService.shutdownNow();
      }
    } catch (InterruptedException e) {
      log.error("Thread was interrupted during shutdown", e);
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  private String loadLuaScriptFromResource(String scriptName) {
    try (InputStream is = new ClassPathResource(scriptName).getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
      return reader.lines().collect(Collectors.joining("\n"));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Scheduled(fixedRate = 1000)
  public void promoteToEntryQueue() {
    try {
      // 1) waiting 스트림의 모든 레코드를 조회
      List<String> keys = scanWaitingQueueKeys();
      if (keys.isEmpty())
        return;

      // 2) Redis에 임시 작업 목록 생성
      String taskListId = UUID.randomUUID().toString();
      String taskListKey = PROMOTION_TASK_LIST_KEY + ":" + taskListId;

      // 각 키를 작업 목록에 추가
      for (int i = 0; i < keys.size(); i++) {
        String key = keys.get(i);
        String[] parts = key.split(":");
        if (parts.length < 2) {
          log.warn("Skipping unexpected waiting key format: {}", key);
          continue;
        }
        String eventId = parts[1];
        // 작업 정보를 Redis 리스트에 저장
        stringRedisTemplate.opsForList().rightPush(taskListKey, eventId);
      }

      log.info("Created temporary task list {} with {} tasks", taskListKey, keys.size());

      // 3) 멀티스레딩으로 작업 처리 with backpressure monitoring
      ThreadPoolExecutor tpe = (ThreadPoolExecutor) executorService;

      // Log thread pool status for monitoring
      if (tpe.getQueue().size() > QUEUE_CAPACITY * 0.8) {
        log.warn("Thread pool queue is {}% full ({}/{}). Backpressure may be triggered.",
            (tpe.getQueue().size() * 100 / QUEUE_CAPACITY), tpe.getQueue().size(), QUEUE_CAPACITY);
      }

      int workerCount = Math.min(keys.size(), THREAD_POOL_SIZE);
      for (int i = 0; i < workerCount; i++) {
        try {
          executorService.submit(() -> processPromotionTasks(taskListKey));
        } catch (Exception e) {
          log.warn(
              "Failed to submit promotion task, likely due to backpressure. Caller thread will handle it.",
              e);
          // CallerRunsPolicy will handle this by running in the caller thread (scheduler thread)
        }
      }

      // Log final thread pool status
      log.debug("Thread pool status - Active: {}, Queue: {}, Completed: {}", tpe.getActiveCount(),
          tpe.getQueue().size(), tpe.getCompletedTaskCount());

      // 작업 목록 만료 시간 설정 (5분)
      stringRedisTemplate.expire(taskListKey, 5, TimeUnit.MINUTES);

    } catch (Exception e) {
      log.error("Error in promoteToEntryQueue: {}", e.getMessage(), e);
    }
  }

  /**
   * 작업 목록에서 작업을 가져와 처리하는 메서드 각 스레드가 이 메서드를 실행하여 작업을 처리함
   */
  private void processPromotionTasks(String taskListKey) {
    try {
      while (true) {
        // 작업 목록에서 작업 가져오기 (LPOP 사용)
        String eventId = stringRedisTemplate.opsForList().leftPop(taskListKey);
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

  /**
   * 주어진 이벤트 ID에 대해 승격 스크립트를 실행하는 메서드
   */
  private void executePromotionScript(String eventId) {
    try {
      if (!isEventOpen(eventId)) {
        return;
      }

      String entryCountHashKey = ENTRY_QUEUE_SLOTS_KEY_NAME; // ex: "ENTRY_QUEUE_SLOTS"
      String waitingRecordHash = "WAITING_QUEUE_INDEX_RECORD:" + eventId; // ex: "WAITING_QUEUE_INDEX_RECORD:42"
      String waitingZsetKey = WAITING_QUEUE_KEY_NAME + ":" + eventId; // ex: "waiting:42"
      String waitingInUserHash = WAITING_USER_IDS_KEY_NAME + ":" + eventId; // ex:
                                                                                         // "WAITING_USER_IDS:42"
      String entryStreamKey = ENTRY_QUEUE_KEY_NAME; // ex: "ENTRY_QUEUE"

      List<String> scriptKeys = List.of(entryCountHashKey, waitingRecordHash, waitingZsetKey,
          waitingInUserHash, entryStreamKey);

      // Lua 스크립트 실행
      Long cnt = redisTemplate.execute(promoteAllScript, scriptKeys, eventId);

      if (cnt != null && cnt > 0) {
        promotionCounter.addAndGet(cnt);
        log.debug("Promoted {} users for event {}", cnt, eventId);
      }
    } catch (Exception e) {
      log.error("Error executing promotion script for event {}: {}", eventId, e.getMessage(), e);
    }
  }

  private boolean isEventOpen(String eventId) {
    Object raw = stringRedisTemplate.opsForHash().get(EVENT_STATUSES_HASH_KEY, eventId);
    if (raw == null) {
      return false;
    }
    return "OPEN".equalsIgnoreCase(raw.toString().trim());
  }

  private List<String> scanWaitingQueueKeys() {
    String pattern = WAITING_QUEUE_KEY_NAME + ":*";
    int count = 1000;

    List<String> keys = redisTemplate.execute((RedisConnection connection) -> {
      List<String> results = new java.util.ArrayList<>();
      try (Cursor<byte[]> cursor =
          connection.scan(ScanOptions.scanOptions().match(pattern).count(count).build())) {
        while (cursor.hasNext()) {
          results.add(new String(cursor.next(), StandardCharsets.UTF_8));
        }
      }
      return results;
    });

    return keys == null ? List.of() : keys;
  }

  private void doPromote(String key) throws JsonProcessingException {

    HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();

    // 해당 키의 스트림 내의 모든 메시지 얻기
    List<Object> records = redisTemplate.opsForZSet().range(key, 0, -1).stream().map(item -> {
      try {
        return redisTemplate.opsForHash().get(
            "WAITING_QUEUE_INDEX_RECORD:" + key.split(":")[1].toString(),
            objectMapper.readTree(item.toString()).get("userId").toString());
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }).toList();

    for (Object record : records) {
      // 메시지 내 데이터 파싱
      Long userId =
          Long.parseLong(objectMapper.readTree(record.toString()).get("userId").toString());
      Long eventId =
          Long.parseLong(objectMapper.readTree(record.toString()).get("eventId").toString());
      String instanceId =
          String.valueOf(objectMapper.readTree(record.toString()).get("instanceId"));

      // 이 waiting 스트림 메시지에 해당하는 유저가 이벤트의 entry queue에 들어갈수 있는지 검사
      // 해당 event의 entry queue count를 조회
      Long queueCount =
          Long.parseLong(hashOps.get(ENTRY_QUEUE_SLOTS_KEY_NAME, eventId.toString()).toString());
      // 이 값이 1 이상이라면 들어갈 자리가 있다는 뜻이므로 유저를 entry queue로 넣음
      if (queueCount != null && queueCount > 0) {

        // 2) 해당 event의 entry queue count 1만큼 감소
        Long tmp = hashOps.increment(ENTRY_QUEUE_SLOTS_KEY_NAME, eventId.toString(), -1);

        // entry queue message를 생성
        redisTemplate.opsForStream()
            .add(StreamRecords
                .mapBacked(Map.of("userId", userId, "eventId", eventId, "instanceId", instanceId))
                .withStreamKey(ENTRY_QUEUE_KEY_NAME));

        // 4) 스트림에서 해당 레코드 삭제
        redisTemplate.opsForZSet().remove(key,
            objectMapper.writeValueAsString(Map.of("userId", userId)));
        redisTemplate.opsForHash().delete("WAITING_QUEUE_INDEX_RECORD:" + eventId.toString(),
            userId.toString());

        hashOps.delete(WAITING_USER_IDS_KEY_NAME + ":" + eventId.toString(),
            userId.toString());
      }
    }
  }
}

//
// Set<String> keys = redisTemplate.keys(RedisConfig.ENTRY_QUEUE_SLOTS_KEY_NAME + ":*");
//
// Long count = Long.parseLong(Objects.requireNonNull(redisTemplate.opsForValue()
// .get(RedisConfig.ENTRY_QUEUE_SLOTS_KEY_NAME)).toString());
// // 갯수만큼 waiting queue에서 가져옴
// List<MapRecord<String, Object, Object>> promoteTarget = redisTemplate.opsForStream()
// .read(Consumer.from(
// RedisConfig.WAITING_QUEUE_GROUP_NAME, RedisConfig.WAITING_QUEUE_CONSUMER_NAME),
// StreamReadOptions.empty().count(count), StreamOffset.create(
// RedisConfig.WAITING_QUEUE_KEY_NAME, ReadOffset.lastConsumed()
// ));
//
// assert promoteTarget != null;
// promoteTarget.forEach(
// record -> {
// // waiting queue message에서 userId, eventId, instanceId 추출
// Long userId = Long.parseLong(record.getValue().get("userId").toString());
// Long eventId = Long.parseLong(record.getValue().get("eventId").toString());
// String instanceId = String.valueOf(record.getValue().get("instanceId"));
//
// // entry queue message를 생성
// redisTemplate.opsForStream()
// .add(StreamRecords.mapBacked(
// Map.of("userId", userId, "eventId", eventId, "instanceId", instanceId)
// ).withStreamKey(RedisConfig.ENTRY_QUEUE_KEY_NAME));
//
// // waiting queue에서 메시지를 consume했음을 알림 (ack)
// redisTemplate.opsForStream()
// .acknowledge(RedisConfig.WAITING_QUEUE_KEY_NAME, RedisConfig.WAITING_QUEUE_GROUP_NAME,
// record.getId());
// // waiting queue에서 consume한 메시지를 삭제
// redisTemplate.opsForStream()
// .delete(RedisConfig.WAITING_QUEUE_KEY_NAME, record.getId());
//
// // WAITING_USER_IDS에서 삭제
// redisTemplate.opsForHash()
// .delete(RedisConfig.WAITING_USER_IDS_KEY_NAME + ":" + eventId, userId.toString());
// }
// );
