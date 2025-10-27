현재 구성은 MSA(마이크로서비스 아키텍처)에서 데이터 정합성을 맞추기 위한 **사가 패턴(Saga Pattern)**을 올바르게 구현하고 계신 상황입니다. 제가 왜 이것이 정상적인 흐름인지, 그리고 이 상황을 어떻게 더 세련되게 처리할 수 있는지 설명해 드리겠습니다.

### 왜 동기적으로 알 수 없는가? (비동기 통신의 본질)

1.  **`UserRegisterService`의 책임**: 이 서비스의 책임은 **"사용자 등록 요청을 접수하고, User 엔티티를 생성한 뒤, '사용자가 등록되었다'는 사실을 다른 시스템에 알리는 것"**까지입니다. `kafkaTemplate.send()`를 호출하는 순간 `UserRegisterService`의 트랜잭션과 책임은 성공적으로 끝난 것입니다.
2.  **분리와 비동기**: Kafka를 중간에 둔다는 것은 `user-service`와 `auth-service`를 의도적으로 분리(decoupling)하여 서로의 상태에 영향을 받지 않도록 설계한 것입니다. 만약 `auth-service`가 잠시 장애가 나더라도 `user-service`는 계속해서 사용자 등록을 받을 수 있어야 합니다. 이것이 비동기 통신의 가장 큰 장점(탄력성, Resilience)입니다.
3.  **동기 호출의 단점**: 만약 `UserRegisterService`가 Kafka 대신 REST API 등으로 `AuthService`를 직접 호출(동기 호출)했다면, `AuthService`가 실패했을 때 즉시 알 수 있었을 겁니다. 하지만 이는 `auth-service`가 다운되면 `user-service`의 회원가입까지 실패하는 **강한 결합(tight coupling)**을 만들어 시스템 전체의 안정성을 해칩니다.

따라서 "동기적으로 실패를 알 수 없다"는 것은 문제가 아니라, **사가 패턴을 사용하는 비동기 아키텍처의 자연스러운 특징**입니다.

### 그렇다면 이 상황을 어떻게 처리해야 하는가? (상태 관리와 보상 트랜잭션)

현재 보상 트랜잭션(실패 시 User 삭제)까지는 잘 구현하셨습니다. 이제 남은 과제는 **최초 요청을 보낸 클라이언트(예: 웹 브라우저, 모바일 앱)와 시스템 내부의 최종적인 데이터 상태를 일치시키는 것**입니다.

가장 좋은 방법은 **`User` 엔티티에 회원가입 진행 상태(`status`)를 추가**하는 것입니다.

#### 단계별 해결 방안

**1. `User` 엔티티에 등록 상태(RegistrationStatus) 필드 추가**

회원가입의 현재 상태를 저장할 Enum과 필드를 `user` 도메인에 추가합니다.

```java
// 예: user/domain/RegistrationStatus.java
public enum RegistrationStatus {
    PENDING,  // 처리 중
    COMPLETED, // 모든 절차 완료
    FAILED     // 실패
}
```


```java
// user/domain/User.java (일부)
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

// ... 기존 필드 ...
@Enumerated(EnumType.STRING)
private RegistrationStatus registrationStatus;

// 생성자에서 초기 상태를 PENDING으로 설정
public User(String name, Sex sex, String phoneNum, String location, Integer age) {
    // ... 기존 초기화 ...
    this.registrationStatus = RegistrationStatus.PENDING;
}
```


**2. `UserRegisterService`에서 상태를 `PENDING`으로 저장**

`register` 메서드는 이제 사용자를 `PENDING` 상태로 생성하고 이벤트를 발행합니다.

```java
package org.codenbug.user.app;

import org.codenbug.message.SecurityUserRegisteredEvent;
import org.codenbug.user.domain.Sex;
import org.codenbug.user.domain.User;
import org.codenbug.user.domain.UserId;
import org.codenbug.user.domain.UserRepository;
import org.codenbug.user.ui.RegisterRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRegisterService {

	private final UserRepository userRepository;
	private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

	public UserRegisterService(UserRepository userRepository,
		KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate) {
		this.userRepository = userRepository;
		this.kafkaTemplate = kafkaTemplate;
	}

	@Transactional("transactionManager")
	public UserId register(RegisterRequest request) {
		// User를 생성하면 상태는 기본적으로 PENDING이 됩니다.
		UserId userId = userRepository.save(
			new User(request.getName(), Sex.valueOf(request.getSex()), request.getPhoneNum(), request.getLocation(),
				request.getAge()));
		UserRegisteredEvent event = new UserRegisteredEvent(userId.getValue(), request.getEmail(),
			request.getPassword(), "USER");
		kafkaTemplate.send("user-registered", event);
		return userId;
	}

	@Transactional("transactionManager")
	public void cancelUserRegistration(String userId) {
        // 실패 시 User를 삭제하는 대신 상태를 FAILED로 변경할 수도 있습니다.
        // 혹은 현재처럼 삭제하는 로직을 유지해도 좋습니다.
        userRepository.findByUserId(new UserId(userId)).ifPresent(user -> {
            user.failRegistration(); // User 엔티티에 상태 변경 메서드 추가
            userRepository.save(user);
        });
        // 또는 userRepository.delete(userId);
	}
}
```


