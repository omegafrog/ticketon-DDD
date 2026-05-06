# Implementation Plan

## 1. 구현 목표
- 실제 Spring Boot main class가 있는 모든 서버가 로컬 인프라 기동 후 정상적으로 bootRun되고 health check에 성공하는 상태를 유지한다.
- 2026-04-30 완료된 bootRun 복구 범위를 보존한다.
  - `:app:bootRun`: `RefundController`와 `RefundCommandController`의 `POST /api/v1/refunds/manager/single` 중복 매핑 제거 완료.
  - `:auth:bootRun`: `UserRegisteredEventConsumer`가 구독하는 RabbitMQ `user-created` 큐 미선언 문제 해결 완료.
- 남은 최종 검증 실패인 `notification` 모듈 Semgrep DDD 위반 3건을 명시적 다음 구현 범위로 포함해 static analysis가 통과하도록 수정한다.
  - `NotificationCommandService`와 `NotificationQueryService`가 `notification.infrastructure.NotificationRepository`를 직접 의존하지 않게 한다.
  - `NotificationDomainService`가 Spring stereotype 없이 순수 domain service로 남도록 한다.

## 2. 구현하지 말아야 할 것
- 신규 endpoint, 신규 유스케이스, 신규 비즈니스 기능을 추가하지 않는다.
- 알림 생성, 조회, 읽음/삭제, SSE 발송, RabbitMQ 이벤트 수신의 사용자-visible 동작을 변경하지 않는다.
- notification 저장 스키마, API 경로, request/response DTO 의미를 변경하지 않는다.
- notification 외 feature module의 구조를 리팩터링하지 않는다.
- Docker image, compose topology, 외부 인프라 구성을 변경하지 않는다.
- `app` 모듈에 비즈니스 로직, 컨트롤러, repository, infrastructure adapter를 추가하지 않는다.
- 기존 purchase 결제 승인 멱등성/worker 리팩터링 구현 범위를 변경하지 않는다.

## 3. 입력 문서
|문서|사용 목적|상태|
|---|---|---|
|`ARCHITECTURE.md`|모듈 책임, 레이어링, 런타임 검증 제약 확인|확인|
|`docs/design/요구사항.md`|기능/비기능 범위 확인|확인|
|`docs/design/유스케이스.md`|기존 유스케이스 범위 확인|확인|
|`docs/design/이벤트 스토밍.md`|이벤트/메시징 맥락 확인|확인|
|`docs/design/details/index.md`|DDD 세부 문서 인덱스 확인|확인|
|`docs/design/details/도메인모델.md`|도메인 모델 변경 범위 확인|확인|
|`docs/design/details/어그리거트.md`|어그리거트 변경 비대상 확인|확인|
|`docs/design/details/애플리케이션서비스.md`|application service는 orchestration과 port 의존만 가져야 함을 확인|확인|
|`docs/design/details/바운디드컨텍스트.md`|notification이 현재 주요 BC 밖 기존 모듈이나, 이번 plan에서 static-analysis cleanup 범위로 재도입됨을 명시|확인|
|`docs/design/기술결정.md`|RabbitMQ projection, 메시징 멱등 처리, 한글 테스트명, 런타임 전제 확인|확인|

## 4. 아키텍처 제약
- ARCHITECTURE.md 기준:
  - `app`은 orchestrator Spring Boot app과 cross-service configuration import만 담당한다.
  - `purchase`는 Booking/Payment BC이며 환불 API/controller는 기존 `ui` 레이어에 둔다.
  - `auth`는 Auth/Authz BC이며 RabbitMQ 소비자와 설정은 기존 auth 모듈 안에서 처리한다.
  - `notification`은 기존 notification module이며, 이번 계획에서는 새 기능 추가 없이 static-analysis cleanup 대상으로만 다룬다.
- 모듈/패키지 경계:
  - 완료된 중복 환불 route 수정은 `purchase/src/main/java/org/codenbug/purchase/ui`의 controller wiring에 한정한다.
  - 완료된 RabbitMQ 큐 선언 수정은 `auth/src/main/java/org/codenbug/auth/config`와 관련 consumer wiring에 한정한다.
  - notification remediation은 `notification` 모듈 내부 application/domain/infrastructure 경계 정리에 한정한다.
- 의존성 방향:
  - controller는 application service를 호출하고 business rule은 application/domain에 남긴다.
  - application service는 domain과 outbound port/domain contract에 의존하고 infrastructure 구현체에 직접 의존하지 않는다.
  - infrastructure repository 또는 adapter는 application/domain port를 구현한다.
  - domain service는 Spring `@Service`, Spring MVC, JPA repository, Redis, RabbitMQ, HTTP client에 의존하지 않는다.
