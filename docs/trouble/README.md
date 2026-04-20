# Trouble

이 디렉터리는 `trouble` 관점의 엔드포인트별 문서를 모은다.

## App
### StaticController
- [GET /static/events/images/{fileName}](./app/StaticController/getEventImage.md)

## Auth
### SecurityController
- [GET /api/v1/auth/logout](./auth/SecurityController/logout.md)
- [GET /api/v1/auth/social/{socialLoginType}](./auth/SecurityController/request.md)
- [GET /api/v1/auth/social/{socialLoginType}/callback](./auth/SecurityController/callback.md)
- [POST /api/v1/auth/login](./auth/SecurityController/login.md)
- [POST /api/v1/auth/refresh](./auth/SecurityController/refreshTokens.md)
- [POST /api/v1/auth/register](./auth/SecurityController/register.md)

## Broker
### MonitoringController
- [GET /api/v1/monitoring/threadpool](./broker/MonitoringController/getThreadPoolStatus.md)
- [GET /api/v1/monitoring/threadpool/summary](./broker/MonitoringController/getThreadPoolSummary.md)
### PollingWaitingQueueController
- [DELETE /api/v1/broker/polling/events/{id}/waiting](./broker/PollingWaitingQueueController/disconnectWaiting.md)
- [GET /api/v1/broker/polling/events/{id}/current](./broker/PollingWaitingQueueController/parseWaitingOrder.md)
- [GET /api/v1/broker/polling/events/{id}/waiting](./broker/PollingWaitingQueueController/enterWaiting.md)
### WaitingQueueController
- [GET /api/v1/broker/events/{id}/tickets/waiting](./broker/WaitingQueueController/entryWaiting.md)
- [POST /api/v1/broker/events/{id}/tickets/disconnect](./broker/WaitingQueueController/disconnectFromQueue.md)

## Event
### CacheTestController
- [POST /api/v1/test/cache-invalidation](./event/CacheTestController/testCacheInvalidation.md)
### EventCategoryController
- [GET /api/v1/categories](./event/EventCategoryController/getAllCategories.md)
### EventCommandController
- [DELETE /api/v1/events/{eventId}](./event/EventCommandController/deleteEvent.md)
- [PATCH /api/v1/events/{eventId}](./event/EventCommandController/changeStatus.md)
- [POST /api/v1/events](./event/EventCommandController/eventRegister.md)
- [PUT /api/v1/events/{eventId}](./event/EventCommandController/updateEvent.md)
### EventInternalController
- [GET /internal/events/{eventId}/summary](./event/EventInternalController/getEventSummary.md)
- [GET /internal/events/{eventId}/version-check](./event/EventInternalController/validateEventVersion.md)
### EventQueryController
- [GET /api/v1/events/manager/me](./event/EventQueryController/getManagerEvents.md)
- [GET /api/v1/events/{id}](./event/EventQueryController/getEvent.md)
- [POST /api/v1/events/list](./event/EventQueryController/getEvents.md)
### FileUploadController
- [PUT /static/events/images/{fileName}](./event/FileUploadController/uploadFile.md)
### ImageUploadController
- [POST /api/v1/events/image/url](./event/ImageUploadController/generateImageUploadUrls.md)
### ViewCountBatchController
- [POST /api/v1/batch/viewcount-sync](./event/ViewCountBatchController/runViewCountSync.md)

## Notification
### NotificationCommandController
- [DELETE /api/v1/notifications/all](./notification/NotificationCommandController/deleteAllNotifications.md)
- [DELETE /api/v1/notifications/{id}](./notification/NotificationCommandController/deleteNotification.md)
- [POST /api/v1/notifications](./notification/NotificationCommandController/createNotification.md)
- [POST /api/v1/notifications/batch-delete](./notification/NotificationCommandController/batchDeleteNotifications.md)
- [POST /api/v1/notifications/test](./notification/NotificationCommandController/createTestNotification.md)
### NotificationQueryController
- [GET /api/v1/notifications](./notification/NotificationQueryController/getNotifications.md)
- [GET /api/v1/notifications/count/unread](./notification/NotificationQueryController/getUnreadCount.md)
- [GET /api/v1/notifications/subscribe](./notification/NotificationQueryController/subscribeNotifications.md)
- [GET /api/v1/notifications/unread](./notification/NotificationQueryController/getUnreadNotifications.md)
- [GET /api/v1/notifications/{id}](./notification/NotificationQueryController/getNotificationDetail.md)

