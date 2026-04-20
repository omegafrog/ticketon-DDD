# PUT /api/v1/events/{eventId} Troubleshooting

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

- [controller] `4c79957` (2026-03-31): refactor(purchase): replace logical hold with pessimistic DB row lock during PG confirm
- [controller] `9c6c823` (2026-03-31): refactor: unify RsData responses and split common/domain exceptions (#7)
- [controller] `105d681` (2026-01-22): feat: 문서 정리 및 API 문서 삭제
- [controller] `96e3084` (2026-01-21): feat: 이벤트 수정 로직 및 데이터 처리 방식 개선
- [controller] `ac8e320` (2025-12-15): feat : event 작성 controller validation 수정



## Related Docs

- [Use Case](../../usecase/event/EventCommandController/updateEvent.md)
- [Flow](../../flow/event/EventCommandController/updateEvent.md)
- [Trouble](../../trouble/event/EventCommandController/updateEvent.md)
