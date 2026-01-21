# API 엔드포인트 응답/상태코드 문서 (코드 기준)

컨트롤러 소스 기준으로 엔드포인트, 리턴 타입, 상태코드를 정리하고 RsData 응답 규칙 준수 여부를 확인했습니다.

## Auth

| Method | Path | Return type | Status code(s) | RsData 응답 |
| --- | --- | --- | --- | --- |
| POST | `/api/v1/auth/register` | `ResponseEntity<RsData<SecurityUserId>>` | 200 | O |
| POST | `/api/v1/auth/login` | `ResponseEntity<RsData<String>>` | 200 | O |
| GET | `/api/v1/auth/logout` | `ResponseEntity<RsData<Void>>` | 200 | O |
| GET | `/api/v1/auth/social/{socialLoginType}` | `ResponseEntity<String>` | 200 | X |
| GET | `/api/v1/auth/social/{socialLoginType}/callback` | `ResponseEntity<RsData<String>>` | 200, 500 | O |
| POST | `/api/v1/auth/refresh` | `ResponseEntity<RsData<String>>` | 200, 401 | O |

## User

| Method | Path | Return type | Status code(s) | RsData 응답 |
| --- | --- | --- | --- | --- |
| GET | `/api/v1/users/me` | `ResponseEntity<RsData<UserInfo>>` | 200 | O |
| PUT | `/api/v1/users/me` | `ResponseEntity<RsData<UserInfo>>` | 200 | O |

## Event

| Method | Path | Return type | Status code(s) | RsData 응답 |
| --- | --- | --- | --- | --- |
| POST | `/api/v1/events` | `ResponseEntity<RsData<EventId>>` | 200 | O |
| PUT | `/api/v1/events/{eventId}` | `ResponseEntity<RsData<EventId>>` | 200 | O |
| DELETE | `/api/v1/events/{eventId}` | `ResponseEntity<RsData<Void>>` | 200 | O |
| PATCH | `/api/v1/events/{eventId}` | `ResponseEntity<RsData<EventId>>` | 200 | O |
| POST | `/api/v1/events/list` | `ResponseEntity<RsData<Page<EventListProjection>>>` | 200 | O |
| GET | `/api/v1/events/{id}` | `ResponseEntity<RsData<EventListProjection>>` | 200 | O |
| GET | `/api/v1/events/manager/me` | `ResponseEntity<RsData<Page<EventListProjection>>>` | 200 | O |
| POST | `/api/v1/events/image/url` | `ResponseEntity<RsData<List<PresignedUrlResponse>>>` | 200 | O |
| PUT | `/static/events/images/{fileName}` | `ResponseEntity<RsData<String>>` | 200, 400, 500 | O |
| GET | `/api/v1/categories` | `ResponseEntity<RsData<List<EventCategoryListResponse>>>` | 200 | O |
| POST | `/api/v1/test/cache-invalidation` | `ResponseEntity<String>` | 200 | X |
| POST | `/api/v1/batch/viewcount-sync` | `ResponseEntity<RsData<String>>` | 200, 500 | O |
| GET | `/internal/events/{eventId}/summary` | `ResponseEntity<EventSummaryResponse>` | 200 | X |

## Seat

| Method | Path | Return type | Status code(s) | RsData 응답 |
| --- | --- | --- | --- | --- |
| GET | `/api/v1/events/{event-id}/seats` | `ResponseEntity<RsData<SeatLayoutResponse>>` | 200 | O |
| POST | `/api/v1/events/{event-id}/seats` | `ResponseEntity<RsData<SeatSelectResponse>>` | 200 | O |
| DELETE | `/api/v1/events/{event-id}/seats` | `ResponseEntity<RsData<Void>>` | 200 | O |
| GET | `/internal/seat-layouts/{layout-id}` | `ResponseEntity<SeatLayoutResponse>` | 200 | X |

## Purchase

| Method | Path | Return type | Status code(s) | RsData 응답 |
| --- | --- | --- | --- | --- |
| POST | `/api/v1/payments/init` | `ResponseEntity<RsData<InitiatePaymentResponse>>` | 200 | O |
| POST | `/api/v1/payments/confirm` | `ResponseEntity<RsData<ConfirmPaymentResponse>>` | 200 | O |
| POST | `/api/v1/payments/{paymentKey}/cancel` | `ResponseEntity<RsData<CancelPaymentResponse>>` | 200 | O |
| GET | `/api/v1/purchases/history` | `ResponseEntity<RsData<Page<PurchaseListProjection>>>` | 200 | O |
| GET | `/api/v1/purchases/event/{eventId}` | `ResponseEntity<RsData<Page<PurchaseListProjection>>>` | 200 | O |
| GET | `/api/v1/refunds/my` | `ResponseEntity<Page<RefundQueryService.RefundDto>>` | 200 | X |
| GET | `/api/v1/refunds/{refundId}` | `ResponseEntity<RefundQueryService.RefundDto>` | 200 | X |
| POST | `/api/v1/refunds/manager/single` | `ResponseEntity<ManagerRefundService.ManagerRefundResult>` | 200 | X |
| POST | `/api/v1/refunds/manager/batch` | `ResponseEntity<List<ManagerRefundService.ManagerRefundResult>>` | 200 | X |
| GET | `/api/v1/refunds/admin/by-status` | `ResponseEntity<List<RefundQueryService.RefundDto>>` | 200 | X |
| GET | `/api/test/purchase/original/{eventId}` | `String` | 200 | X |
| GET | `/api/test/purchase/optimized/{eventId}` | `String` | 200 | X |
| GET | `/api/test/purchase/compare/{eventId}` | `String` | 200 | X |
| GET | `/api/test/purchase/events` | `String` | 200 | X |

