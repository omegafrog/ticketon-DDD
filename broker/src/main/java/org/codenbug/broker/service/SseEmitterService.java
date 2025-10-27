package org.codenbug.broker.service;


import static org.codenbug.broker.infra.RedisConfig.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.codenbug.broker.domain.SseConnection;
import org.codenbug.broker.domain.Status;
import org.codenbug.redislock.lock.RedisLock;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SseEmitterService {

  private static final Map<String, SseConnection> emitterMap = new ConcurrentHashMap<>();

  public Map<String, SseConnection> getEmitterMap() {
    return emitterMap;
  }

  private final RedisTemplate<String, Object> redisTemplate;

  public SseEmitterService(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @RedisLock(key = "#userId" + ":" + "#eventId")
  public SseEmitter add(String userId, String eventId) {

    // 새로운 emitter 생성
    SseEmitter emitter = new SseEmitter(0L);
    // emitter연결이 끊어질 때 만약 entry상태라면 entry count를 1 증가
    emitter.onCompletion(() -> {
      closeConn(userId, eventId, redisTemplate);
    });
    emitter.onError((e) -> {
      closeConn(userId, eventId, redisTemplate);

    });
    emitter.onTimeout(() -> {
      closeConn(userId, eventId, redisTemplate);

    });

    // 초기 메시지 전달
    try {
      emitter.send(SseEmitter.event().data("sse 연결 성공. userId:" + userId));
    } catch (Exception e) {
      closeConn(userId, eventId, redisTemplate);

    }

    // 전역 공간에 emitter 저장
    emitterMap.put(userId, new SseConnection(userId, emitter, Status.IN_ENTRY, eventId));

    return emitter;
  }

  public static void closeConn(String userId, String eventId,
      RedisTemplate<String, Object> redisTemplate) {
    // 커넥션 정보 얻기

    SseConnection sseConnection = emitterMap.remove(userId);
    if (sseConnection == null) {
      log.warn("Attempted to close connection for user '{}', but it was already closed.", userId);
      return;
    }
    Status status = sseConnection.getStatus();

    // 커넥션 정보로부터 이벤트 아이디 얻기
    String parsedEventId = sseConnection.getEventId().toString();

    log.info("status:{}", status);
    // 대기열 탈출 상태에서 커넥션이 종료되었다면
    // entry_queue_count를 1 감소시킨 것을 다시 증가
    if (status.equals(Status.IN_PROGRESS)) {
      log.info("count incremented");
      redisTemplate.opsForHash().increment(ENTRY_QUEUE_COUNT_KEY_NAME, parsedEventId, 1);
      redisTemplate.opsForHash().delete(ENTRY_TOKEN_STORAGE_KEY_NAME, userId.toString());
    } else if (status.equals(Status.IN_ENTRY)) {

      redisTemplate.opsForZSet().remove(WAITING_QUEUE_KEY_NAME + ":" + eventId,
          Map.of(QUEUE_MESSAGE_USER_ID_KEY_NAME, userId));
      redisTemplate.opsForHash().delete("WAITING_QUEUE_RECORD:" + eventId.toString(),
          userId.toString());
      redisTemplate.opsForHash().delete(WAITING_QUEUE_IN_USER_RECORD_KEY_NAME + ":" + parsedEventId,
          userId.toString());
    }
  }

  /**
   * 사용자의 SSE 연결을 명시적으로 해제합니다. 클라이언트에서 IN_PROGRESS 상태 후 즉시 호출하여 다음 사용자 승급을 촉진합니다.
   *
   * @param userId 사용자 ID
   * @param eventId 이벤트 ID
   */
  public void closeConnection(String userId, String eventId) {
    closeConn(userId, eventId, redisTemplate);
  }
}
