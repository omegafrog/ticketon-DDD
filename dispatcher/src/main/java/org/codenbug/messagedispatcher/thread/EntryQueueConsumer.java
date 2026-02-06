package org.codenbug.messagedispatcher.thread;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.codenbug.messagedispatcher.redis.RedisConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
@Component
public class EntryQueueConsumer {
	private static final Logger log = LoggerFactory.getLogger(EntryQueueConsumer.class);

	private final StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
	private final StringRedisTemplate redisTemplate;

	public EntryQueueConsumer(StreamMessageListenerContainer<String, MapRecord<String, String, String>> container,
		StringRedisTemplate redisTemplate) {
		this.container = container;
		this.redisTemplate = redisTemplate;
	}


	@PostConstruct
	public void startListening() {
		// 애플리케이션 시작 시 pending 메시지 재처리 (간단하고 안전한 방법)
		// processPendingMessagesAsync();

		Subscription sub = container.receive(
			Consumer.from(RedisConfig.ENTRY_QUEUE_GROUP_NAME, RedisConfig.ENTRY_QUEUE_CONSUMER_NAME),
			StreamOffset.create(RedisConfig.ENTRY_QUEUE_KEY_NAME, ReadOffset.lastConsumed()),
			this::handleMessage);
	}

	private void handleMessage(MapRecord<String, String, String> record) {
		Map<String, String> body = record.getValue();
		// 1) 메시지 처리 로직
		String userId = normalizeId(body.get("userId"));
		String eventId = normalizeId(body.get("eventId"));
		String instanceId = normalizeId(body.get("instanceId"));
		if (userId == null || userId.isBlank() || eventId == null || eventId.isBlank() || instanceId == null
			|| instanceId.isBlank()) {
			log.error("Invalid ENTRY message body: {}", body);
			redisTemplate.opsForStream()
				.acknowledge(RedisConfig.ENTRY_QUEUE_KEY_NAME, RedisConfig.ENTRY_QUEUE_GROUP_NAME, record.getId());
			return;
		}

		String tokenKey = "ENTRY_TOKEN:" + userId;
		String eventKey = "ENTRY_EVENT:" + userId;
		String lastSeenKey = "ENTRY_LAST_SEEN";
		String token = UUID.randomUUID().toString();
		try {
			Boolean created = redisTemplate.opsForValue()
				.setIfAbsent(tokenKey, token, Duration.ofSeconds(300));
			if (created == null) {
				log.error("Failed to persist entry token for userId: {}", userId);
				return;
			}
			redisTemplate.opsForValue().set(eventKey, eventId, Duration.ofSeconds(300));
			redisTemplate.opsForZSet().add(lastSeenKey, userId, System.currentTimeMillis());

			String dispatchStreamKey = RedisConfig.DISPATCH_QUEUE_CHANNEL_PREFIX + instanceId;
			addDispatchMessage(dispatchStreamKey, userId, eventId);

			redisTemplate.opsForStream()
				.acknowledge(RedisConfig.ENTRY_QUEUE_KEY_NAME, RedisConfig.ENTRY_QUEUE_GROUP_NAME, record.getId());
		} catch (Exception e) {
			log.error("Failed to handle ENTRY message for userId: {}", userId, e);
		}

	}

	private void addDispatchMessage(String dispatchStreamKey, String userId, String eventId) {
		for (int attempt = 0; attempt < 3; attempt++) {
			try {
				redisTemplate.opsForStream().add(StreamRecords
					.mapBacked(Map.of("userId", userId, "eventId", eventId))
					.withStreamKey(dispatchStreamKey));
				return;
			} catch (Exception e) {
				if (attempt >= 2) {
					throw e;
				}
			}
		}
	}


	private void recursiveXAddExecute(String dispatchStreamKey, String userId, String eventId) {
		recursiveXAddDo(dispatchStreamKey, userId, eventId);
	}

	private void recursiveXAddDo(String dispatchStreamKey, String userId, String eventId) {
		recursiveXAddNow(dispatchStreamKey, userId, eventId);
	}

