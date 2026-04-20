# POST /api/v1/payments/confirm Flow

## Entry Point

- `PurchaseCommandController.confirmPayment()`
- `POST /api/v1/payments/confirm`

## Flow

- 요청은 `PurchaseCommandController.confirmPayment()`에서 접수된다.
- `PurchaseConfirmCommandService.requestConfirm()`가 `CONFIRM_REQUESTED` 이벤트와 outbox 메시지를 만든다.
- worker가 event store의 `expectedSalesVersion`과 현재 `EventSummary.version`을 다시 비교한다.
- 불일치 시 PG confirm 이전에 종료하고, 일치할 때만 후속 정산으로 간다.

## Guardrails

- 입력 검증은 컨트롤러 경계에서 먼저 적용한다.
- 핵심 상태 변경은 서비스 계층에서 수행한다.
- 내부 경로는 외부 사용자 경로와 분리해서 본다.

## Related Docs

- [Use Case](../../usecase/purchase/PurchaseCommandController/confirmPayment.md)
- [Trouble](../../trouble/purchase/PurchaseCommandController/confirmPayment.md)
- [Troubleshooting](../../troubleshooting/purchase/PurchaseCommandController/confirmPayment.md)