**3. `SecurityUserRegisteredFailedConsumer`에서 `User`의 상태를 `FAILED`로 변경**

`auth-service`에서 `SecurityUser` 생성이 실패하여 `user-registered-failed` 이벤트가 발행되면, 이를 수신하는 `user-service`의 Consumer가 `User`의 상태를 `FAILED`로 업데이트합니다. (현재는 `cancelUserRegistration`을 호출하여 삭제하고 계십니다. 상태 관리 방식에서는 삭제 대신 상태를 변경합니다.)

```java
package org.codenbug.user.consumer;

import org.codenbug.message.UserRegisteredFailedEvent;
import org.codenbug.user.app.UserCommandService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityUserRegisteredFailedConsumer {

    private final UserRegisterService userRegisterService;

    @KafkaListener(topics = "user-registered-failed", groupId = "user-registration-failure-group")
    public void consume(UserRegisteredFailedEvent event) {
        log.warn("SecurityUser 생성이 실패하여 보상 트랜잭션을 시작합니다. UserId: {}", event.getUserId());
        // User를 삭제하거나, 상태를 FAILED로 업데이트합니다.
        userRegisterService.cancelUserRegistration(event.getUserId());
    }
}
```


**4. (중요) 성공 케이스 처리: `User` 상태를 `COMPLETED`로 변경**

`auth-service`에서 `SecurityUser` 생성이 성공했을 때도 이 사실을 `user-service`에 알려주어 `User`의 상태를 `COMPLETED`로 변경해야 합니다.

*   `auth-service`의 `UserRegisteredEventConsumer`에서 `SecurityUser` 생성 성공 후, `security-user-registered-successfully` 같은 성공 이벤트를 발행합니다.
*   `user-service`에서 이 성공 이벤트를 수신하는 새로운 Consumer를 만들어 `User`의 상태를 `COMPLETED`로 업데이트합니다.

**5. API 응답 및 클라이언트 처리**

*   **API 응답**: `UserController`의 회원가입 API는 이제 `201 Created`나 `200 OK` 대신 **`202 Accepted`**를 반환하는 것이 더 정확합니다. 이는 "요청을 접수했으며 비동기적으로 처리하겠다"는 의미입니다.
*   **클라이언트**: 클라이언트는 `202 Accepted` 응답과 함께 `userId`를 받은 후, 주기적으로 `GET /api/users/{userId}/status` 와 같은 API를 호출하여 가입 상태(`PENDING`, `COMPLETED`, `FAILED`)를 **폴링(Polling)**하여 확인하고 그에 따라 다음 화면으로 이동하거나 오류 메시지를 보여줄 수 있습니다.

### 요약

| 단계 | 서비스 | 작업 내용 | 상태 변경 |
| :--- | :--- | :--- | :--- |
| 1 | `user-service` | 회원가입 API 요청 받음 | User: **PENDING** |
| 2 | `user-service` | `user-registered` 이벤트 발행 | |
| 3 | `auth-service` | 이벤트 수신, `SecurityUser` 생성 | |
| 4a (실패) | `auth-service` | `user-registered-failed` 이벤트 발행 | |
| 5a (실패) | `user-service` | 이벤트 수신, User 삭제 또는 상태 변경 | User: **FAILED** |
| 4b (성공) | `auth-service` | `security-user-registered-successfully` 이벤트 발행 | |
| 5b (성공) | `user-service` | 이벤트 수신, User 상태 변경 | User: **COMPLETED** |

이처럼 상태 필드를 통해 분산된 시스템 간의 최종 데이터 정합성을 맞추는 것이 사가 패턴의 핵심입니다. 현재 구현은 매우 올바른 방향이며, 여기에 '상태 관리' 개념을 추가하시면 완벽한 비동기 분산 트랜잭션 처리를 완성하실 수 있습니다.