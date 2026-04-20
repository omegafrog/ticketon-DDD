# Purchase Confirm Version Mismatch Trouble

## Before

이 흐름의 이전 계열 구현은 결제 confirm 단계에서 상태를 사후 보정하는 쪽에 더 가까웠다.

- `PurchaseService.confirmPaymentWithSaga(...)` 시절에는 이벤트 version을 먼저 캡처하고, 최종 승인 단계에서 다시 비교했다.
- 그 다음 단계에서는 `PurchaseConfirmWorker`가 event hold / lock 계열 제어를 거쳤다.
- `4c79957`에서는 PG confirm 구간을 pessimistic DB row lock으로 감싸는 방향으로 바뀌었다.
- `836c064`과 `522f561` 계열에서는 이벤트소싱 confirm worker로 전환되면서 hold 기반 흐름이 유지됐고, `9fcb0cc`에서 holder 기반 concurrency 관리가 제거됐다.

## Problem

이 방식은 두 가지 문제가 있었다.

- PG confirm 같은 외부 I/O가 길어질수록 DB 락이나 hold가 길게 유지된다.
- version mismatch를 늦게 발견하면 이미 비싼 외부 호출 흐름을 타고 난 뒤라서, 실패 복구 비용이 커진다.
- version check가 "승인 직전"이 아니라 "승인 중간 또는 이후"에 가까우면, 잘못된 상태를 되돌리는 비용이 커진다.

## Why It Changed

- 결제 승인 전에 version mismatch를 판정해야 한다.
- hold/lock 대신, worker가 event store의 기대 version과 현재 이벤트 요약을 바로 비교하는 편이 더 단순하다.
- `c48631b`에서 이벤트 상태/버전 검증 API가 추가되면서, worker가 재검증할 수 있는 기반이 생겼다.

## Design Direction

- `CONFIRM_REQUESTED` payload에 `expectedSalesVersion`을 넣는다.
- worker는 PG confirm 전에 현재 version을 재조회한다.
- 불일치 시 즉시 실패하고, PG 호출과 후속 finalization을 막는다.

## Related Docs

- [Use Case](../usecase/purchase-confirm-worker-version-mismatch.md)
- [Flow](../flow/purchase-confirm-worker-version-mismatch.md)
- [Troubleshooting](../troubleshooting/purchase-confirm-worker-version-mismatch.md)
- Legacy plan: `docs/purchase/purchase-confirm-worker-version-mismatch-plan.md`
