package org.codenbug.broker.thread;

import static org.codenbug.broker.redis.RedisConfig.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	private final SseEmitterService emitterService;
	private final ObjectMapper objectMapper;

	public QueueInfoScheduler(RedisTemplate<String, String> redisTemplate, SseEmitterService emitterService,
		ObjectMapper objectMapper) {
		this.redisTemplate = redisTemplate;
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
				log.error(e.getMessage());
			}
		});

	}

	private void doPrintInfo(Map<String, SseConnection> emitterMap, String key) throws JsonProcessingException {
		Set<String> waitingKeyList = redisTemplate.opsForZSet().range(key, 0, -1);
		List<Object> waitingList = new ArrayList<>();
		for (String waitingKey : waitingKeyList) {
			Object result = null;
			result = redisTemplate.opsForHash()
				.get("WAITING_QUEUE_RECORD:" + key.split(":")[1].toString(),
					objectMapper.readTree(waitingKey.toString()).get("userId").asText());

			if (result == null)
				continue;
			waitingList.add(result);
		}

		if (waitingList == null || waitingList.isEmpty()) {
			return;
		}
		// 대기열 큐에 있는 모든 유저들에게 대기열 순번과 userId, eventId를 전송합니다.
		for (Object record : waitingList) {
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
			} catch (IOException e) {
				emitter.completeWithError(e);
				log.debug("user %s가 연결이 끊어진 상태입니다.".formatted(userId));
				log.error("messageListener1:{}", e.getMessage());
				// throw new RuntimeException(e);
			} catch (IllegalStateException e) {
				log.error("messageListener2:{}", e.getMessage());
				// throw new RuntimeException(e);

			}
		}
	}

	@Scheduled(cron = "*/5 * * * * *")
	public void heartBeat() {
		Map<String, SseConnection> emitterMap = emitterService.getEmitterMap();
		for (SseConnection conn : emitterMap.values()) {
			SseEmitter emitter = conn.getEmitter();
			try {
				emitter.send(
					SseEmitter.event()
						.comment("heartBeat")
				);
			} catch (IOException e) {
				emitter.completeWithError(e);
				log.error("messageListener:{}", e.getMessage());

			} catch (IllegalStateException e) {
				log.error("messageListener:{}", e.getMessage());
			}
		}
	}
}

