# Purchase Confirm Version Mismatch

## Actor

- 사용자
- 결제 시스템

## Goal

사용자는 결제 요청을 보낸 뒤, 결제 중 상품 정보가 바뀌면 잘못된 승인으로 이어지지 않기를 원한다.
결제 시스템은 승인 전에 최신 이벤트 상태와 기대 버전을 다시 확인해야 한다.

## Use Cases

### 1. 결제 confirm 요청 접수

- 액터: 사용자
- 시작점: `PurchaseCommandController.confirmPayment()`
- 기대 결과: 결제 confirm 요청이 접수되고, 비동기 worker가 처리할 수 있는 상태가 된다.

### 2. 결제 중 이벤트 변경 차단

- 액터: 결제 시스템
- 시작점: worker가 `CONFIRM_REQUESTED` 이벤트를 소비하는 시점
- 기대 결과: 저장된 `expectedSalesVersion`과 현재 이벤트 `version`이 다르면 승인으로 넘어가지 않는다.

## Success Criteria

- version mismatch는 결제 승인 전에 차단된다.
- 실패 시 PG confirm 단계는 호출되지 않는다.
- 실패는 재현 가능해야 하며, 로그와 테스트로 확인 가능해야 한다.

## Related Docs

- [Flow](../flow/purchase-confirm-worker-version-mismatch.md)
- [Trouble](../trouble/purchase-confirm-worker-version-mismatch.md)
- [Troubleshooting](../troubleshooting/purchase-confirm-worker-version-mismatch.md)
- Legacy: `docs/purchase/purchase-confirm-worker-version-mismatch/use-cases.md`
