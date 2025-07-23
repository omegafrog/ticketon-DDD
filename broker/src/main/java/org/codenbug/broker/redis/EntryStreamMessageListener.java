package org.codenbug.broker.redis;

import static org.codenbug.broker.redis.RedisConfig.*;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.codenbug.broker.entity.SseConnection;
import org.codenbug.broker.entity.Status;
import org.codenbug.broker.service.EntryAuthService;
import org.codenbug.broker.service.SseEmitterService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EntryStreamMessageListener implements StreamListener<String, MapRecord<String, String, String>> {

	private final RedisTemplate<String, Object> redisTemplate;
	private final RedisConnectionFactory redisConnectionFactory;
	private final SseEmitterService sseEmitterService;
	private final EntryAuthService entryAuthService;
	private final RedisConfig redisConfig;

	@Value("${custom.instance-id}")
	private String instanceId;

	private StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer;

	public EntryStreamMessageListener(RedisTemplate<String, Object> redisTemplate,
		RedisConnectionFactory redisConnectionFactory, SseEmitterService sseEmitterService,
		EntryAuthService entryAuthService, RedisConfig redisConfig) {
		this.redisTemplate = redisTemplate;
		this.redisConnectionFactory = redisConnectionFactory;
		this.sseEmitterService = sseEmitterService;
		this.entryAuthService = entryAuthService;
		this.redisConfig = redisConfig;
	}

	@PostConstruct
	public void startListening() {
		String instanceStreamName = redisConfig.getInstanceDispatchStreamName();
		String groupName = instanceStreamName + ":GROUP";
		String consumerName = instanceId + "-consumer"; // 각 인스턴스마다 고유한 컨슈머 이름

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

		// '>'는 아직 처리되지 않은 새로운 메시지만 읽겠다는 의미
		// ReadOffset.lastConsumed()는 현재 컨슈머 그룹에서 마지막으로 처리(ack)한 메시지 다음부터 읽음
		streamMessageListenerContainer.receive(
			Consumer.from(groupName, consumerName),
			StreamOffset.create(instanceStreamName, ReadOffset.lastConsumed()),
			this // 리스너로 현재 클래스 인스턴스 지정
		);

		streamMessageListenerContainer.start();
		log.info("Started listening to Redis Stream '{}' with consumer group '{}' and consumer name '{}'",
			instanceStreamName, groupName, consumerName);

		// 애플리케이션 시작 시 pending 메시지 재처리 (간단하고 안전한 방법)
		processPendingMessagesAsync(instanceStreamName, groupName, consumerName);
	}

	@PreDestroy
	public void stopListening() {
		if (streamMessageListenerContainer != null) {
			streamMessageListenerContainer.stop();
			log.info("Stopped listening to Redis Stream.");
		}
	}


	/**
	 * Redis 재시작 시 ACK되지 않은 pending 메시지들을 비동기로 재처리
	 */
	@Async
	public CompletableFuture<Void> processPendingMessagesAsync(String streamName, String groupName, String consumerName) {
		processPendingMessages(streamName, groupName, consumerName);
		return CompletableFuture.completedFuture(null);
	}

	/**
	 * Redis 재시작 시 ACK되지 않은 pending 메시지들을 재처리
	 */
	private void processPendingMessages(String streamName, String groupName, String consumerName) {
		try {
			// Consumer Group 전체의 pending 메시지들을 조회 (더 안전한 방법)
			List<PendingMessage> pendingMessages = redisTemplate.opsForStream()
				.pending(streamName, groupName)
				.getMessages();

			if (!pendingMessages.isEmpty()) {
				log.info("Found {} pending messages for consumer '{}'. Processing...", 
					pendingMessages.size(), consumerName);

				for (PendingMessage pendingMessage : pendingMessages) {
					try {
						// 오래된 pending 메시지(30초 이상)는 현재 consumer가 처리하도록 재할당
						long pendingTimeMs = System.currentTimeMillis() - pendingMessage.getTotalDeliveryCount();
						if (pendingTimeMs > 30000) { // 30초 이상 pending된 메시지
							log.info("Claiming old pending message {} from consumer {}", 
								pendingMessage.getId(), pendingMessage.getConsumerName());
							
							// XCLAIM으로 메시지를 현재 consumer에게 재할당
							List<MapRecord<String, String, String>> claimedMessages = redisTemplate.opsForStream()
								.claim(streamName, groupName, consumerName, 
									java.time.Duration.ofSeconds(30), pendingMessage.getId());
							
							// 재할당된 메시지 처리
							for (MapRecord<String, String, String> message : claimedMessages) {
								log.info("Processing claimed message: {}", message.getId());
								onMessage(message);
							}
						} else {
							// 최근 pending 메시지는 원래 consumer가 처리하도록 대기
							log.debug("Skipping recent pending message {} (consumer: {})", 
								pendingMessage.getId(), pendingMessage.getConsumerName());
						}
					} catch (Exception e) {
						log.error("Failed to process pending message {}: {}", 
							pendingMessage.getId(), e.getMessage());
						// 실패한 메시지는 ACK하여 다시 처리되지 않도록 함
						redisTemplate.opsForStream()
							.acknowledge(streamName, groupName, pendingMessage.getId());
					}
				}
			}
		} catch (Exception e) {
			log.error("Failed to process pending messages: {}", e.getMessage());
		}
	}

	@Override
	public void onMessage(MapRecord<String, String, String> message) {

		String instanceStreamName = redisConfig.getInstanceDispatchStreamName();
		String groupName = instanceStreamName + ":GROUP";
		String consumerName = instanceId + "-consumer";

		Map<String, String> body = message.getValue();

		String userId = body.get("userId").replaceAll("\"", "");
		String eventId = body.get("eventId").replaceAll("\"", "");
		SseConnection sseConnection = sseEmitterService.getEmitterMap().get(userId);

		if (sseConnection == null || !sseConnection.getEventId().equals(eventId)) {
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
			SseEmitterService.closeConn(userId, eventId, redisTemplate);
			emitter.completeWithError(e);
			log.error("messageListener:{}", e.getMessage());
		}
		catch (IllegalStateException e){
			SseEmitterService.closeConn(userId, eventId, redisTemplate);
			log.error("messageListener:{}", e.getMessage());
		}
		redisTemplate.opsForStream()
			.acknowledge(instanceStreamName, groupName, message.getId());
	}
}
