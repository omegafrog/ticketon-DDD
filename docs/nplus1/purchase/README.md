# Purchase 모듈 N+1 및 개선 내용

## 문제 상황
환불 목록을 조회한 뒤 `refund.getPurchase()`를 접근하는 DTO 변환 흐름에서 N+1이 발생했습니다.

```java
private RefundDto convertToDto(Refund refund) {
    return RefundDto.builder()
        .refundId(refund.getRefundId().getValue())
        .purchaseId(refund.getPurchase().getPurchaseId().getValue())
        .refundAmount(refund.getRefundAmount().getValue())
        .refundReason(refund.getRefundReason().getValue())
        .status(refund.getStatus())
        .receiptUrl(refund.getReceiptUrl())
        .requestedAt(refund.getRequestedAt())
        .processedAt(refund.getProcessedAt())
        .isUserRefund(refund.isUserRefund())
        .isManagerRefund(refund.isManagerRefund())
        .isSystemRefund(refund.isSystemRefund())
        .build();
}
```

`Refund`는 `Purchase`를 LAZY로 참조합니다.

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "purchase_id", nullable = false)
private Purchase purchase;
```

## 원인
- `findByStatus()`로 환불 목록을 가져온 뒤
- 루프에서 `refund.getPurchase()`를 접근하면서 환불 수만큼 추가 쿼리 발생

## 해결 방법
환불 조회 시 `Purchase`를 함께 로딩하도록 레포지토리 쿼리를 수정했습니다.

```java
@EntityGraph(attributePaths = "purchase")
@Query("SELECT r FROM Refund r WHERE r.purchase.userId = :userId")
Page<Refund> findByPurchaseUserId(@Param("userId") UserId userId, Pageable pageable);

@Query("SELECT r FROM Refund r JOIN FETCH r.purchase WHERE r.status = :status")
List<Refund> findByStatus(@Param("status") RefundStatus status);
```

이렇게 하면 DTO 변환 시 `refund.getPurchase()` 접근으로 추가 쿼리가 발생하지 않습니다.
