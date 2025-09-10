package org.codenbug.broker.app;

import static org.codenbug.broker.infra.RedisConfig.*;

import java.util.Map;

import org.codenbug.broker.config.InstanceConfig;
import org.codenbug.broker.domain.SseConnection;
import org.codenbug.broker.service.SseEmitterService;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WaitingQueueEntryService {

	private final SseEmitterService sseEmitterService;
	private final RedisTemplate<String, Object> simpleRedisTemplate;
	private final ObjectMapper objectMapper;
	private final InstanceConfig instanceConfig;

	@Value("${custom.events.url}")
	private String url;

	public WaitingQueueEntryService(SseEmitterService sseEmitterService,
		RedisTemplate<String, Object> simpleRedisTemplate, ObjectMapper objectMapper,
		InstanceConfig instanceConfig) {
		this.sseEmitterService = sseEmitterService;
		this.simpleRedisTemplate = simpleRedisTemplate;
		this.objectMapper = objectMapper;
		this.instanceConfig = instanceConfig;
	}

	public SseEmitter entry(String eventId) throws JsonProcessingException {
		// 로그인한 유저 id 조회
		String id = getLoggedInUserId();
		SseEmitter emitter;
		// emitter 생성 및 저장
		try {
			emitter = sseEmitterService.add(id, eventId);
		}catch (IllegalStateException e){
			log.error("다른 대기열에 이미 들어와 있습니다.");
			throw e;
		}

		enter(id, eventId);
		assert emitter !=null;
		return emitter;
	}

	/**
	 * 사용자를 대기열에 추가한다. redis의 메시지 idx를 가져와 메시지의 idx로 추가하고 1을 증가시킨다.
	 * 이후 WAITING stream에 메시지를 추가해 사용자를 대기열에 추가한다.
	 *
	 * @param userId 대기열에 추가할 유저 id
	 * @param eventId 행사의 id
	 */
	private void enter(String userId, String eventId) throws JsonProcessingException {

		if (!WaitingQueueCountExist(eventId)) {
			// 총 좌석수 얻기
			updateWaitingQueueCount(eventId);
		}

		// 유저가 대기열에 있었는지 확인하기 위한 hash 값 조회
		// => redis lock으로 대체. emitterService.add 호출시 락 검사
		// Boolean isEntered = simpleRedisTemplate.opsForHash()
		// 	.hasKey(WAITING_QUEUE_IN_USER_RECORD_KEY_NAME + ":" + eventId, userId.toString());
		//
		// if (isEntered) {
		// 	return;
		// }

		// 대기열 큐 idx 증가
		Long idx = incrementWaitingQueueIdx(eventId);

		// { idx , userId}
		// 로 key가 WAITING:<eventId>인 zset에 저장
		saveUserToWaitingQueue(userId, eventId, idx);

		// 나머지 정보를 hash에 저장
		saveAdditionalUserDataToHash(userId, eventId, idx);

	}

	private void saveAdditionalUserDataToHash(String userId, String eventId, Long idx) {
		simpleRedisTemplate.opsForHash()
			.put("WAITING_QUEUE_RECORD:" + eventId, userId.toString(),
				Map.of(QUEUE_MESSAGE_USER_ID_KEY_NAME, userId.toString(),
					QUEUE_MESSAGE_IDX_KEY_NAME, idx.toString(),
					QUEUE_MESSAGE_EVENT_ID_KEY_NAME, eventId.toString(),
					QUEUE_MESSAGE_INSTANCE_ID_KEY_NAME, instanceConfig.getInstanceId()
				));
	}

	private void saveUserToWaitingQueue(String userId, String eventId, Long idx) {
		simpleRedisTemplate.opsForZSet()
			.add(WAITING_QUEUE_KEY_NAME + ":" + eventId,
				Map.of(QUEUE_MESSAGE_USER_ID_KEY_NAME, userId.toString()),
				idx);
	}

	private Long incrementWaitingQueueIdx(String eventId) {
		Long idx = simpleRedisTemplate.opsForHash()
			.increment(WAITING_QUEUE_IDX_KEY_NAME, eventId.toString(), 1);
		return idx;
	}

	private void updateWaitingQueueCount(String eventId) throws JsonProcessingException {
		RestTemplate restTemplate = new RestTemplate();

		ResponseEntity<String> forEntity = restTemplate.getForEntity(
			url + "/api/v1/events/" + eventId, String.class);

		int seatCount = objectMapper.readTree(forEntity.getBody())
			.get("data")
			.get("seatCount")
			.asInt();
		simpleRedisTemplate.opsForHash()
			.put(ENTRY_QUEUE_COUNT_KEY_NAME, eventId.toString(), seatCount);
	}

	private Boolean WaitingQueueCountExist(String eventId) {
		return simpleRedisTemplate.opsForHash().hasKey(ENTRY_QUEUE_COUNT_KEY_NAME, eventId.toString());
	}

	/**
	 * 현재 로그인한 사용자의 대기열 연결을 명시적으로 해제합니다.
	 * IN_PROGRESS 상태에 도달한 후 즉시 호출하여 다음 사용자가 빠르게 승급할 수 있도록 돕니다.
	 *
	 * @param eventId 행사의 id
	 * @return 성공 시 200 OK 응답
	 */
	public ResponseEntity<Void> disconnect(String eventId) {
		String userId = getLoggedInUserId();

		// SSE 연결 해제 및 리소스 정리
		sseEmitterService.closeConn(userId, eventId, simpleRedisTemplate);

		return ResponseEntity.ok().build();
	}

	private String getLoggedInUserId() {
		return LoggedInUserContext.get().getUserId();
	}
}
