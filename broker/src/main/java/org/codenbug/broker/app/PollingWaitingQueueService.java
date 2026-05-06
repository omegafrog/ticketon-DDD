package org.codenbug.broker.app;

import org.codenbug.broker.config.InstanceConfig;
import org.codenbug.broker.ui.PollingQueueInfo;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.springframework.stereotype.Service;

@Service
public class PollingWaitingQueueService {

  private static final long USER_QUEUE_EVENT_TTL_SECONDS = 30;

  private final EventClient eventClient;
  private final WaitingQueueStore waitingQueueRedisRepository;
  private final EventStatusInitializationPort eventStatusInitializer;
  private final InstanceConfig instanceConfig;
  private final QueueObservation queueObservation;

  public PollingWaitingQueueService(EventClient eventClient, WaitingQueueStore waitingQueueRedisRepository,
      EventStatusInitializationPort eventStatusInitializer, InstanceConfig instanceConfig) {
    this(eventClient, waitingQueueRedisRepository, eventStatusInitializer, instanceConfig,
        QueueObservation.noop());
  }

  @org.springframework.beans.factory.annotation.Autowired
  public PollingWaitingQueueService(EventClient eventClient, WaitingQueueStore waitingQueueRedisRepository,
      EventStatusInitializationPort eventStatusInitializer, InstanceConfig instanceConfig,
      QueueObservation queueObservation) {
    this.eventClient = eventClient;
    this.waitingQueueRedisRepository = waitingQueueRedisRepository;
    this.eventStatusInitializer = eventStatusInitializer;
    this.instanceConfig = instanceConfig;
    this.queueObservation = queueObservation;
  }

  public void enter(String eventId) {
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
    String eventStatus = eventClient.getSeatStatus(eventId);
    if (!"OPEN".equals(eventStatus)) {
      waitingQueueRedisRepository.clearUserQueueEvent(userId);
      throw new IllegalStateException("예매 가능한 이벤트가 아닙니다.");
    }

    boolean inserted = waitingQueueRedisRepository.recordWaitingUserIfAbsent(eventId, userId);
    if (!inserted) {
      throw new IllegalStateException("이미 대기열에 존재합니다.");
    }
    long idx = waitingQueueRedisRepository.incrementWaitingQueueIdx(eventId);

    waitingQueueRedisRepository.saveUserToWaitingQueue(userId, eventId, idx);
    waitingQueueRedisRepository.updateWaitingLastSeen(eventId, userId, System.currentTimeMillis());

    waitingQueueRedisRepository.saveAdditionalUserData(userId, eventId, idx,
        instanceConfig.getInstanceId());
    recordQueueState(eventId);
  }

  public PollingQueueInfo parseOrder(String eventId) {
    String userId = LoggedInUserContext.get().getUserId();
    waitingQueueRedisRepository.refreshUserQueueEventTtl(userId, USER_QUEUE_EVENT_TTL_SECONDS);

    String entryToken = waitingQueueRedisRepository.getEntryToken(userId);
    if (entryToken != null) {
      waitingQueueRedisRepository.updateEntryLastSeen(userId, System.currentTimeMillis());
      PollingQueueInfo info = new PollingQueueInfo(
          "ENTRY",
          null,
          entryToken,
          calculatePollAfterMs(eventId, null, true));
      queueObservation.recordPollingRequest(eventId, info.getState(), info.getPollAfterMs());
      recordQueueState(eventId);
      return info;
    }

    Long rank = waitingQueueRedisRepository.getUserRank(eventId, userId);
    if (rank != null) {
      Long adjustedRank = rank + 1;
      waitingQueueRedisRepository.updateWaitingLastSeen(eventId, userId, System.currentTimeMillis());
      PollingQueueInfo info = new PollingQueueInfo(
          "WAITING",
          adjustedRank,
          null,
          calculatePollAfterMs(eventId, adjustedRank, false));
      queueObservation.recordPollingRequest(eventId, info.getState(), info.getPollAfterMs());
      recordQueueState(eventId);
      return info;
    }

    PollingQueueInfo info = new PollingQueueInfo(
        "NONE",
        null,
        null,
        calculatePollAfterMs(eventId, null, false));
    queueObservation.recordPollingRequest(eventId, info.getState(), info.getPollAfterMs());
    recordQueueState(eventId);
    return info;
  }

  private long calculatePollAfterMs(String eventId, Long rank, boolean hasEntryToken) {
    long baseMs = calculateBasePollAfterMs(rank, hasEntryToken);
    if (hasEntryToken) {
      return baseMs;
    }

    boolean includeWaitingQueueSize = rank != null && rank > 100;
    WaitingQueueStore.PollingAdaptiveContext context = waitingQueueRedisRepository.getPollingAdaptiveContext(eventId,
        includeWaitingQueueSize);

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
    return 5000L;
  }

  public void disconnect(String eventId) {
    String userId = LoggedInUserContext.get().getUserId();
    boolean removedEntryToken = waitingQueueRedisRepository.deleteUserFromEntry(userId);
    boolean removedWaiting = waitingQueueRedisRepository.deleteUserFromWaiting(eventId, userId);
    if (removedWaiting) {
      waitingQueueRedisRepository.deleteWaitingUserRecord(eventId, userId);
    }
    waitingQueueRedisRepository.clearUserQueueEvent(userId);
    if (removedEntryToken) {
      waitingQueueRedisRepository.incrementEntryQueueCount(eventId);
    }
    recordQueueState(eventId);
  }

  private void recordQueueState(String eventId) {
    queueObservation.recordQueueState(eventId, waitingQueueRedisRepository.getWaitingQueueSize(eventId),
        waitingQueueRedisRepository.getEntryQueueSlots(eventId));
  }
}
