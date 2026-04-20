# POST /api/v1/test/cache-invalidation Trouble

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

- [controller] `fb6b9dd` (2026-01-21): feat: 캐시 무효화 응답 형식 개선 및 예외 처리 핸들러 추가
- [controller] `022985e` (2025-08-22): feat: 캐시 무효화 시스템 구현



## Related Docs

- [Use Case](../../usecase/event/CacheTestController/testCacheInvalidation.md)
- [Flow](../../flow/event/CacheTestController/testCacheInvalidation.md)
- [Troubleshooting](../../troubleshooting/event/CacheTestController/testCacheInvalidation.md)