## Notification

| Method | Path | Return type | Status code(s) | RsData 응답 |
| --- | --- | --- | --- | --- |
| GET | `/api/v1/notifications` | `RsData<Page<NotificationListProjection>>` | 200 | O |
| GET | `/api/v1/notifications/{id}` | `ResponseEntity<RsData<NotificationDto>>` | 200 | O |
| POST | `/api/v1/notifications` | `ResponseEntity<RsData<NotificationDto>>` | 200 | O |
| GET | `/api/v1/notifications/unread` | `RsData<Page<NotificationListProjection>>` | 200 | O |
| GET | `/api/v1/notifications/subscribe` | `SseEmitter` | 200 | X |
| DELETE | `/api/v1/notifications/{id}` | `ResponseEntity<RsData<Void>>` | 200 | O |
| POST | `/api/v1/notifications/batch-delete` | `ResponseEntity<RsData<Void>>` | 200 | O |
| DELETE | `/api/v1/notifications/all` | `ResponseEntity<RsData<Void>>` | 200 | O |
| GET | `/api/v1/notifications/count/unread` | `ResponseEntity<RsData<Long>>` | 200 | O |
| POST | `/api/v1/notifications/test` | `ResponseEntity<RsData<NotificationDto>>` | 200 | O |

## Broker

| Method | Path | Return type | Status code(s) | RsData 응답 |
| --- | --- | --- | --- | --- |
| GET | `/api/v1/broker/events/{id}/tickets/waiting` | `SseEmitter` | 200 | X |
| POST | `/api/v1/broker/events/{id}/tickets/disconnect` | `ResponseEntity<Void>` | 200 | X |
| GET | `/api/v1/monitoring/threadpool` | `ResponseEntity<Map<String, Object>>` | 200 | X |
| GET | `/api/v1/monitoring/threadpool/summary` | `ResponseEntity<Map<String, Object>>` | 200 | X |

## App

| Method | Path | Return type | Status code(s) | RsData 응답 |
| --- | --- | --- | --- | --- |
| GET | `/static/events/images/{fileName}` | `ResponseEntity<Resource>` | 200, 404, 500 | X |

---

## 예외 처리 점검 결과

**RsData 규칙 위반 (성공 응답 기준)**
- `auth/src/main/java/org/codenbug/auth/ui/SecurityController.java`의 소셜 로그인 요청(`GET /api/v1/auth/social/{socialLoginType}`)은 `ResponseEntity<String>` 반환.
- `broker/src/main/java/org/codenbug/broker/ui/WaitingQueueController.java`의 SSE, `ResponseEntity<Void>` 응답은 RsData 형식이 아님.
- `broker/src/main/java/org/codenbug/broker/ui/MonitoringController.java`의 모니터링 응답은 `Map` 반환.
- `event/src/main/java/org/codenbug/event/ui/EventInternalController.java` 내부 API는 RsData 미사용.
- `event/src/main/java/org/codenbug/event/ui/CacheTestController.java` 테스트 API는 String 반환.
- `app/src/main/java/org/codenbug/app/ui/StaticController.java` 정적 파일 응답은 RsData 미사용.
- `seat/src/main/java/org/codenbug/seat/ui/SeatInternalController.java` 내부 API는 RsData 미사용.
- `purchase/src/main/java/org/codenbug/purchase/ui/RefundController.java` 전체(환불 관련) 응답은 RsData 미사용.
- `purchase/src/main/java/org/codenbug/purchase/ui/PurchaseTestController.java` 테스트 API는 String 반환.
- `notification/src/main/java/org/codenbug/notification/controller/NotificationController.java`의 SSE 구독(`GET /api/v1/notifications/subscribe`)은 RsData 미사용.

**예외 처리 RsData 미준수**
- `auth/src/main/java/org/codenbug/auth/global/GlobalExceptionHandler.java`는 `ResponseEntity<String>`로 예외 응답을 반환.
- `broker/src/main/java/org/codenbug/broker/infra/GlobalExceptionHandler.java`는 `ResponseEntity<String>`로 예외 응답을 반환하며 `e.printStackTrace()` 호출 포함.
- `event/src/main/java/org/codenbug/event/ui/GlobalExceptionHandler.java`는 검증 예외만 RsData로 처리하고, 그 외 예외는 Spring 기본 오류 응답(비 RsData)로 내려갈 가능성이 있음.
- `user/`, `seat/`, `purchase/`, `notification/`, `app/` 모듈에는 RsData 기반의 전역 예외 처리기가 없어, 런타임 예외 시 기본 오류 응답(비 RsData)으로 내려갈 가능성이 있음.

**스택트레이스 응답 여부**
- 응답 바디에 스택트레이스를 직접 포함하는 구현은 확인되지 않음.
- 다만 `broker` 및 `app` 일부 코드에서 `printStackTrace()`가 존재하므로, 로그/콘솔 출력은 발생함.
- `server.error.include-stacktrace` 설정이 명시되지 않아 기본값(never)에 의존함. 설정이 변경되면 기본 오류 응답에서 스택트레이스가 노출될 수 있음.

