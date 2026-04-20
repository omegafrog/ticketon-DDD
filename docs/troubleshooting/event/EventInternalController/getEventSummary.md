# GET /internal/events/{eventId}/summary Troubleshooting

## Current State

- internal validation은 purchase worker의 gate로만 쓰인다.
- query/update 경계가 섞이지 않았는지 확인한다.

## Verification

- boolean guard와 400 parse failure를 분리한다.
- 권한이 필요한 경로는 role guard가 적용되는지 확인한다.

## Quantitative Notes

- internal guard count: `1`
- failure mode count: `2` (`400`, `false`)

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
- [Trouble](../../trouble/event/EventInternalController/getEventSummary.md)