## Purchase
### PurchaseCommandController
- [POST /api/v1/payments/confirm](./purchase/PurchaseCommandController/confirmPayment.md)
- [POST /api/v1/payments/init](./purchase/PurchaseCommandController/initiatePayment.md)
- [POST /api/v1/payments/{paymentKey}/cancel](./purchase/PurchaseCommandController/cancelPayment.md)
### PurchaseConfirmQueryController
- [GET /api/v1/payments/confirm/{purchaseId}/status](./purchase/PurchaseConfirmQueryController/getConfirmStatus.md)
### PurchaseQueryController
- [GET /api/v1/purchases/event/{eventId}](./purchase/PurchaseQueryController/getEventPurchases.md)
- [GET /api/v1/purchases/history](./purchase/PurchaseQueryController/getPurchaseHistory.md)
### PurchaseTestController
- [GET /api/test/purchase/compare/{eventId}](./purchase/PurchaseTestController/compareQueries.md)
- [GET /api/test/purchase/events](./purchase/PurchaseTestController/getTestEvents.md)
- [GET /api/test/purchase/optimized/{eventId}](./purchase/PurchaseTestController/testOptimizedQuery.md)
- [GET /api/test/purchase/original/{eventId}](./purchase/PurchaseTestController/testOriginalQuery.md)
### RefundCommandController
- [POST /api/v1/refunds/manager/batch](./purchase/RefundCommandController/processBatchRefund.md)
- [POST /api/v1/refunds/manager/single](./purchase/RefundCommandController/processManagerRefund.md)
### RefundController
- [GET /api/v1/refunds/admin/by-status](./purchase/RefundController/getRefundsByStatus.md)
- [GET /api/v1/refunds/my](./purchase/RefundController/getMyRefunds.md)
- [GET /api/v1/refunds/{refundId}](./purchase/RefundController/getRefundDetail.md)
- [POST /api/v1/refunds/manager/batch](./purchase/RefundController/processBatchRefund.md)
- [POST /api/v1/refunds/manager/single](./purchase/RefundController/processManagerRefund.md)
### RefundQueryController
- [GET /api/v1/refunds/admin/by-status](./purchase/RefundQueryController/getRefundsByStatus.md)
- [GET /api/v1/refunds/my](./purchase/RefundQueryController/getMyRefunds.md)
- [GET /api/v1/refunds/{refundId}](./purchase/RefundQueryController/getRefundDetail.md)

## Seat
### SeatCommandController
- [DELETE /api/v1/events/{event-id}/seats](./seat/SeatCommandController/cancelSeat.md)
- [POST /api/v1/events/{event-id}/seats](./seat/SeatCommandController/selectSeat.md)
### SeatInternalController
- [GET /internal/seat-layouts/{layout-id}](./seat/SeatInternalController/getSeatLayout.md)
### SeatQueryController
- [GET /api/v1/events/{event-id}/seats](./seat/SeatQueryController/getSeatLayout.md)

## User
### UserCommandController
- [PUT /api/v1/users/me](./user/UserCommandController/updateMe.md)
### UserQueryController
- [GET /api/v1/users/me](./user/UserQueryController/getMe.md)
### UserValidationController
- [POST /internal/users/validate](./user/UserValidationController/validateRegister.md)

## Special Deep Dives

- [Purchase Confirm Version Mismatch](./purchase-confirm-worker-version-mismatch.md)
- [Waiting Queue](./waiting-queue.md)
