package org.codenbug.broker.service;

import static org.codenbug.broker.redis.RedisConfig.*;

import java.io.IOException;
import java.util.Map;

import org.codenbug.broker.entity.SseConnection;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class WaitingQueueEntryService {

	private final SseEmitterService sseEmitterService;
	private final RedisTemplate<String, Object> simpleRedisTemplate;
	private final ObjectMapper objectMapper;
	private final RedisTemplate<Object, Object> redisTemplate;
	@Value("${custom.events.url}")
	private String url;

	@Value("${custom.instance-id}")
	private String instanceId;

	public WaitingQueueEntryService(SseEmitterService sseEmitterService,
		RedisTemplate<String, Object> simpleRedisTemplate, ObjectMapper objectMapper,
		RedisTemplate<Object, Object> redisTemplate) {
		this.sseEmitterService = sseEmitterService;
		this.simpleRedisTemplate = simpleRedisTemplate;
		this.objectMapper = objectMapper;
		this.redisTemplate = redisTemplate;
	}

	public SseEmitter entry(String eventId) throws JsonProcessingException {
		// 로그인한 유저 id 조회
		String id = getLoggedInUserId();

		// emitter 생성 및 저장
		SseEmitter emitter = sseEmitterService.add(id, eventId);

		// TODO: waiting thread에 유저를 추가하도록 전달
		enter(id, eventId);
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

		Map<String, SseConnection> emitterMap = sseEmitterService.getEmitterMap();
		emitterMap.forEach((id, emitterConnection) -> {
			try {
				emitterConnection.getEmitter().send(SseEmitter.event()
					.comment("heartBeat")
					.build());
			} catch (IOException e) {
				System.out.println("error");
				emitterConnection.getEmitter().complete();
			}
		});
		// 총 좌석수 얻기
		RestTemplate restTemplate = new RestTemplate();

		ResponseEntity<String> forEntity = restTemplate.getForEntity(
			url + "/api/v1/events/" + eventId, String.class);

		int seatCount = objectMapper.readTree(forEntity.getBody())
			.get("data")
			.get("information")
			.get("seatCount")
			.asInt();

		if (!simpleRedisTemplate.opsForHash().hasKey(ENTRY_QUEUE_COUNT_KEY_NAME, eventId.toString())) {
			simpleRedisTemplate.opsForHash()
				.put(ENTRY_QUEUE_COUNT_KEY_NAME, eventId.toString(), seatCount);
		}




		// 유저가 대기열에 있었는지 확인하기 위한 hash 값 조회
		Boolean isEntered = simpleRedisTemplate.opsForHash()
			.hasKey(WAITING_QUEUE_IN_USER_RECORD_KEY_NAME + ":" + eventId, userId.toString());

		if (isEntered) {
			return;
		}
		// 대기열 큐 idx 추가
		Long idx = simpleRedisTemplate.opsForHash()
			.increment(WAITING_QUEUE_IDX_KEY_NAME, eventId.toString(), 1);
		assert idx != null;

		// { idx , userId}
		// 로 key가 WAITING:<eventId>인 zset에 저장
		simpleRedisTemplate.opsForZSet()
			.add(WAITING_QUEUE_KEY_NAME + ":" + eventId,
				Map.of(QUEUE_MESSAGE_USER_ID_KEY_NAME, userId.toString()),
				idx);

		// 나머지 정보를 hash에 저장
		simpleRedisTemplate.opsForHash()
			.put("WAITING_QUEUE_RECORD:" + eventId, userId.toString(),
				Map.of(QUEUE_MESSAGE_USER_ID_KEY_NAME, userId.toString(),
					QUEUE_MESSAGE_IDX_KEY_NAME, idx.toString(),
					QUEUE_MESSAGE_EVENT_ID_KEY_NAME, eventId.toString(),
					QUEUE_MESSAGE_INSTANCE_ID_KEY_NAME, instanceId.toString()
			));
		// 유저가 대기열에 있는지 확인하기 위한 hash 값 업데이트
		simpleRedisTemplate.opsForHash()
			.put(WAITING_QUEUE_IN_USER_RECORD_KEY_NAME + ":" + eventId, userId.toString(), idx);

	}

	private String getLoggedInUserId() {
		return LoggedInUserContext.get().getUserId();
	}
}
