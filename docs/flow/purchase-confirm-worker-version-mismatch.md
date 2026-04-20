# Purchase Confirm Version Mismatch Flow

## Flow Summary

현재 결제 confirm 흐름은 `PurchaseConfirmCommandService`가 요청을 접수하고, `PurchaseConfirmWorker`가 event store를 다시 읽은 뒤, 외부 이벤트 요약을 조회해 version을 검증하는 방식이다.

## Main Steps

1. `PurchaseCommandController.confirmPayment()`가 요청을 받는다.
2. `PurchaseConfirmCommandService.requestConfirm()`가 `CONFIRM_REQUESTED` 이벤트와 outbox 메시지를 만든다.
3. worker가 outbox/queue 메시지를 소비한다.
4. worker가 `PurchaseStoredEvent`에서 `expectedSalesVersion`과 `eventId`를 복원한다.
5. worker가 `EventServiceClient.getEventSummary(eventId)`로 현재 version을 조회한다.
6. version이 다르면 `ConcurrencyFailureException`을 만들고 `RuntimeException`으로 전환해 실패시킨다.
7. version이 같을 때만 PG confirm과 후속 정산으로 진행한다.

## Key Implementation Points

- 검증 위치는 PG 호출 직전이다.
- 불일치 시 `paymentProviderRouter.get(...)` 이전에 종료한다.
- confirm context는 event store payload에 들어 있는 값만 사용한다.

## Related Docs

- [Use Case](../usecase/purchase-confirm-worker-version-mismatch.md)
- [Trouble](../trouble/purchase-confirm-worker-version-mismatch.md)
- [Troubleshooting](../troubleshooting/purchase-confirm-worker-version-mismatch.md)
- Legacy: `docs/purchase/payment-flow.md`
- Legacy: `docs/purchase/payment-event-version-check.md`
