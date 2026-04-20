# POST /api/v1/payments/init

- Controller: `PurchaseCommandController.initiatePayment()`
- Actor: 사용자
- Goal: 결제, 환불, 구매 조회, confirm status, 테스트 쿼리를 처리한다.
- Source: `/mnt/e/workspace/ticketon-DDD/purchase/src/main/java/org/codenbug/purchase/ui/PurchaseCommandController.java`

## Use Case

결제, 환불, 구매 조회, confirm status, 테스트 쿼리를 처리한다.

## Success Criteria

- 요청은 `POST` `/api/v1/payments/init` 기준으로 해석한다.
- 컨트롤러는 자신이 맡은 입력 검증과 서비스 호출까지만 책임진다.
- 응답은 `ResponseEntity<RsData<InitiatePaymentResponse>>` 기준으로 외부 계약을 유지한다.

## Related Docs

- [Flow](../../flow/purchase/PurchaseCommandController/initiatePayment.md)
- [Trouble](../../trouble/purchase/PurchaseCommandController/initiatePayment.md)
- [Troubleshooting](../../troubleshooting/purchase/PurchaseCommandController/initiatePayment.md)
