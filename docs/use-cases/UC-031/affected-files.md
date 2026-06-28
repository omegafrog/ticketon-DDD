# UC-031 Affected Files

## Expected Files

- `notification/src/main/java/org/codenbug/notification/controller/NotificationCommandController.java`
- `notification/src/main/java/org/codenbug/notification/controller/NotificationDeleteRequestDto.java`
- `notification/src/main/java/org/codenbug/notification/application/service/NotificationCommandService.java`
- `notification/src/main/java/org/codenbug/notification/application/port/NotificationStore.java`
- `notification/src/main/java/org/codenbug/notification/domain/entity/Notification.java`
- `notification/src/main/java/org/codenbug/notification/domain/entity/UserId.java`
- `notification/src/main/java/org/codenbug/notification/domain/service/NotificationDomainService.java`
- `notification/src/main/java/org/codenbug/notification/infrastructure/NotificationStoreAdapter.java`
- `notification/src/main/java/org/codenbug/notification/infrastructure/NotificationRepository.java`
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
