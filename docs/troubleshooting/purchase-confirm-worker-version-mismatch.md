# Purchase Confirm Version Mismatch Troubleshooting

## Before Fix

- version mismatch가 나도 confirm worker가 뒤 단계로 더 들어가는 위험이 있었다.
- hold / lock 기반 보호는 외부 PG 호출과 분리되어 있지 않아 실패 시점이 늦었다.

## Fix

- worker가 `EventServiceClient.getEventSummary(eventId)`를 PG confirm 전에 호출한다.
- `expectedSalesVersion`과 현재 `version`이 다르면 `RuntimeException`으로 중단한다.
- mismatch 시 `paymentProviderRouter`와 `finalizationService`는 호출되지 않는다.

## Quantitative Verification

- 검증 명령: `./gradlew :purchase:test --tests org.codenbug.purchase.app.es.PurchaseConfirmWorkerTest --no-daemon --console=plain`
- 결과: `tests=1, failures=0, errors=0`
- 예외 메시지: `결제 도중 상품 내용이 변경되었습니다.`
- PG confirm 진입 수: `0`
- finalization 진입 수: `0`

## Related Docs

- [Use Case](../usecase/purchase-confirm-worker-version-mismatch.md)
- [Flow](../flow/purchase-confirm-worker-version-mismatch.md)
- [Trouble](../trouble/purchase-confirm-worker-version-mismatch.md)
- Legacy: `docs/purchase/purchase-confirm-worker-version-mismatch-plan.md`
