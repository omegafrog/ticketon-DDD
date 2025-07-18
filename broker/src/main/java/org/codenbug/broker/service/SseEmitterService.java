package org.codenbug.broker.service;

import static org.codenbug.broker.redis.RedisConfig.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.codenbug.broker.entity.SseConnection;
import org.codenbug.broker.entity.Status;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SseEmitterService {

	private static final Map<String, SseConnection> emitterMap = new ConcurrentHashMap<>();
	private final ObjectMapper objectMapper;

	public Map<String, SseConnection> getEmitterMap() {
		return emitterMap;
	}

	private final RedisTemplate<String, Object> redisTemplate;

	public SseEmitterService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
	}

	public SseEmitter add(String userId, String eventId) {
		if (emitterMap.containsKey(userId)) {
			throw new RuntimeException("다른 대기열에 이미 들어와 있습니다.");
		}
		// 새로운 emitter 생성
		SseEmitter emitter = new SseEmitter(0L);
		emitterMap.putIfAbsent(userId, new SseConnection(emitter, Status.IN_ENTRY, eventId));

		// emitter연결이 끊어질 때 만약 entry상태라면 entry count를 1 증가
		emitter.onCompletion(() -> {
			// log.info("emitter completed");
			closeConn(userId, eventId);
		});
		emitter.onError((e) -> {
			// log.info("emitter error");
			closeConn(userId, eventId);
		});
		emitter.onTimeout(() -> {
			// log.info("emitter timeout");
			closeConn(userId, eventId);
		});

		try {
			// 초기 메시지 전달
			emitter.send(
				SseEmitter.event()
					.data("sse 연결 성공. userId:" + userId));
		} catch (IOException e) {
			emitter.completeWithError(e);
			// log.error("messageListener:{}", e.getMessage());

			throw new RuntimeException(e);
		} catch (IllegalStateException e) {
			// log.error("messageListener:{}", e.getMessage());
			throw new RuntimeException(e);
		}

		// 전역 공간에 emitter 저장

		return emitter;
	}

	private void closeConn(String userId, String eventId) {
		// 커넥션 정보 얻기

		SseConnection sseConnection = emitterMap.remove(userId);
		if(sseConnection == null){
			log.warn("Attempted to close connection for user '{}', but it was already closed.", userId);
			return;
		}
		Status status = sseConnection.getStatus();

		// 커넥션 정보로부터 이벤트 아이디 얻기
		String parsedEventId = sseConnection.getEventId().toString();

		// 대기열 탈출 상태에서 커넥션이 종료되었다면
		// entry_queue_count를 1 감소시킨 것을 다시 증가
		if (status.equals(Status.IN_PROGRESS)) {
			redisTemplate.opsForHash()
				.increment(ENTRY_QUEUE_COUNT_KEY_NAME, parsedEventId, 1);
			// redisTemplate.opsForHash()
			// 	.delete(ENTRY_TOKEN_STORAGE_KEY_NAME, userId.toString());
		} else if (status.equals(Status.IN_ENTRY)) {

			redisTemplate.opsForZSet()
				.remove(WAITING_QUEUE_KEY_NAME + ":" + eventId,
					Map.of(QUEUE_MESSAGE_USER_ID_KEY_NAME, userId));
			redisTemplate.opsForHash()
				.delete("WAITING_QUEUE_RECORD:" + eventId.toString(), userId.toString());
			redisTemplate.opsForHash()
				.delete(WAITING_QUEUE_IN_USER_RECORD_KEY_NAME + ":" + parsedEventId,
					userId.toString());
		}
	}
}
