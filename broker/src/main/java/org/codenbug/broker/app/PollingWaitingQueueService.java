package org.codenbug.broker.app;

import org.codenbug.broker.config.InstanceConfig;
import org.codenbug.broker.infra.WaitingQueueRedisRepository;
import org.codenbug.broker.ui.PollingQueueInfo;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.springframework.stereotype.Service;


@Service
public class PollingWaitingQueueService {

	private static final long USER_QUEUE_EVENT_TTL_SECONDS = 30;

	private final EventClient eventClient;
	private final WaitingQueueRedisRepository waitingQueueRedisRepository;
	private final org.codenbug.broker.infra.EventStatusInitializer eventStatusInitializer;
	private final InstanceConfig instanceConfig;

	public PollingWaitingQueueService(EventClient eventClient, WaitingQueueRedisRepository waitingQueueRedisRepository,
		org.codenbug.broker.infra.EventStatusInitializer eventStatusInitializer, InstanceConfig instanceConfig) {
		this.eventClient = eventClient;
		this.waitingQueueRedisRepository = waitingQueueRedisRepository;
		this.eventStatusInitializer = eventStatusInitializer;
		this.instanceConfig = instanceConfig;
	}

	public void enter( String eventId) {
		String userId = LoggedInUserContext.get().getUserId();
		if (waitingQueueRedisRepository.isUserExistInEntry(userId)) {
			throw new IllegalStateException("이미 입장 토큰이 발급되었습니다.");
		}
		boolean setQueueEvent = waitingQueueRedisRepository.setUserQueueEventIfAbsent(userId, eventId);
		if (!setQueueEvent) {
			String currentEventId = waitingQueueRedisRepository.getUserQueueEvent(userId);
			if (currentEventId != null && !currentEventId.equals(eventId)) {
				throw new IllegalStateException("이미 다른 이벤트 대기열에 존재합니다.");
			}
		}
		if (!waitingQueueRedisRepository.entryQueueCountExists(eventId)) {
			int seatCount = eventClient.getSeatCount(eventId);
			waitingQueueRedisRepository.updateEntryQueueCount(eventId, seatCount);
		}

		eventStatusInitializer.ensureInitialized(eventId);

		boolean inserted = waitingQueueRedisRepository.recordWaitingUserIfAbsent(eventId, userId);
		if (!inserted) {
			throw new IllegalStateException("이미 대기열에 존재합니다.");
		}
		eventClient.getSeatStatus(eventId);

		long idx = waitingQueueRedisRepository.incrementWaitingQueueIdx(eventId);

		waitingQueueRedisRepository.saveUserToWaitingQueue(userId, eventId, idx);
		waitingQueueRedisRepository.updateWaitingLastSeen(eventId, userId, System.currentTimeMillis());

		waitingQueueRedisRepository.saveAdditionalUserData(userId, eventId, idx,
			instanceConfig.getInstanceId());
	}

	public PollingQueueInfo parseOrder(String eventId) {
		String userId = LoggedInUserContext.get().getUserId();
		waitingQueueRedisRepository.refreshUserQueueEventTtl(userId, USER_QUEUE_EVENT_TTL_SECONDS);

		String entryToken = waitingQueueRedisRepository.getEntryToken(userId);
		if (entryToken != null) {
			waitingQueueRedisRepository.updateEntryLastSeen(userId, System.currentTimeMillis());
			return new PollingQueueInfo(
				"ENTRY",
				null,
				entryToken,
				calculatePollAfterMs(eventId, null, true)
			);
		}

		Long rank = waitingQueueRedisRepository.getUserRank(eventId, userId);
		if (rank != null) {
			Long adjustedRank = rank + 1;
			waitingQueueRedisRepository.updateWaitingLastSeen(eventId, userId, System.currentTimeMillis());
			return new PollingQueueInfo(
				"WAITING",
				adjustedRank,
				null,
				calculatePollAfterMs(eventId, adjustedRank, false)
			);
		}

		return new PollingQueueInfo(
			"NONE",
			null,
			null,
			calculatePollAfterMs(eventId, null, false)
		);
	}

	private long calculatePollAfterMs(String eventId, Long rank, boolean hasEntryToken) {
		long baseMs = calculateBasePollAfterMs(rank, hasEntryToken);
		if (hasEntryToken) {
			return baseMs;
		}

		boolean includeWaitingQueueSize = rank != null && rank > 100;
		WaitingQueueRedisRepository.PollingAdaptiveContext context =
			waitingQueueRedisRepository.getPollingAdaptiveContext(eventId, includeWaitingQueueSize);

		String eventStatus = context.eventStatus();
		if (eventStatus != null && !"OPEN".equals(eventStatus)) {
			return 30000L;
		}

		long adaptedMs = baseMs;

		Long entrySlots = context.entryQueueSlots();
		if (entrySlots != null && entrySlots <= 0) {
			adaptedMs = Math.max(adaptedMs, 8000L);
		}

		if (includeWaitingQueueSize) {
			Long waitingSize = context.waitingQueueSize();
			if (waitingSize != null && waitingSize >= 5000) {
				adaptedMs = Math.max(adaptedMs, 10000L);
			} else if (waitingSize != null && waitingSize >= 1000) {
				adaptedMs = Math.max(adaptedMs, 7000L);
			}
		}

		return adaptedMs;
	}

	private long calculateBasePollAfterMs(Long rank, boolean hasEntryToken) {
		if (hasEntryToken) {
			return 1000L;
		}
		if (rank == null) {
			return 5000L;
		}
		if (rank <= 10) {
			return 1000L;
		}
		if (rank <= 100) {
			return 3000L;
		}
		return 5000L;
	}

	public void disconnect(String eventId) {
		String userId = LoggedInUserContext.get().getUserId();
		waitingQueueRedisRepository.deleteUserFromEntry( userId);
		waitingQueueRedisRepository.deleteUserFromWaiting(eventId, userId);
		waitingQueueRedisRepository.incrementEntryQueueCount(eventId);
	}
}
