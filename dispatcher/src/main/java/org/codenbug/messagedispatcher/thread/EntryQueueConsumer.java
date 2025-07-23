package org.codenbug.messagedispatcher.thread;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.codenbug.messagedispatcher.redis.RedisConfig;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EntryQueueConsumer {

	private final StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
	private final RedisTemplate<String, Object> redisTemplate;

	public EntryQueueConsumer(StreamMessageListenerContainer<String, MapRecord<String, String, String>> container,
		RedisTemplate<String, Object> redisTemplate) {
		this.container = container;
		this.redisTemplate = redisTemplate;
	}


	@PostConstruct
	public void startListening() {
		// 애플리케이션 시작 시 pending 메시지 재처리 (간단하고 안전한 방법)
		processPendingMessagesAsync();
		
		Subscription sub = container.receive(
			Consumer.from(RedisConfig.ENTRY_QUEUE_GROUP_NAME, RedisConfig.ENTRY_QUEUE_CONSUMER_NAME),
			StreamOffset.create(RedisConfig.ENTRY_QUEUE_KEY_NAME, ReadOffset.lastConsumed()),
			this::handleMessage);
	}

	private void handleMessage(MapRecord<String, String, String> record) {
		Map<String, String> body = record.getValue();
		// 1) 메시지 처리 로직
		String userId = body.get("userId");
		String eventId = body.get("eventId");
		String instanceIdRaw = body.get("instanceId");
		
		// instanceId에서 따옴표 제거 (JSON에서 파싱될 때 생기는 따옴표)
		String instanceId = instanceIdRaw;
		if (instanceIdRaw.startsWith("\"") && instanceIdRaw.endsWith("\"")) {
			instanceId = instanceIdRaw.substring(1, instanceIdRaw.length() - 1);
		}

		// instanceId에 따라 해당 Broker의 전용 DISPATCH 스트림으로 라우팅
		String targetDispatchStream = RedisConfig.DISPATCH_QUEUE_CHANNEL_PREFIX + instanceId;
		
		log.info("Routing message to instance-specific stream: {} for user: {}", targetDispatchStream, userId);

		redisTemplate.opsForStream()
			.add(targetDispatchStream, Map.of(
				"userId", userId,
				"eventId", eventId,
				"instanceId", instanceId)
			);
		// 2) ACK
		redisTemplate.opsForStream()
			.acknowledge(RedisConfig.ENTRY_QUEUE_KEY_NAME, RedisConfig.ENTRY_QUEUE_GROUP_NAME, record.getId());

	}


	/**
	 * Redis 재시작 시 ACK되지 않은 pending 메시지들을 비동기로 재처리
	 */
	@Async
	public CompletableFuture<Void> processPendingMessagesAsync() {
		processPendingMessages();
		return CompletableFuture.completedFuture(null);
	}

	/**
	 * Redis 재시작 시 ACK되지 않은 pending 메시지들을 재처리
	 */
	private void processPendingMessages() {
		try {
			// Consumer Group 전체의 pending 메시지들을 조회 (더 안전한 방법)
			List<PendingMessage> pendingMessages = redisTemplate.opsForStream()
				.pending(RedisConfig.ENTRY_QUEUE_KEY_NAME, RedisConfig.ENTRY_QUEUE_GROUP_NAME)
				.getMessages();

			if (!pendingMessages.isEmpty()) {
				log.info("Found {} pending messages for ENTRY queue consumer. Processing...", 
					pendingMessages.size());

				for (PendingMessage pendingMessage : pendingMessages) {
					try {
						// pending 메시지의 실제 데이터를 읽어옴
						List<MapRecord<String, String, String>> messages = redisTemplate.opsForStream()
							.range(RedisConfig.ENTRY_QUEUE_KEY_NAME, 
								org.springframework.data.domain.Range.closed(
									pendingMessage.getId().getValue(), 
									pendingMessage.getId().getValue()
								));

						// 각 메시지를 재처리
						for (MapRecord<String, String, String> message : messages) {
							log.info("Reprocessing pending ENTRY message: {}", message.getId());
							handleMessage(message);
						}
					} catch (Exception e) {
						log.error("Failed to reprocess pending ENTRY message {}: {}", 
							pendingMessage.getId(), e.getMessage());
						// 실패한 메시지는 ACK하여 다시 처리되지 않도록 함
						redisTemplate.opsForStream()
							.acknowledge(RedisConfig.ENTRY_QUEUE_KEY_NAME, RedisConfig.ENTRY_QUEUE_GROUP_NAME, 
								pendingMessage.getId());
					}
				}
			}
		} catch (Exception e) {
			log.error("Failed to process pending ENTRY messages: {}", e.getMessage());
		}
	}

}

