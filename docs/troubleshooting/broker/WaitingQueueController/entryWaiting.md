# GET /api/v1/broker/events/{id}/tickets/waiting Troubleshooting

## Current State

- SSE emitter는 `0L` timeout으로 생성되어 장기 연결을 허용한다.
- 대기열 제어의 기준값은 `ENTRY_QUEUE_CAPACITY = 100`이다.
- dispatch listener는 `pollTimeout = 1s`로 스트림을 확인한다.

## Verification

- `WAITING_USER_IDS` 중복 방지와 `ENTRY_TOKEN` 유무를 함께 본다.
- queue rank는 `WAITING_QUEUE_IDX` 단조 증가값으로 검사한다.
- 인스턴스별 `DISPATCH:{instanceId}` 스트림으로 ACK 범위를 줄인다.

## Quantitative Notes

- guard scope: `1 userId / 1 eventId`
- queue capacity baseline: `100`
- poll cadence: `1s`

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
- [Trouble](../../trouble/broker/WaitingQueueController/entryWaiting.md)
