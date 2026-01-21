* 대기열 고쳐야 할 문제점
  고부하 위험
  - Redis KEYS를 1초마다 호출(전역 스캔)하고 있음. 키 수가 많아질수록 Redis 전체가 블로킹되어 지연/타임아웃이 발생할 수 있습니다. broker/src/main/java/org/codenbug/broker/infra/
    QueueInfoScheduler.java, dispatcher/src/main/java/org/codenbug/messagedispatcher/thread/EntryPromoteThread.java
  - 대기열 안내는 매 초마다 모든 ZSET 전체를 읽고, 각 사용자마다 ZRANK를 다시 호출(O(N^2)). 대기열이 커지면 Redis 부하와 SSE 전송량이 폭발합니다. broker/src/main/java/org/codenbug/
    broker/infra/QueueInfoScheduler.java
  - Lua 승격 스크립트가 “남은 자리만큼”이 아니라 “대기열 전체”를 순회합니다. 대기열이 큰 이벤트에서 Redis 단일 스레드를 길게 점유해 다른 명령이 막힐 가능성이 큽니다. dispatcher/src/
    main/resources/promote_all_waiting_for_event.lua
  - 1초 스케줄이 중첩될 수 있는데(이전 작업이 끝나기 전에 새 작업 제출), 동일 이벤트에 대한 프로모션 작업이 겹쳐서 Redis에 불필요한 부하를 줍니다. dispatcher/src/main/java/org/
    codenbug/messagedispatcher/thread/EntryPromoteThread.java
  - SSE 하트비트/상태 전송이 전 사용자에게 주기적으로 발생하며, 연결 수가 많을 때 브로커 인스턴스가 네트워크/CPU 병목이 됩니다. broker/src/main/java/org/codenbug/broker/infra/
    QueueInfoScheduler.java

  정합성/버그 (대기열 구현 정확성)

  - DISPATCH 메시지를 받은 뒤 sseConnection == null인 경우 NPE가 발생합니다. 현재 코드에서 null 처리 후 sseConnection.getEventId()를 바로 호출합니다. 결과적으로 ACK가 안 되고 pending
    에 남을 수 있습니다. broker/src/main/java/org/codenbug/broker/infra/EntryStreamMessageListener.java
  - WAITING_QUEUE_IN_USER_RECORD를 추가하는 로직이 실제로 없어서, 중복 진입 방지와 Lua 스크립트의 정리 로직이 무의미합니다(중복 대기열 엔트리 가능). broker/src/main/java/org/
    codenbug/broker/app/WaitingQueueEntryService.java, dispatcher/src/main/resources/promote_all_waiting_for_event.lua
  - @RedisLock(key = "#userId" + ":" + "#eventId")는 SpEL 표현식으로 안전하지 않습니다. 실제 키가 의도대로 동작하지 않을 가능성이 있고, 중복 진입 보호가 약합니다. broker/src/main/
    java/org/codenbug/broker/service/SseEmitterService.java
  - Entry 토큰 TTL이 실제 저장된 키에 걸리지 않습니다(해시는 ENTRY_TOKEN에 저장하지만 TTL은 ENTRY_TOKEN:<userId>로 설정). 만료가 동작하지 않아 토큰 누적/유효기간 관리가 틀어집니다.
    broker/src/main/java/org/codenbug/broker/infra/EntryStreamMessageListener.java
  - 승격 Lua가 중간에 자리가 없으면 return 0으로 종료하지만 이미 승격된 사용자는 롤백되지 않습니다. 프로모션 카운터는 0으로 기록되어 실제 승격 수와 불일치합니다. dispatcher/src/main/
    resources/promote_all_waiting_for_event.lua, dispatcher/src/main/java/org/codenbug/messagedispatcher/thread/EntryPromoteThread.java
  - ENTRY_QUEUE_SLOTS가 실제 “좌석 수”라면, IN_PROGRESS 종료 시 1을 증가시키는 로직은 재입장/재승격을 허용해 좌석 초과를 유발할 수 있습니다(의도한 의미 확인 필요). broker/src/main/
    java/org/codenbug/broker/service/SseEmitterService.java

  추상화 레벨 불일치

  - WaitingQueueEntryService가 외부 이벤트 서비스 호출(HTTP), Redis 저장, SSE 연결 상태 관리까지 담당합니다. 애플리케이션 서비스가 인프라/통신 세부 구현까지 포함해 결합도가 높습니다.
    broker/src/main/java/org/codenbug/broker/app/WaitingQueueEntryService.java
  - EntryStreamMessageListener가 스트림 소비 + 토큰 발급 + Redis 상태 정리까지 한 곳에서 처리합니다(리스너가 도메인/보안 책임을 가져감). broker/src/main/java/org/codenbug/broker/
    infra/EntryStreamMessageListener.java
  - SseEmitterService.closeConn이 static이며 Redis 처리까지 포함되어 있어, “연결 관리”와 “상태 저장”의 책임이 섞여 있습니다. broker/src/main/java/org/codenbug/broker/service/
    SseEmitterService.java