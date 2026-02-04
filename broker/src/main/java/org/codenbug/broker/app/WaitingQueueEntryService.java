package org.codenbug.broker.app;

import org.codenbug.broker.config.InstanceConfig;
import org.codenbug.broker.infra.WaitingQueueRedisRepository;
import org.codenbug.broker.service.SseEmitterService;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WaitingQueueEntryService {

  private final SseEmitterService sseEmitterService;
	private final WaitingQueueRedisRepository waitingQueueRepository;
	private final EventClient eventClient;
	private final org.codenbug.broker.infra.EventStatusInitializer eventStatusInitializer;
	private final InstanceConfig instanceConfig;

	public WaitingQueueEntryService(SseEmitterService sseEmitterService,
		WaitingQueueRedisRepository waitingQueueRepository, EventClient eventClient,
		org.codenbug.broker.infra.EventStatusInitializer eventStatusInitializer,
		InstanceConfig instanceConfig) {
		this.sseEmitterService = sseEmitterService;
		this.waitingQueueRepository = waitingQueueRepository;
		this.eventClient = eventClient;
		this.eventStatusInitializer = eventStatusInitializer;
		this.instanceConfig = instanceConfig;
	}

  public SseEmitter entry(String eventId) throws JsonProcessingException {
    // 로그인한 유저 id 조회
    String id = getLoggedInUserId();
    SseEmitter emitter;
    // emitter 생성 및 저장
    try {
      emitter = sseEmitterService.add(id, eventId);
    } catch (IllegalStateException e) {
      log.error("다른 대기열에 이미 들어와 있습니다.");
      throw e;
    }

    enter(id, eventId);
    assert emitter != null;
    return emitter;
  }

  /**
   * 사용자를 대기열에 추가한다. redis의 메시지 idx를 가져와 메시지의 idx로 추가하고 1을 증가시킨다. 이후 WAITING stream에 메시지를 추가해 사용자를
   * 대기열에 추가한다.
   *
   * @param userId 대기열에 추가할 유저 id
   * @param eventId 행사의 id
   */
  private void enter(String userId, String eventId) throws JsonProcessingException {

		if (!waitingQueueRepository.entryQueueCountExists(eventId)) {
			int seatCount = eventClient.getSeatCount(eventId);
			waitingQueueRepository.updateEntryQueueCount(eventId, seatCount);
		}

		eventStatusInitializer.ensureInitialized(eventId);

		boolean inserted = waitingQueueRepository.recordWaitingUserIfAbsent(eventId, userId);
    if (!inserted) {
      throw new IllegalStateException("이미 대기열에 존재합니다.");
    }

    long idx = waitingQueueRepository.incrementWaitingQueueIdx(eventId);

    waitingQueueRepository.saveUserToWaitingQueue(userId, eventId, idx);

    waitingQueueRepository.saveAdditionalUserData(userId, eventId, idx,
        instanceConfig.getInstanceId());

  }

  /**
   * 현재 로그인한 사용자의 대기열 연결을 명시적으로 해제합니다. IN_PROGRESS 상태에 도달한 후 즉시 호출하여 다음 사용자가 빠르게 승급할 수 있도록 돕니다.
   *
   * @param eventId 행사의 id
   * @return 성공 시 200 OK 응답
   */
  public ResponseEntity<Void> disconnect(String eventId) {
    String userId = getLoggedInUserId();

    // SSE 연결 해제 및 리소스 정리
    sseEmitterService.closeConnection(userId, eventId);

    return ResponseEntity.ok().build();
  }

  private String getLoggedInUserId() {
    return LoggedInUserContext.get().getUserId();
  }
}