- 금지 참조:
  - 다른 feature module의 `domain`, `infra`, `infrastructure`, `ui`, `presentation` 내부 패키지를 직접 참조하지 않는다.
  - `app` 모듈에 purchase/auth/notification 실패를 우회하는 bean override나 scan 제외 로직을 추가하지 않는다.
  - notification application service에서 `org.codenbug.notification.infrastructure.NotificationRepository`를 import하지 않는다.
  - notification domain service에 Spring stereotype annotation을 두지 않는다.

## 5. 구현 범위
- 포함:
  - 완료된 `RefundController`/`RefundCommandController` 중복 매핑 제거와 테스트/런타임 검증 증거를 유지한다.
  - 완료된 auth RabbitMQ `user-created`, `user-created-failed` queue/binding 선언과 테스트/런타임 검증 증거를 유지한다.
  - `NotificationCommandService`와 `NotificationQueryService`가 infrastructure repository 대신 notification application/domain port를 주입받도록 정리한다.
  - 기존 `NotificationRepository` 또는 새 infrastructure adapter가 그 port를 구현하도록 연결한다.
  - `NotificationDomainService`에서 Spring stereotype을 제거하고, 필요하면 application/infrastructure configuration 또는 application service 생성자 wiring을 통해 사용 가능하게 한다.
  - notification 동작이 바뀌지 않았음을 확인하는 focused test를 추가 또는 보강한다.
  - 최종적으로 build, tests, 여섯 개 실제 runnable server bootRun/health, ArchUnit, Semgrep을 모두 검증한다.
- 제외:
  - notification 기능 정책 변경.
  - notification API/DTO/DB schema 변경.
  - SSE/RabbitMQ delivery semantics 변경.
  - notification 모듈 전체 package 재설계.
  - 이번 Semgrep 3건과 무관한 legacy `notification.service` 계층 정리.
- 가정:
  - 로컬 인프라는 `docker compose -f docker/docker-compose.yml up -d`로 MySQL master/replica, Redis/cache/polling, RabbitMQ를 기동한다.
  - `broker`는 현재 구성상 random port로 뜰 수 있으므로 로그에서 실제 port를 확인해 `/actuator/health`를 호출한다.
  - notification cleanup은 architecture/static-analysis 목적의 구조 변경이며, 런타임 표면은 기존 서버 전체 boot/health와 tests로 검증한다.

## 5.1 승인된 기술 결정
|영역|결정|구현 반영|테스트/검증 반영|
|---|---|---|---|
|Java/Spring/Gradle|Java 21, Spring Boot, Gradle multi-module|기존 모듈 안에서 최소 수정|`./gradlew build`, `./gradlew test`|
|인프라 전제|Gateway, Eureka, MySQL, Redis, RabbitMQ 전제|RabbitMQ 큐 선언은 애플리케이션 설정으로 보강 완료|docker compose 후 auth bootRun 검증 완료 및 최종 재검증|
|레이어링|Application은 orchestration/port 의존, infrastructure 직접 결합 금지|notification application service가 repository port에 의존하도록 변경|Semgrep violation 제거, application service focused test|
|Domain purity|Domain은 Spring/JPA/RabbitMQ 등 framework 의존 금지|`NotificationDomainService`에서 Spring stereotype 제거|reflection 또는 ArchUnit/Semgrep으로 stereotype 부재 검증|
|BC 간 통신|일반 BC 간 조회는 RabbitMQ projection 사용|auth consumer 큐 이름/선언 일관성 보장 완료, notification messaging 의미 변경 금지|큐 bean/바인딩 테스트와 runtime 재검증|
|Outbox/Inbox/메시징|메시지는 중복 허용, 소비자는 멱등 처리|이번 범위는 구조/lint cleanup이며 소비 로직 의미는 변경하지 않음|consumer 의미 변경 없이 context/wiring 검증|
|테스트 전략|계약 테스트는 unit, 통합은 happy path 중심|notification port 사용과 domain service 순수성을 focused test로 검증|신규/수정 테스트 메서드는 한글 문장형 이름 사용|
|런타임 검증|서버는 debug attach와 health endpoint로 확인|여섯 개 bootRun 대상 전체 기동 확인|Eureka 선기동 후 각 서버 health 확인 및 종료|

