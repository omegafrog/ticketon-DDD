# POST /api/v1/payments/confirm

- Controller: `PurchaseCommandController.confirmPayment()`
- Actor: 사용자
- Goal: 사용자는 결제 승인 요청을 보낸 뒤, 승인 과정에서 이벤트가 바뀌면 잘못된 결제가 확정되지 않기를 원한다.
- Source: `/mnt/e/workspace/ticketon-DDD/purchase/src/main/java/org/codenbug/purchase/ui/PurchaseCommandController.java`

## Use Case

사용자는 결제 승인 요청을 보낸 뒤, 승인 과정에서 이벤트가 바뀌면 잘못된 결제가 확정되지 않기를 원한다.

## Success Criteria

- 요청은 `POST` `/api/v1/payments/confirm` 기준으로 해석한다.
- 컨트롤러는 자신이 맡은 입력 검증과 서비스 호출까지만 책임진다.
- 응답은 `ResponseEntity<RsData<ConfirmPaymentAcceptedResponse>>` 기준으로 외부 계약을 유지한다.

## Related Docs

- [Flow](../../flow/purchase/PurchaseCommandController/confirmPayment.md)
- [Trouble](../../trouble/purchase/PurchaseCommandController/confirmPayment.md)
- [Troubleshooting](../../troubleshooting/purchase/PurchaseCommandController/confirmPayment.md)
