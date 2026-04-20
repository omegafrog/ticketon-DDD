# GET /api/v1/events/manager/me Troubleshooting

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

- [controller] `9fcb0cc` (2026-04-02): refactor(event): holder를 사용해서 purchase concurrency를 관리하지 않도록 수정
- [controller] `ac8e320` (2025-12-15): feat : event 작성 controller validation 수정
- [controller] `ff09c7c` (2025-09-02): docs: 이벤트 조회 API 카테고리 필터링 문서 업데이트
- [controller] `0ee3bc9` (2025-09-02): feat: 모든 컨트롤러에 Swagger API 문서화 어노테이션 추가
- [controller] `e4b36f4` (2025-08-22): refactor: 구조 변경에 따른 import 및 참조 업데이트



## Related Docs

- [Use Case](../../usecase/event/EventQueryController/getManagerEvents.md)
- [Flow](../../flow/event/EventQueryController/getManagerEvents.md)
- [Trouble](../../trouble/event/EventQueryController/getManagerEvents.md)