## 6. 구현 계획
- [x] `spring-package-structure` 관점으로 현재 모듈/패키지 구조와 `ARCHITECTURE.md`가 bootRun 복구 범위에 충분히 일치하는지 확인한다. 신규 Spring Boot 프로젝트나 신규 모듈은 만들지 않는다.
- [x] purchase 환불 controller 구조를 점검해 `RefundController`와 `RefundCommandController` 중 어느 controller가 command endpoint를 소유할지 결정하고, query endpoint와 command endpoint의 책임이 겹치지 않게 중복 매핑을 제거한다.
- [x] `POST /api/v1/refunds/manager/single`과 `POST /api/v1/refunds/manager/batch`의 기존 경로, request DTO, response status/body 의미가 유지되는지 확인하며 controller wiring만 수정한다.
- [x] purchase 쪽 focused MVC/context 테스트를 추가 또는 보강해 환불 controller mapping이 충돌 없이 등록되고 단일/일괄 환불 command endpoint가 하나의 handler에만 매핑됨을 검증한다. 신규/수정 테스트 메서드명은 한글 문장형으로 작성한다.
- [x] auth RabbitMQ 설정과 consumer를 점검해 `UserRegisteredEventConsumer`가 구독하는 `user-created` 큐를 기존 exchange/routing/queue naming 패턴에 맞게 선언하거나 listener queue 이름을 선언된 큐와 일치시킨다.
- [x] `user-created-failed`, `sns-user-created`, `security-user-created` 등 기존 큐 선언과 충돌하지 않도록 bean 이름, 큐 이름, binding 이름을 명확히 유지한다.
- [x] auth 쪽 focused wiring/communication 테스트를 추가 또는 보강해 `user-created` 큐가 Spring context에 선언되고 Rabbit listener startup에서 누락 큐로 실패하지 않음을 검증한다. 신규/수정 테스트 메서드명은 한글 문장형으로 작성한다.
- [x] focused 검증으로 `./gradlew :purchase:test :auth:test --no-daemon --console=plain` 또는 동등한 모듈 테스트 명령을 실행하고 실패 시 원인을 수정한다.
- [ ] notification application service가 필요로 하는 저장/조회 연산을 표현하는 port 또는 domain contract를 도입하거나 기존 적절한 contract가 있으면 사용한다. Port는 application/domain 쪽에 두고 infrastructure 패키지에 의존하지 않는다.
- [ ] `NotificationCommandService`가 `NotificationRepository` infrastructure type을 직접 import하지 않고 port/interface에만 의존하도록 변경한다. 알림 생성/삭제/상태 변경의 기존 동작은 유지한다.
- [ ] `NotificationQueryService`가 `NotificationRepository` infrastructure type을 직접 import하지 않고 port/interface에만 의존하도록 변경한다. 조회 결과와 정렬/필터링 의미는 유지한다.
- [ ] 기존 `notification.infrastructure.NotificationRepository`가 새 port를 직접 구현할 수 있으면 그렇게 하고, Spring Data method signature 때문에 adapter가 더 안전하면 infrastructure adapter를 추가해 repository를 감싼다.
- [ ] `NotificationDomainService`에서 Spring `@Service` 및 Spring import를 제거한다. Bean 주입이 필요하면 application/infrastructure configuration에서 명시적으로 생성하거나 application service가 순수 객체로 사용하도록 한다.
- [ ] notification focused test를 추가 또는 보강한다. 테스트 메서드명은 한글 문장형으로 작성하고, application service가 port를 통해 저장/조회 협력을 수행하며 domain service에 Spring stereotype이 없음을 검증한다.
- [ ] notification focused 검증으로 `./gradlew :notification:test --no-daemon --console=plain` 또는 repository에서 가능한 동등한 테스트 명령을 실행하고 실패 시 원인을 수정한다.
- [ ] 최종 검증으로 build, 전체 test, runtime server verification, static analysis를 실행하고 결과를 `10. 검증 결과`에 기록한다.

## 7. 테스트 계획
- [ ] Domain/Aggregate/VO 테스트:
  - notification domain entity/VO 규칙 자체를 변경하지 않는다면 신규 invariant 테스트는 추가하지 않는다.
  - `NotificationDomainService`가 Spring stereotype annotation을 갖지 않는지 reflection 또는 architecture test로 검증한다. 테스트 메서드명은 예: `도메인_서비스는_스프링_스테레오타입을_가지지_않는다`.
