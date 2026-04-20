# GET /api/v1/events/manager/me Trouble

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

- [controller] `9fcb0cc` (2026-04-02): refactor(event): holder를 사용해서 purchase concurrency를 관리하지 않도록 수정
- [controller] `ac8e320` (2025-12-15): feat : event 작성 controller validation 수정
- [controller] `ff09c7c` (2025-09-02): docs: 이벤트 조회 API 카테고리 필터링 문서 업데이트
- [controller] `0ee3bc9` (2025-09-02): feat: 모든 컨트롤러에 Swagger API 문서화 어노테이션 추가
- [controller] `e4b36f4` (2025-08-22): refactor: 구조 변경에 따른 import 및 참조 업데이트



## Related Docs

- [Use Case](../../usecase/event/EventQueryController/getManagerEvents.md)
- [Flow](../../flow/event/EventQueryController/getManagerEvents.md)
- [Troubleshooting](../../troubleshooting/event/EventQueryController/getManagerEvents.md)
