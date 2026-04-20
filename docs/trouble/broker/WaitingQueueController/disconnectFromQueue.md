# POST /api/v1/broker/events/{id}/tickets/disconnect Trouble

## Before

- 연결 해제는 단순한 네트워크 종료가 아니라, 상태별 Redis 정리가 같이 일어나야 한다.
- IN_PROGRESS와 IN_ENTRY는 정리 대상이 다르며, 슬롯 반환 여부가 달라진다.
- 이전에 close hook이 idempotent하지 않으면 같은 사용자를 두 번 정리하는 문제가 있었다.

## Decision Points

- status가 `IN_PROGRESS`면 `ENTRY_QUEUE_SLOTS`를 다시 올리고 `ENTRY_TOKEN`을 제거한다.
- status가 `IN_ENTRY`면 waiting zset, index record, waiting user guard를 함께 제거한다.
- closeConnection은 한 번 더 불려도 안전해야 하므로, 이미 닫힌 connection은 경고만 남기고 종료한다.

## Failure Modes

- slot 반환이 누락되면 다음 사용자가 들어갈 수 없는 throughput 손실로 이어진다.
- waiting record만 지우고 token을 남기면 재접속 시 중복 진입 방지가 오히려 사용자를 막을 수 있다.
- 여러 콜백에서 중복 종료가 들어오면 slot 과반환이나 stale record가 생긴다.

## Why It Matters

- disconnect는 사용자 경험보다 시스템 회수율에 더 직접적인 영향을 준다.
- 정리 경계가 흐리면 queue backlog가 눈에 띄게 늘어난다.

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

- [Use Case](../../usecase/broker/WaitingQueueController/disconnectFromQueue.md)
- [Flow](../../flow/broker/WaitingQueueController/disconnectFromQueue.md)
- [Troubleshooting](../../troubleshooting/broker/WaitingQueueController/disconnectFromQueue.md)