- [ ] Application Service 흐름 테스트:
  - `NotificationCommandService`가 repository port를 통해 저장/삭제/상태 변경 협력을 수행하는지 fake 또는 mock port로 검증한다.
  - `NotificationQueryService`가 repository port를 통해 조회 협력을 수행하는지 fake 또는 mock port로 검증한다.
  - 테스트는 notification behavior를 바꾸지 않았음을 확인하고 infrastructure repository class를 직접 요구하지 않도록 한다.
- [ ] Infrastructure/Adapter 테스트:
  - `NotificationRepository`가 port를 직접 구현하는 경우 Spring Data repository wiring이 깨지지 않는지 기존 context/build/test로 확인한다.
  - 별도 adapter를 추가하는 경우 adapter가 Spring Data repository에 위임하는지 focused test를 추가한다.
- [ ] Communication/Transaction 테스트:
  - 이번 notification remediation은 메시징 의미를 변경하지 않는다.
  - listener 또는 RabbitMQ 설정을 건드리는 경우에만 기존 listener/context test를 보강한다.
- [x] Presentation/Mapping 테스트:
  - purchase controller mapping을 검증해 `/api/v1/refunds/manager/single`, `/api/v1/refunds/manager/batch`가 중복 handler 없이 등록되는지 확인 완료.
  - 테스트 메서드명은 한글 문장형으로 작성 완료.

## 8. 검증 방법
- [ ] Build:
  - 명령: `./gradlew build --no-daemon --console=plain`
  - 성공 기준: 모든 모듈 build 성공.
- [ ] Tests:
  - focused 명령:
    - `./gradlew :purchase:test :auth:test --no-daemon --console=plain`
    - `./gradlew :notification:test --no-daemon --console=plain` 또는 notification 모듈 테스트가 별도 task로 없으면 `./gradlew test --tests '*Notification*' --no-daemon --console=plain`
  - 전체 명령: `./gradlew test --no-daemon --console=plain`
  - 성공 기준: 신규/수정 테스트와 기존 전체 테스트 통과.
- [ ] Runtime server verification:
  - 인프라 실행 명령: `docker compose -f docker/docker-compose.yml up -d`
  - 서버 실행 순서:
    - `./gradlew :platform:eureka:bootRun --no-daemon --console=plain`
    - `./gradlew :app:bootRun --no-daemon --console=plain`
    - `./gradlew :auth:bootRun --no-daemon --console=plain`
    - `./gradlew :broker:bootRun --no-daemon --console=plain`
    - `./gradlew :dispatcher:bootRun --no-daemon --console=plain`
    - `./gradlew :platform:gateway:bootRun --no-daemon --console=plain`
  - 구현사항 확인 방법:
    - Eureka를 먼저 기동하고 `http://127.0.0.1:8761/actuator/health`가 `UP`인지 확인한다.
    - App은 `http://127.0.0.1:9000/actuator/health`가 `UP`인지 확인하고, Spring MVC ambiguous mapping 예외가 없는지 로그로 확인한다.
    - Auth는 `http://127.0.0.1:9001/actuator/health`가 `UP`인지 확인하고, RabbitMQ `user-created` queue missing 또는 listener startup 실패가 없는지 로그로 확인한다.
    - Broker는 로그에서 실제 random port를 확인한 뒤 `http://127.0.0.1:<broker-port>/actuator/health`가 `UP`인지 확인한다.
    - Dispatcher는 `http://127.0.0.1:9002/actuator/health`가 `UP`인지 확인한다.
    - Gateway는 `http://127.0.0.1:8080/actuator/health`가 `UP`인지 확인한다.
    - 각 서버는 검증 후 종료한다. 로그는 `/tmp` 등 임시 위치에 저장하고 secret은 출력하지 않는다.
  - 성공 기준: 여섯 개 실제 runnable server가 모두 bootRun 상태에서 readiness/health 확인에 성공하고, 검증 후 프로세스가 정리된다.
- [ ] Static analysis:
  - 절차: 기존 repository static-analysis 절차를 그대로 사용한다.
  - 명령:
    - `./gradlew architectureRules --no-daemon --console=plain`
    - `TMPDIR=/mnt/e/workspace/ticketon-ddd/.tmp SEMGREP_SEND_METRICS=off semgrep --config .semgrep/ddd-architecture.yml .`
  - 성공 기준:
    - ArchUnit/architectureRules가 violation 없이 종료한다.
    - Semgrep이 기존 notification 3건을 포함해 repository DDD violation 없이 종료한다.
    - Semgrep이 설치되어 있지 않으면 환경 blocker로 기록하고 코드 remediation task를 추가하지 않는다.

