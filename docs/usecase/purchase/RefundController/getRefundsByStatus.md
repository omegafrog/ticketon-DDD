# GET /api/v1/refunds/admin/by-status

- Controller: `RefundController.getRefundsByStatus()`
- Actor: 외부 호출자
- Goal: 결제, 환불, 구매 조회, confirm status, 테스트 쿼리를 처리한다.
- Source: `/mnt/e/workspace/ticketon-DDD/purchase/src/main/java/org/codenbug/purchase/ui/RefundController.java`

## Use Case

결제, 환불, 구매 조회, confirm status, 테스트 쿼리를 처리한다.

## Success Criteria

- 요청은 `GET` `/api/v1/refunds/admin/by-status` 기준으로 해석한다.
- 컨트롤러는 자신이 맡은 입력 검증과 서비스 호출까지만 책임진다.
- 응답은 `ResponseEntity<RsData<List<RefundQueryService.RefundDto>>>` 기준으로 외부 계약을 유지한다.

## Related Docs

- [Flow](../../flow/purchase/RefundController/getRefundsByStatus.md)
- [Trouble](../../trouble/purchase/RefundController/getRefundsByStatus.md)
- [Troubleshooting](../../troubleshooting/purchase/RefundController/getRefundsByStatus.md)
