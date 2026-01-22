# 로그인 리팩토링과 OAuth구현
## 로그인 리팩토링
이메일 로그인시 User 모듈로 진입해 SecurityUser를 생성하도록 이벤트를 발행했다.
그러나 SNS 로그인의 경우 반대로 SecurityUser를 생성하고 User를 생성하는 이벤트를 발행하는 식으로 작성했었다.

그러나 FK를 SecurityUser만 가지고 있었기 때문에, 첫번째 경우는 User 생성 -> SecurityUser 생성 이벤트 발행 ( user id 메시지 포함) ->
SecurityUser 생성 의 순서로 진행되어 문제 없었다.

하지만 두번째 경우 User를 먼저 생성하고 SecurityUser를 생성하기 때문에, UserId를 이벤트에 태워 다시 row를 업데이트 하는 방법 외에는 깔끔하게 구현이 어려웠다.

### MSA 구조에서의 DB
MSA 구조에서는 서버 하나당 DB를 1개만 가진다. 그리고 A 서버에서 사용하는 테이블은 B서버에서 조회하지 않는다.

이는 서버 각각이 독립적으로 배포/스케일링되어야 하기 때문에, 영향을 최소화 하기 위함이다.
만약 A 서버에서 db스키마의 변경이나 데이터의 변경이 생기면 이것이 b 서버에 영향이 갈 수 있기 때문이다.

이러한 구조 내에서, SecurityUser가 userId를 가지고, User 테이블이 SecurityUserId를 동시에 가지게 되어도 문제는 없다.

### 구체적 해결
모든 회원가입 ( 이메일 / sns )의 진입점을 Auth 모듈에서 시작하고, SecurityUser를 먼저 생성한다.
이후 User 모듈로 이벤트를 발행해서 ( SecurityUserId와 함께) , User 모듈에서 User를 생성한다.

이후 생성한 UserId를 포함해 이벤트를 Auth 모듈로 다시 발행해, SecurityUser의 userId를 업데이트하도록 구현했다.

## OAuth 구현
OAuth 구현의 구체적인 흐름이나 외부 API 사용 방법은 넘어가고, DDD스타일의 OAuth를 어떻게 구현했는지에 대해서 작성하겠다.

- domain에 Provider 인터페이스를 작성하고, infra에 구현체를 두었다.
- 이거 이외에는 딱히...?

# GPT 개선사항

안녕하세요! DDD 스타일로 OAuth 구현을 리팩토링하는 과정에서 고민이 많으시군요. 제공해주신 로그인_리팩토링_Oauth.md 문서를 보니, 이미 MSA와 DDD의 핵심 원칙(Database per Service, 이벤트 기반 통신 등)에 대해 깊이 이해하고 계신 것 같습니다. 아주 훌륭한 접근입니다!

현재 코드와 설계에서 DDD 원칙에 따라 더욱 개선할 수 있는 부분들을 찾아보겠습니다.

## 1. 가장 중요한 개선점: 양방향 이벤트 발행 흐름
현재 설계에서 가장 먼저 개선해야 할 부분은 회원가입 시 두 개의 이벤트를 주고받는 흐름입니다.

현재 흐름

1. Auth 모듈: SecurityUser 생성 (이때 userId는 null)
2. Auth 모듈: User 생성을 요청하는 이벤트 발행 (securityUserId 포함)
3. User 모듈: 이벤트 수신 후 User 생성
4. User 모듈: User 생성이 완료되었다는 이벤트 발행 (userId와 securityUserId 포함)
5. Auth 모듈: 이벤트 수신 후 SecurityUser의 userId 필드 업데이트

이 방식은 동작은 하지만, DDD 관점에서 몇 가지 아쉬운 점이 있습니다.