## 9. 완료 조건
- 모든 체크박스가 `- [x]` 상태다.
- 구현 범위의 테스트가 작성되어 통과했다.
- Build, Tests, Runtime server verification, Static analysis가 성공했다.
- `10. 검증 결과`에 성공한 명령과 런타임 health 확인 결과가 기록되어 있다.
- 성공 후 `docs/plans/complete/plan.md`로 이동한다.

## 10. 검증 결과
- Build:
  - 2026-04-30 `./gradlew build --no-daemon --console=plain` 성공.
  - notification remediation 이후 재실행 필요.
- Tests:
  - `./gradlew :purchase:test --tests org.codenbug.purchase.ui.RefundControllerMappingTest :auth:test --tests org.codenbug.auth.config.RabbitMqConfigTest --no-daemon --console=plain` 성공.
  - `./gradlew :purchase:test :auth:test --no-daemon --console=plain` 성공. 첫 실행은 `RefundControllerMappingTest`의 테스트 전용 WebMvc 설정이 purchase 통합 테스트 컨텍스트에 스캔되어 실패했고, 컨텍스트 없는 reflection 기반 mapping 검증으로 수정한 뒤 재실행 성공.
  - 2026-04-30 `./gradlew test --no-daemon --console=plain` 성공.
  - notification remediation 이후 notification focused test와 전체 test 재실행 필요.
- Runtime server verification:
  - 2026-04-30 `docker compose -f docker/docker-compose.yml up -d`로 MySQL master/replica, Redis/cache/polling, RabbitMQ 기동 확인.
  - `./gradlew :platform:eureka:bootRun --no-daemon --console=plain` 성공, `http://127.0.0.1:8761/actuator/health` => `{"status":"UP"}`.
  - `./gradlew :app:bootRun --no-daemon --console=plain` 성공, `http://127.0.0.1:9000/actuator/health` => `{"status":"UP"}`. 기존 Spring MVC ambiguous mapping 예외 없음.
  - `./gradlew :auth:bootRun --no-daemon --console=plain` 성공, `http://127.0.0.1:9001/actuator/health` => `{"status":"UP"}`. 기존 RabbitMQ `user-created` missing queue/listener startup 예외 없음.
  - `./gradlew :broker:bootRun --no-daemon --console=plain` 성공, 로그상 random port `41931`, `http://127.0.0.1:41931/actuator/health` => `UP`.
  - `./gradlew :dispatcher:bootRun --no-daemon --console=plain` 성공, `http://127.0.0.1:9002/actuator/health` => `{"status":"UP"}`.
  - `./gradlew :platform:gateway:bootRun --no-daemon --console=plain` 성공, `http://127.0.0.1:8080/actuator/health` => `{"status":"UP"}`.
  - 검증 후 bootRun 프로세스를 종료했고, 주요 포트 health 호출이 connection refused로 바뀐 것을 확인했다.
  - notification remediation 이후 여섯 서버 runtime verification 재실행 필요.
- Static analysis:
  - 2026-04-30 `./gradlew architectureRules --no-daemon --console=plain` 성공.
  - 2026-04-30 `TMPDIR=/mnt/e/workspace/ticketon-ddd/.tmp SEMGREP_SEND_METRICS=off semgrep --config .semgrep/ddd-architecture.yml .` 실행은 완료됐으나, `notification` 모듈의 DDD 위반 3건으로 실패.
  - notification remediation 이후 Semgrep 재실행 필요.

## 11. 검증 실패
- Static analysis:
  - 실패 명령: `TMPDIR=/mnt/e/workspace/ticketon-ddd/.tmp SEMGREP_SEND_METRICS=off semgrep --config .semgrep/ddd-architecture.yml .`
  - exit result: 1
  - 실패 증거:
    - `notification/src/main/java/org/codenbug/notification/application/service/NotificationCommandService.java:11`가 `notification.infrastructure.NotificationRepository`를 직접 import.
    - `notification/src/main/java/org/codenbug/notification/application/service/NotificationQueryService.java:8`가 `notification.infrastructure.NotificationRepository`를 직접 import.
    - `notification/src/main/java/org/codenbug/notification/domain/service/NotificationDomainService.java:9`가 domain service에 `@Service`를 사용.
  - 분류: 사용자 요청에 따라 이번 active plan의 명시적 implementation-level remediation 범위로 전환했다. `6. 구현 계획`의 notification 미완료 체크박스를 executor가 수행한 뒤 Semgrep을 재실행한다.
