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
2. Facade 패턴
다른 aggregate를 호출할 모듈에 인터페이스를 작성, 이 인터페이스를 만족하도록 호출받는 모듈에서 구현
3. 이벤트 리스너 이용
