# GET /api/v1/broker/events/{id}/tickets/waiting Trouble

## Before

- SSE 연결이 먼저 살아 있어야 사용자가 이후 승격 메시지를 받을 수 있다.
- 대기열 순번은 HTTP 요청 시점이 아니라 Redis의 단조 증가 `idx`로 고정된다.
- 동일 userId의 중복 진입은 `WAITING_USER_IDS`와 `ENTRY_TOKEN` 둘 다에서 막아야 한다.

## Decision Points

- `SseEmitter(0L)`로 서버 타임아웃보다 긴-lived 연결을 만들고, 끊김은 completion/error/timeout 콜백에서 정리한다.
- 대기열 진입 전 `ENTRY_QUEUE_SLOTS`를 좌석 수로 초기화해서, 승격 가능한 총량을 기준점으로 만든다.
- 인스턴스별 `DISPATCH:{instanceId}` 스트림을 써서 여러 노드가 같은 승격 메시지를 동시에 처리하지 않게 한다.

## Failure Modes

- emitters를 Redis보다 늦게 만들면, 등록은 됐는데 연결이 없는 유령 대기자가 남을 수 있다.
- 순번을 ZSET score가 아닌 요청 도착 순서로 계산하면 scale-out 환경에서 순서가 흔들린다.
- 중복 진입을 `SSE` 레벨에서만 막으면 브라우저 탭 중복이나 재시도에서 queue 상태가 먼저 오염된다.

## Why It Matters

- 이 엔드포인트는 fairness, admission capacity, and reconnect semantics를 동시에 만족해야 한다.
- 조금만 느슨해져도 사용자 체감상 ‘줄이 밀린다’는 문제가 바로 드러난다.

## Recent History

- [controller] `9c6c823` (2026-03-31): refactor: unify RsData responses and split common/domain exceptions (#7)
- [queue-service] `0ae509c` (2026-02-05): feat: 대기열 폴링 주기 최적화 및 중복 진입 방지 로직 추가
- [queue-listener] `ed148bf` (2026-02-05): feat: 결제 모듈 이벤트 소싱 전환 및 이벤트 점유(Hold) 로직 구현
- [queue-service] `afd3c9b` (2026-02-04): feat: Polling 대기열 시스템 구현 및 Redis 큐 구조 최적화
- [queue-listener] `9b3acdd` (2026-02-02): feat: EventViewRepository 예외 처리 개선 및 Broker 의존성 리팩터링
- [queue-service] `6215885` (2026-01-21): feat: Redis 키 네이밍 변경 및 Lua 스크립트 개선, API 문서 추가
- [queue-service] `0c89f45` (2026-01-21): feat: 중복 대기열 진입 방지 로직 추가 및 GlobalExceptionHandler 개선
- [queue-listener] `b5ecf96` (2026-01-21): feat: Redis 토큰 저장 및 삭제 로직 변경, MySQL 설정 파일 경로 수정
- [controller] `ca5516c` (2025-10-27): refactor: 코드 컨벤션에 맞게 탭을 스페이스로 변경
- [controller] `0ee3bc9` (2025-09-02): feat: 모든 컨트롤러에 Swagger API 문서화 어노테이션 추가
- [controller] `712962e` (2025-08-18): chore: merge conflict 해결
- [controller] `11d59cb` (2025-07-24): feat: 문서화 추가



## Related Docs

- [Use Case](../../usecase/broker/WaitingQueueController/entryWaiting.md)
- [Flow](../../flow/broker/WaitingQueueController/entryWaiting.md)
- [Troubleshooting](../../troubleshooting/broker/WaitingQueueController/entryWaiting.md)