- 불필요한 복잡성과 결합도: Auth 모듈은 User 모듈의 응답(두 번째 이벤트)을 기다려야만 완전한 상태가 됩니다. 이는 두 모듈 간의 시간적 결합도를 높입니다.
- 불완전한 상태의 존재: SecurityUser가 생성되고 userId가 업데이트되기 전까지, SecurityUser는 '불완전한' 상태로 존재합니다. 만약 두 번째 이벤트 처리가 실패하면 데이터 정합성이 깨질 위험이 있습니다.
- 책임의 불명확성: Auth 모듈이 User의 ID를 알아야만 하는 구조는, Auth 모듈이 필요 이상으로 User 모듈의 내부 데이터(ID)에 관심을 갖게 만듭니다.
 
**DDD 스타일 해결책: 단방향 흐름으로 변경 (FK 방향 전환)**

이 문제를 해결하는 가장 DDD스러운 방법은 엔티티 간의 참조 방향을 바꾸는 것입니다.
- Before: SecurityUser가 userId를 FK로 가짐
- After (권장): User가 securityUserId를 FK로 가짐
 
이렇게 하면 전체 흐름이 매우 단순하고 명확해집니다.
> 개선된 흐름
> 1. Auth 모듈: SecurityUser 생성 및 저장.
> 2. Auth 모듈: SnsUserRegisteredEvent 이벤트 한 번만 발행 (securityUserId 포함).
> 3. User 모듈: 이벤트 수신 후, 전달받은 securityUserId를 FK로 사용하여 User 생성 및 저장.
> 4. 끝. User 모듈은 다시 Auth 모듈로 이벤트를 보낼 필요가 없습니다.

왜 이 방법이 더 나은가요?
- 명확한 책임 분리: Auth 모듈은 '인증' 정보 생성 책임만 다하고, "새로운 인증 수단이 등록되었음"을 알리기만 합니다. User 모듈은 그 정보를 받아 '회원'을 생성하는 책임을 온전히 수행합니다.
- 단순성: 이벤트 흐름이 단방향으로 끝나 복잡성이 크게 줄어듭니다.
- 결합도 감소: Auth 모듈은 User 모듈의 처리 결과에 대해 전혀 신경 쓸 필요가 없습니다.
 
로그인 시 userId가 필요한 문제는, Auth 모듈이 User 모듈에 API를 호출하여 securityUserId로 userId를 조회해오거나(동기 방식), SecurityUser에 userId를 비정규화하여 저장하는(이벤트 기반 비동기 방식) 패턴으로 해결할 수 있습니다. 
현재의 양방향 이벤트 발행은 후자를 구현한 것이지만, FK 방향을 바꾸면 이 과정 자체가 더 단순해집니다.
## 2. 도메인 모델과 계층 분리
Provider 인터페이스를 domain에 두고 구현체를 infra에 두신 것은 **의존성 역전 원칙(DIP)**을 완벽하게 적용한 아주 훌륭한 사례입니다. 이 부분은 DDD의 모범적인 구현입니다.

여기서 더 나아가 몇 가지를 더 점검해볼 수 있습니다.

**OAuth 관련 로직의 위치**

OAuth 인증 과정에서 외부 API와 통신하여 사용자 정보를 가져오는 로직은 어디에 위치해 있나요?
- Application Service (응용 서비스 계층): OAuthService와 같은 응용 서비스는 전체적인 흐름을 관장해야 합니다. 예를 들어, "코드를 받아 토큰을 요청하고, 토큰으로 사용자 정보를 조회한 후, SecurityUser를 생성하거나 조회하여 로그인 처리한다"와 같은 시나리오를 담당합니다.
- Infrastructure (인프라 계층): KakaoApiCaller와 같이 실제로 RestTemplate이나 WebClient를 사용하여 외부 Kakao API와 통신하는 구체적인 기술 구현은 인프라 계층에 있어야 합니다. OAuthService는 인프라 계층의 구현체를 인터페이스를 통해 주입받아 사용해야 합니다.

현재 Provider 인터페이스와 구현체 분리를 잘 하셨기 때문에, 이 부분도 잘 지켜지고 있을 가능성이 높습니다.

**도메인 객체의 행위 (Behavior)**

