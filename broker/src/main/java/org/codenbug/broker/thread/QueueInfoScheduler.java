package org.codenbug.broker.thread;

import static org.codenbug.broker.redis.RedisConfig.*;
import static org.codenbug.broker.service.SseEmitterService.*;

import java.util.List;
import java.util.Map;

import org.codenbug.broker.entity.SseConnection;
import org.codenbug.broker.service.SseEmitterService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class QueueInfoScheduler {

	private final RedisTemplate<String, String> redisTemplate;
	private final RedisTemplate<String, Object> objectRedisTemplate;
	private final SseEmitterService emitterService;
	private final ObjectMapper objectMapper;

	public QueueInfoScheduler(RedisTemplate<String, String> redisTemplate,
		RedisTemplate<String, Object> objectRedisTemplate, SseEmitterService emitterService,
		ObjectMapper objectMapper) {
		this.redisTemplate = redisTemplate;
		this.objectRedisTemplate = objectRedisTemplate;
		this.emitterService = emitterService;
		this.objectMapper = objectMapper;
	}

	/**
	 * 대기열 순번 정보를 유저에게 전송하는 스레드 스케줄링 메서드
	 * 1초마다 수행됩니다.
	 */
	@Scheduled(cron = "* * * * * *")
	public void run() {

		// emitter map 가져옵니다
		Map<String, SseConnection> emitterMap = emitterService.getEmitterMap();

		// redis waiting queue의 모든 요소를 가져옵니다
		redisTemplate.keys(WAITING_QUEUE_KEY_NAME + ":*").forEach(key -> {
			try {
				doPrintInfo(emitterMap, key);
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		});

	}

	private void doPrintInfo(Map<String, SseConnection> emitterMap, String key) throws JsonProcessingException {
		// eventId에 해당하는 모든 stream 가져오기
		List<Object> waitingList = redisTemplate.opsForZSet()
			.range(key, 0, -1).stream().map(
				item -> {
					try {
						return redisTemplate.opsForHash()
							.get("WAITING_QUEUE_RECORD:" + key.split(":")[1].toString(),
								objectMapper.readTree(item.toString()).get("userId").asText());
					} catch (JsonProcessingException e) {
						throw new RuntimeException(e);
					}
				}
			).toList();

		if (waitingList == null || waitingList.isEmpty()) {
			return;
		}
		// 대기열 큐에 있는 모든 유저들에게 대기열 순번과 userId, eventId를 전송합니다.
		for (Object record : waitingList) {
			if(record==null)
				return;
			// 대기열 큐 메시지로부터 데이터를 파싱합니다.
			String userId =
				objectMapper.readTree(record.toString())
					.get(QUEUE_MESSAGE_USER_ID_KEY_NAME)
					.toString()
					.replaceAll("\"", "");
			String eventId =
				objectMapper.readTree(record.toString())
					.get(QUEUE_MESSAGE_EVENT_ID_KEY_NAME)
					.toString()
					.replaceAll("\"", "");
			Long idx = Long.parseLong(
				objectMapper.readTree(record.toString())
					.get(QUEUE_MESSAGE_IDX_KEY_NAME)
					.toString()
					.replaceAll("\"", ""));

			if (!emitterMap.containsKey(userId)) {
				log.debug("user %s가 연결이 끊어진 상태입니다.".formatted(userId));
				continue;
			}

			// 파싱한 userId로 sse 연결 객체를 가져옵니다.
			SseConnection sseConnection = emitterMap.get(userId);
			SseEmitter emitter = sseConnection.getEmitter();
			// 대기열 순번을 계산하고 sse 메시지를 전송합니다.
			try {
				emitter.send(
					SseEmitter.event()
						.data(Map.of("status", sseConnection.getStatus(), QUEUE_MESSAGE_USER_ID_KEY_NAME, userId,
							QUEUE_MESSAGE_EVENT_ID_KEY_NAME, eventId, "order",
							redisTemplate.opsForZSet()
								.rank(WAITING_QUEUE_KEY_NAME + ":" + eventId,
									"{\"userId\":\"%s\"}".formatted(userId))
								+ 1))
				);
			} catch (Exception e) {
				closeConn(userId, eventId, objectRedisTemplate);
				log.debug("user %s가 연결이 끊어진 상태입니다.".formatted(userId));
			}
		}
	}

	@Scheduled(cron = "*/3 * * * * *")
	public void heartBeat() {
		Map<String, SseConnection> emitterMap = emitterService.getEmitterMap();
		for (SseConnection conn : emitterMap.values()) {
			SseEmitter emitter = conn.getEmitter();
			try {
				emitter.send(
					SseEmitter.event()
						.comment("heartBeat")
				);
			} catch (Exception e) {
				closeConn(conn.getUserId(), conn.getEventId(), objectRedisTemplate);
			}
		}
	}

	// /**
	//  * 좀비 커넥션 정리 스케줄러
	//  * 30초마다 실행되어 Redis와 동기화되지 않은 커넥션을 정리합니다.
	//  */
	// @Scheduled(cron = "0,30 * * * * *")
	// public void cleanupZombieConnections() {
	// 	Map<String, SseConnection> emitterMap = emitterService.getEmitterMap();
	// 	AtomicInteger cleanedCount = new AtomicInteger(0);
	//
	// 	log.debug("Starting zombie connection cleanup. Current connections: {}", emitterMap.size());
	//
	// 	emitterMap.entrySet().removeIf(entry -> {
	// 		String userId = entry.getKey();
	// 		SseConnection connection = entry.getValue();
	// 		String eventId = connection.getEventId();
	//
	// 		// Redis에서 해당 사용자가 실제로 대기열에 있는지 확인
	// 		boolean existsInRedis = objectRedisTemplate.opsForHash()
	// 			.hasKey(WAITING_QUEUE_IN_USER_RECORD_KEY_NAME + ":" + eventId, userId);
	//
	// 		if (!existsInRedis) {
	// 			log.warn("Found zombie connection for user {} in event {}, cleaning up", userId, eventId);
	// 			try {
	// 				connection.getEmitter().complete();
	// 			} catch (Exception e) {
	// 				log.debug("Error completing emitter for zombie connection: {}", e.getMessage());
	// 			}
	// 			cleanedCount.incrementAndGet();
	// 			return true; // 맵에서 제거
	// 		}
	// 		return false; // 유지
	// 	});
	//
	// 	if (cleanedCount.get() > 0) {
	// 		log.info("Cleaned up {} zombie connections. Remaining connections: {}",
	// 			cleanedCount.get(), emitterMap.size());
	// 	}
	// }
}