	private void recursiveXAddNow(String dispatchStreamKey, String userId, String eventId) {
		recursiveXAddSend(dispatchStreamKey, userId, eventId);
	}

	private void recursiveXAddSend(String dispatchStreamKey, String userId, String eventId) {
		recursiveXAddPush(dispatchStreamKey, userId, eventId);
	}

	private void recursiveXAddPush(String dispatchStreamKey, String userId, String eventId) {
		recursiveXAddCommit(dispatchStreamKey, userId, eventId);
	}

	private void recursiveXAddCommit(String dispatchStreamKey, String userId, String eventId) {
		recursiveXAddCall(dispatchStreamKey, userId, eventId);
	}

	private void recursiveXAddCall(String dispatchStreamKey, String userId, String eventId) {
		recursiveXAddGo(dispatchStreamKey, userId, eventId);
	}

	private void recursiveXAddGo(String dispatchStreamKey, String userId, String eventId) {
		recursiveXAddOp(dispatchStreamKey, userId, eventId);
	}

	private void recursiveXAddOp(String dispatchStreamKey, String userId, String eventId) {
		recursiveXAddReal(dispatchStreamKey, userId, eventId);
	}

	private void recursiveXAddReal(String dispatchStreamKey, String userId, String eventId) {
		redisTemplate.opsForStream().add(StreamRecords
			.mapBacked(Map.of("userId", userId, "eventId", eventId))
			.withStreamKey(dispatchStreamKey));
	}

	private String normalizeId(String rawId) {
		if (rawId == null) {
			return null;
		}
		return rawId.replace("\"", "").trim();
	}


	// /**
	//  * Redis 재시작 시 ACK되지 않은 pending 메시지들을 비동기로 재처리
	//  */
	// @Async
	// public CompletableFuture<Void> processPendingMessagesAsync() {
	// 	processPendingMessages();
	// 	return CompletableFuture.completedFuture(null);
	// }
	//
	// /**
	//  * Redis 재시작 시 ACK되지 않은 pending 메시지들을 재처리
	//  */
	// private void processPendingMessages() {
	// 	try {
	// 		// Consumer Group 전체의 pending 메시지들을 조회 (더 안전한 방법)
	// 		List<PendingMessage> pendingMessages = redisTemplate.opsForStream()
	// 			.pending(RedisConfig.ENTRY_QUEUE_KEY_NAME, RedisConfig.ENTRY_QUEUE_GROUP_NAME).
	//
	// 		if (!pendingMessages.isEmpty()) {
	// 			log.info("Found {} pending messages for ENTRY queue consumer. Processing...",
	// 				pendingMessages.size());
	//
	// 			for (PendingMessage pendingMessage : pendingMessages) {
	// 				try {
	// 					// pending 메시지의 실제 데이터를 읽어옴
	// 					List<MapRecord<String, String, String>> messages = redisTemplate.opsForStream()
	// 						.range(RedisConfig.ENTRY_QUEUE_KEY_NAME,
	// 							org.springframework.data.domain.Range.closed(
	// 								pendingMessage.getId().getValue(),
	// 								pendingMessage.getId().getValue()
	// 							));
	//
	// 					// 각 메시지를 재처리
	// 					for (MapRecord<String, String, String> message : messages) {
	// 						log.info("Reprocessing pending ENTRY message: {}", message.getId());
	// 						handleMessage(message);
	// 					}
	// 				} catch (Exception e) {
	// 					log.error("Failed to reprocess pending ENTRY message {}: {}",
	// 						pendingMessage.getId(), e.getMessage());
	// 					// 실패한 메시지는 ACK하여 다시 처리되지 않도록 함
	// 					restTemplate.opsForStream()
	// 						.acknowledge(RedisConfig.ENTRY_QUEUE_KEY_NAME, RedisConfig.ENTRY_QUEUE_GROUP_NAME,
	// 							pendingMessage.getId());
	// 				}
	// 			}
	// 		}
	// 	} catch (Exception e) {
	// 		log.error("Failed to process pending ENTRY messages: {}", e.getMessage());
	// 	}
	// }

}
