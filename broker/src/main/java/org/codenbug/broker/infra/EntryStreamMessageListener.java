package org.codenbug.broker.infra;

import static org.codenbug.broker.infra.RedisConfig.*;
import static org.codenbug.broker.service.SseEmitterService.*;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.codenbug.broker.app.EntryAuthService;
import org.codenbug.broker.config.InstanceConfig;
import org.codenbug.broker.domain.SseConnection;
import org.codenbug.broker.domain.Status;
import org.codenbug.broker.service.SseEmitterService;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EntryStreamMessageListener implements StreamListener<String, MapRecord<String, String, String>> {

	private final RedisTemplate<String, Object> redisTemplate;
	private final RedisConnectionFactory redisConnectionFactory;
	private final SseEmitterService sseEmitterService;
	private final EntryAuthService entryAuthService;
	private final RedisConfig redisConfig;
	private final InstanceConfig instanceConfig;


	private StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer;

	public EntryStreamMessageListener(RedisTemplate<String, Object> redisTemplate,
		RedisConnectionFactory redisConnectionFactory, SseEmitterService sseEmitterService,
		EntryAuthService entryAuthService, RedisConfig redisConfig, InstanceConfig instanceConfig) {
		this.redisTemplate = redisTemplate;
		this.redisConnectionFactory = redisConnectionFactory;
		this.sseEmitterService = sseEmitterService;
		this.entryAuthService = entryAuthService;
		this.redisConfig = redisConfig;
		this.instanceConfig = instanceConfig;
	}

	@PostConstruct
	public void startListening() {
		String instanceStreamName = redisConfig.getInstanceDispatchStreamName();
		String groupName = instanceStreamName + ":GROUP";
		String consumerName = instanceConfig.getInstanceId() + "-consumer"; // 각 인스턴스마다 고유한 컨슈머 이름

		log.info("Starting to listen on instance-specific stream: {}", instanceStreamName);

		// 인스턴스별 전용 스트림의 컨슈머 그룹 생성
		try {
			// 스트림이 존재하지 않으면 BUSYGROUP 에러가 발생할 수 있으므로 확인
			if (redisTemplate.opsForStream().groups(instanceStreamName).stream()
				.noneMatch(xInfoGroup -> xInfoGroup.groupName().equals(groupName))) {
				redisTemplate.opsForStream().createGroup(instanceStreamName, groupName);
			}
		} catch (RedisSystemException e) {
			// 스트림이 존재하지 않는 경우 먼저 더미 메시지를 추가하여 스트림 생성
			redisTemplate.opsForStream().add(instanceStreamName, Map.of("init", "true"));
			redisTemplate.opsForStream().createGroup(instanceStreamName, groupName);
		}

		StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
			StreamMessageListenerContainer.StreamMessageListenerContainerOptions
				.builder()
				.pollTimeout(Duration.ofSeconds(1)) // 폴링 타임아웃 설정
				.batchSize(RedisConfig.ENTRY_QUEUE_CAPACITY) // 한 번에 가져올 메시지 수
				.build();

		streamMessageListenerContainer = StreamMessageListenerContainer.create(redisConnectionFactory, options);

		// ReadOffset.lastConsumed()는 현재 컨슈머 그룹에서 마지막으로 처리(ack)한 메시지 다음부터 읽음
		streamMessageListenerContainer.receive(
			Consumer.from(groupName, consumerName),
			StreamOffset.create(instanceStreamName, ReadOffset.lastConsumed()),
			this // 리스너로 현재 클래스 인스턴스 지정
		);

		streamMessageListenerContainer.start();
		log.info("Started listening to Redis Stream '{}' with consumer group '{}' and consumer name '{}'",
			instanceStreamName, groupName, consumerName);

	}

	@Override
	public void onMessage(MapRecord<String, String, String> message) {

		String instanceStreamName = redisConfig.getInstanceDispatchStreamName();
		String groupName = instanceStreamName + ":GROUP";
		String consumerName = instanceConfig.getInstanceId() + "-consumer";

		Map<String, String> body = message.getValue();

		String userId = body.get("userId").replaceAll("\"", "");
		String eventId = body.get("eventId").replaceAll("\"", "");
		SseConnection sseConnection = sseEmitterService.getEmitterMap().get(userId);
		// dispatcher가 유저 승급했는데, 유저는 정작 연결 끊은 상태
		if (sseConnection == null) {
			log.info("count incremented");
			redisTemplate.opsForHash()
				.increment(ENTRY_QUEUE_COUNT_KEY_NAME, eventId, 1);
			redisTemplate.opsForHash()
				.delete(ENTRY_TOKEN_STORAGE_KEY_NAME, userId.toString());
		}
		if (!sseConnection.getEventId().equals(eventId)) {
			return;
		}

		sseConnection.setStatus(Status.IN_PROGRESS);
		SseEmitter emitter = sseConnection.getEmitter();

		String token = entryAuthService.generateEntryAuthToken(Map.of("eventId", eventId, "userId", userId),
			"entryAuthToken");
		redisTemplate.opsForHash()
			.put(RedisConfig.ENTRY_TOKEN_STORAGE_KEY_NAME, userId.toString(), token);
		redisTemplate.expire(RedisConfig.ENTRY_TOKEN_STORAGE_KEY_NAME + ":" + userId, 5, TimeUnit.MINUTES);

		try {
			emitter.send(
				SseEmitter.event()
					.data(Map.of(
						"eventId", eventId,
						"userId", userId,
						"status", sseConnection.getStatus(),
						"token", token
					))
			);
		} catch (IOException e) {
			closeConn(userId, eventId, redisTemplate);
		} catch (
			Exception e) {
			closeConn(userId, eventId, redisTemplate);
		}
		redisTemplate.opsForStream().acknowledge(instanceStreamName, groupName, message.getId());
	}
}
