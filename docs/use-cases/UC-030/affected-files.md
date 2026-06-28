# UC-030 Affected Files

## Expected Files

- `notification/src/main/java/org/codenbug/notification/controller/NotificationQueryController.java`
- `notification/src/main/java/org/codenbug/notification/application/service/NotificationQueryService.java`
- `notification/src/main/java/org/codenbug/notification/application/port/NotificationStore.java`
- `notification/src/main/java/org/codenbug/notification/domain/entity/Notification.java`
- `notification/src/main/java/org/codenbug/notification/domain/entity/NotificationContent.java`
- `notification/src/main/java/org/codenbug/notification/domain/entity/UserId.java`
- `notification/src/main/java/org/codenbug/notification/domain/service/NotificationDomainService.java`
- `notification/src/main/java/org/codenbug/notification/infrastructure/NotificationStoreAdapter.java`
- `notification/src/main/java/org/codenbug/notification/infrastructure/NotificationRepository.java`
- `notification/src/main/java/org/codenbug/notification/infrastructure/event/PurchaseEventListener.java`
- `notification/src/main/java/org/codenbug/notification/infrastructure/messaging/PurchaseNotificationEventListener.java`
- `notification/src/main/java/org/codenbug/notification/ui/projection/NotificationListProjection.java`
- `notification/src/main/java/org/codenbug/notification/ui/repository/NotificationViewRepository.java`
- `notification/src/main/java/org/codenbug/notification/ui/repository/NotificationViewRepositoryImpl.java`
- `notification/build.gradle`
- `scripts/run-app-infra.sh`
- `scripts/check-app-infra.sh`
- `scripts/run-app-server.sh`

## Test Targets

- `notification/src/test/java/org/codenbug/notification/application/service/NotificationApplicationServicePortTest.java`
- `notification/src/test/java/org/codenbug/notification/domain/service/NotificationDomainServiceTest.java`
- `notification/src/test/java/org/codenbug/notification/**`

## Forbidden Files

- `app/**`
- `platform/**`
- `auth/**`
- `purchase/**`
- `seat/**`
- `event/**`
- `dispatcher/**`
- `broker/**`
- `user/**`
- `**/application-secret.yml`
