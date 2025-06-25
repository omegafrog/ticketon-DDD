# AOP를 이용한 토큰값 공유
gateway로부터 토큰을 파싱해 얻은 User-Id, Email, Role은 route할 때 헤더에 포함되어 전달된다.

그러면 controller -> service -> domain 의 컴포넌트들이 이 값을 공유하며 사용할 수 있어야 한다.

이를 기존의 Spring security의 SecurityContextHolder가 담당했으나, domain 모듈에서 이것 하나를 위해 Spring security를 추가하는 것은 맞지 않다고 판단했다.

## ThreadLocal을 이용한 값 공유
ThreadLocal은 하나의 스레드 내부에서 공유할 수 있는 데이터 공간이다. 이 ThreadLocal에 해당 정보들을 담고, 비즈니스 컴포넌트에서 자유롭게 사용하도록 해 보자.

```java
public class LoggedInUserContext implements AutoCloseable {
	private static final ThreadLocal<UserSecurityToken> storage = new ThreadLocal<>();

	public static LoggedInUserContext open(UserSecurityToken token){
		return new LoggedInUserContext(token);
	}
	private LoggedInUserContext(UserSecurityToken token) {
		storage.set(token);
	}

	public static UserSecurityToken get() {
		return storage.get();
	}

	public static void clear() {
		storage.remove();
	}

	@Override
	public void close() throws IOException {
		clear();
	}
}
```
`private static final`로 정의된 storage는 얼핏 보면 모든 jvm에서 공유하도록 작성된 것 같지만, 그렇지 않다.
ThreadLocal은 저렇게 정의되어 있어도 내부적으로 Map 형태로 구현되어, 스레드마다 개인 공간을 가진다.

하나의 요청을 처리할 때 한명의 클라이언트 정보만 필요하므로, 단일 유저 인가 정보만 담으면 된다. 따라서 이를 의미하는 `UserSecurityToken` 클래스를 작성, 
이것을 넣어 준다.

생성과 동시에 초기화하기 위해서, 정적 팩토리 메서드로 작성하고, 생성자는 프라이빗으로 지정했다.

이제 원하는 곳에서 `LoggedInUserContext.get()`을 이용해 값을 가져올 수 있다.

## ThreadLocal의 주의점
기본적으로 객체에 대한 참조가 모두 사라질 때, 가비지 컬렉션이 작동하고, 메모리가 해제된다.
하지만 스레드는 다 사용했다고 바로 GC하지 않고, 스레드풀에 의해 재사용된다.

그러므로 이 스레드와 연결된 스레드로컬 저장소 또한 재사용될 수 있다.
이를 방지하기 위해서는 항상 초기화`clear()` 가 필요하다.

## AutoClosable
Java 7 이후부터 try-with-resource 구문이 추가되었다. 이는 명시적으로 종료해야 하는 자원들을 실수 없이 닫을 수 있도록 해 주는 구문이다.
```java
try (BufferedReader reader = new BufferedReader(new FileReader("file.txt"))) {
    String line = reader.readLine();
    System.out.println(line);
} catch (IOException e) {
	e.printStackTrace();
}
```
이 경우 try 문이 종료될 경우 reader의 close() 메서드가 자동으로 호출되어 메모리 누수를 방지한다.

구현 방법은 간단하다. `AutoClosable`인터페이스를 구현한 구현체를 위 예시와 같이 사용하면 된다.
```java
public class LoggedInUserContext implements AutoCloseable {
    ...
	@Override
	public void close() throws IOException {
		clear();
	}
}
```
이렇게 적용하여 쉽게 ThreadLocal을 사용할 수 있다.

## 저장소 사용을 위한 AOP
이제 이 저장소를 사용해 보자. 필터에서 요청의 헤더를 받아 이 저장소에 넣는 AOP를 작성해 보자.
인증이 필요한 경우만 이 작업을 수행하기 위해서 @AuthNeeded 어노테이션을 작성하고 컨트롤러 메소드에 추가했다.

```java
@Around("@annotation(AuthNeeded)")
	public Object setUserSecurityToken(ProceedingJoinPoint joinPoint) throws Throwable {
		String userId = request.getHeader("User-Id");
		Role role = Role.valueOf(request.getHeader("Role"));
		// boolean socialUser = request.getHeader("socialUser") != null;
		String email = request.getHeader("Email");
		try (LoggedInUserContext context = LoggedInUserContext.open(new UserSecurityToken(userId, email, role))
		) {
			return joinPoint.proceed();
		}
	}
```

```java
@AuthNeeded
	@GetMapping("/logout")
	public ResponseEntity<RsData<Void>> logout(HttpServletRequest req, HttpServletResponse resp){
		Cookie refreshToken = Arrays.stream(req.getCookies())
			.filter(cookie -> cookie.getName().equals("refreshToken"))
			.findFirst()
			.orElseThrow(() -> new RuntimeException("refreshToken is null."));

		refreshToken.setMaxAge(0);
		resp.addCookie(refreshToken);

		blackList.add(req.getHeader("User-Id") ,refreshToken);



		return ResponseEntity.ok(new RsData<>("200", "logout success.", null));
	}
```

