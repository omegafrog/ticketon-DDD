# POST /api/v1/test/cache-invalidation Troubleshooting

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

- [controller] `fb6b9dd` (2026-01-21): feat: 캐시 무효화 응답 형식 개선 및 예외 처리 핸들러 추가
- [controller] `022985e` (2025-08-22): feat: 캐시 무효화 시스템 구현



## Related Docs

- [Use Case](../../usecase/event/CacheTestController/testCacheInvalidation.md)
- [Flow](../../flow/event/CacheTestController/testCacheInvalidation.md)
- [Trouble](../../trouble/event/CacheTestController/testCacheInvalidation.md)
