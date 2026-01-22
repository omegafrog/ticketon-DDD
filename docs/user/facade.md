# 다른 aggregate의 app service를 호출
## 작업 개요
User를 등록하는 과정에서, SecurityUser aggregate에 SecurityUser를 추가해야 하는 상황이 발생.<br/>
이때 UserRegisterService 클래스에서 SecurityUser aggregate의 엔티티를 추가해야 하는데...<br/>

```java
public UserId register(RegisterRequest request) {
		// TODO : securityUser 생성 메소드 어떻게 호출
		return userRepository.save(
			new User(request.getName(), Sex.valueOf(request.getSex()), request.getPhoneNum(), request.getLocation(),
				request.getAge()));
	}
```
## 개선 방법

1. UserRegisterService에서 SecurityUserRegisterService를 주입받는다
직접적으로 다른 aggregate의 component를 호출할 뿐더러, User aggregate에서 SecurityUser aggregate를 수정하고 있기 때문에 별로<br/>
2. Facade 패턴<br/>
다른 aggregate를 호출할 모듈에 인터페이스를 작성, 이 인터페이스를 만족하도록 호출받는 모듈에서 구현
3. 이벤트 리스너 이용

User와 SecurityUser가 서로 모듈이 다르고, 실행되는 물리 서버도 다르다.
그런데 User에서 SecurityUser를 참조하면 spring security dependency를 사용하지 않아도 user 모듈에 추가되는 상황.

그래서 kafka 도입 후 이벤트를 이용해 user를 추가 -> 이벤트 발행 -> securityUser 추가
의 방식으로 진행
## 문제
user를 추가하고, securityUser를 추가할 때, email 중복 등의 이유로 securityUser의 추가가 실패한 경우,
user만 추가되고 securityUser는 추가되지 못한 상태가 된다.
## 해결
1. saga 패턴을 통한 보상 트랜잭션
securityUser가 실패할 때 try-catch로 실패시 user를 삭제하도록 한다.
## 해결의 문제
securityUser가 삽입 실패되어 user 삭제 메시지가 발행되는데, user 삭제 메시지의 소비가 user 삽입 트랜잭션 완료보다 먼저 실행되어
user가 삭제되지 않는 오류 발생.
기대 : user 삽입 -> securityUser 삽입 -> securityUser 삽입 실패 -> user 삭제 메시지 발행 -> user 삭제 실행 -> user 삽입 트랜잭션 종료
## 다른 해결법
JTA를 구현한 구현체 Atomikos를 이용해 분산 트랜잭션 구현.
### Atomikos
2단계 커밋 프로토콜 사용. <br/>
    1. 모든 리소스가 트랜잭션 커밋 준비 확인
    2. 모두 준비되었을때 실제 커밋 진행

이렇게 되면 트랜잭션 시작 -> user삽입 -> securityUser삽입 메시지 발행 -> user삽입 트랜잭션 종료 -> user삽입 쿼리 -> securityUser 메시지 발행
-> securityUser메시지 소비 -> securityUser 삽입 트랜잭션 시작 -> securityUser 삽입 -> securityUser 삽입 트랜잭션 종료
만약 이때 securityUser 삽입 실패 -> securityUser 삽입 트랜잭션 롤백 -> catch로 user삭제 메시지 발행 -> user삭제 메시지 소비 -> user 삭제 트랜잭션 시작 및 삭제