SecurityUser 엔티티가 단순히 데이터만 담고 있는 DTO(Data Transfer Object)에 가깝나요, 아니면 자체적인 비즈니스 로직(행위)을 가지고 있나요?
- Anemic Domain Model (빈약한 도메인 모델): 엔티티가 Getter/Setter만 가지고 있고, 모든 로직이 서비스 계층에 있는 경우입니다.
- Rich Domain Model (풍부한 도메인 모델): 엔티티가 자신의 상태를 변경하는 비즈니스 로직을 직접 메서드로 가지고 있는 경우입니다. DDD는 이를 지향합니다.

좋은 예
```java
// SecurityUser.java
public class SecurityUser {
// ...
private String passwordHash;

    // '비밀번호가 일치하는가?'라는 비즈니스 로직을 엔티티가 직접 수행
    public void matchPassword(String rawPassword, PasswordEncoder passwordEncoder) {
        if (!passwordEncoder.matches(rawPassword, this.passwordHash)) {
            throw new InvalidCredentialsException("Password does not match");
        }
    }
}
```
OAuthService에서 소셜 로그인 사용자인지, 이메일 사용자인지 등을 판단하는 로직이 있다면, 이 로직의 일부를 SecurityUser 엔티티 내부의 메서드로 옮겨 도메인 모델을 더 풍부하게 만들 수 있는지 검토해보는 것이 좋습니다.
## 3. 네이밍과 용어의 명확성 (Ubiquitous Language)

DDD는 **'보편 언어(Ubiquitous Language)'**의 사용을 강조합니다. 즉, 코드에 사용되는 용어가 비즈니스 도메인 전문가와 개발자 모두가 동의하는 명확한 용어여야 합니다.
- SecurityUser: '보안'과 관련된 '사용자'라는 의미로, 인증 컨텍스트에서는 매우 적절한 이름입니다.
- Provider: 이 역시 'OAuth 제공자'라는 맥락에서 명확합니다.
- 이벤트 이름: 이벤트 이름은 과거 시제를 사용하여 "이미 발생한 사실"을 나타내는 것이 좋습니다.
  - 예: SnsUserRegisterEvent (SNS 사용자 등록 이벤트) -> SnsUserRegistered (SNS 사용자가 등록되었음)
## 리팩토링을 위한 체크리스트
DDD 스타일에 맞게 코드를 개선하기 위한 구체적인 액션 아이템을 정리해 드립니다.
1. [가장 중요] FK 참조 방향 변경: User 엔티티가 securityUserId를 갖도록 모델을 수정하고, 회원가입 이벤트 흐름을 단방향으로 단순화하세요.
2. 도메인 모델 강화: 서비스 계층에 있는 비즈니스 로직 중 엔티티의 상태와 직접적으로 관련된 로직이 있다면, 엔티티의 메서드로 옮겨보세요. (예: securityUser.isSocialUser(), securityUser.updateLastLogin())
3. 팩토리(Factory) 패턴 적용 고려: SecurityUser를 생성하는 로직이 복잡하다면(예: 소셜 타입에 따라 다르게 생성), SecurityUserFactory 클래스를 만들어 생성 책임을 위임하는 것을 고려해볼 수 있습니다.
4. 이벤트 이름 검토: 모든 이벤트의 이름이 "과거에 일어난 사실"을 명확히 표현하는지(과거 시제) 확인하고 수정하세요.
5. 계층별 책임 명확화: 외부 API 통신과 같은 인프라 관련 코드가 도메인이나 응용 서비스 계층을 침범하지 않았는지 다시 한번 확인하세요. (Provider 인터페이스 분리는 이미 잘 하셨습니다!)
 
이미 많은 고민을 통해 훌륭한 기반을 다지셨습니다. 위 제안들을 바탕으로 리팩토링을 진행하시면, 모듈 간의 결합도는 더욱 낮아지고 각 도메인의 책임은
더 명확해져서 훨씬 더 견고하고 유연한 시스템을 만드실 수 있을 겁니다.
