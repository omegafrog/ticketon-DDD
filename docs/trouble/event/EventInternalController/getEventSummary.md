# GET /internal/events/{eventId}/summary Trouble

## Before

- event 조회/변경 API는 manager, internal, batch, image 등 목적이 서로 다르다.
- 버전과 상태, 권한과 소유권이 섞이면 수정과 조회의 책임이 흐려진다.
- event list와 detail은 같은 엔티티를 보더라도 필요한 guard가 다르다.

## Decision Points

- 조회는 query service로, 변경은 command/service로 분리한다.
- 내부 호출은 외부 API보다 더 좁은 contract로 유지한다.
- 이미지/배치/manager 기능은 일반 CRUD와 독립적으로 본다.

## Failure Modes

- 권한 검사 누락은 잘못된 이벤트 수정으로 이어진다.
- 버전 제어가 빠지면 stale update가 발생한다.
- 조회와 변경이 섞이면 캐시와 소스오브트루스가 어긋난다.

## Why It Matters

- event는 읽기와 쓰기가 동시에 많아서 경계가 흐려지면 유지보수 비용이 빠르게 증가한다.

## Recent History

- [event-repository] `522f561` (2026-04-01): feat(purchase): EventSourcing 기반 결제 Confirm 플로우 + 스케줄러 기반 재시도 (#9)
- [controller] `9c6c823` (2026-03-31): refactor: unify RsData responses and split common/domain exceptions (#7)
- [controller] `ed148bf` (2026-02-05): feat: 결제 모듈 이벤트 소싱 전환 및 이벤트 점유(Hold) 로직 구현
- [event-query] `105d681` (2026-01-22): feat: 문서 정리 및 API 문서 삭제
- [controller] `c48631b` (2026-01-21): feat: 이벤트 상태 및 버전 검증 로직 추가
- [event-repository] `0f82549` (2026-01-21): feat: 좌석 상태 업데이트 및 이벤트 삭제 로직 개선
- [controller] `3fa11c3` (2026-01-20): delete: 불필요한 파일 및 설정 제거
- [event-query] `dd1dae9` (2025-08-20): feat: 매니저 이벤트 리스트 조회 API 추가
- [event-repository] `fbe21df` (2025-08-20): feat: 이벤트 검색 로직 개선
- [event-query] `b856823` (2025-07-22): feat: 문서화 추가
- [event-query] `10662e7` (2025-07-20): feat: 프런트 연동



## Related Docs

- [Use Case](../../usecase/event/EventInternalController/getEventSummary.md)
- [Flow](../../flow/event/EventInternalController/getEventSummary.md)
- [Troubleshooting](../../troubleshooting/event/EventInternalController/getEventSummary.md)
