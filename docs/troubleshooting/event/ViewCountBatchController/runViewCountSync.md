# POST /api/v1/batch/viewcount-sync Troubleshooting

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

- [controller] `df9d9bd` (2025-08-21): feat: Event Redis ViewCount 시스템 및 N+1 문제 해결 구현



## Related Docs

- [Use Case](../../usecase/event/ViewCountBatchController/runViewCountSync.md)
- [Flow](../../flow/event/ViewCountBatchController/runViewCountSync.md)
- [Trouble](../../trouble/event/ViewCountBatchController/runViewCountSync.md)
