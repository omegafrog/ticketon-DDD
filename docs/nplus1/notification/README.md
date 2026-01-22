# Notification 모듈 테스트 환경 이슈 및 수정

## 문제 상황
테스트에서 `Notification` 저장 시 `NotificationContent`와 `UserId`가 임베디드 타입임에도 JPA가 인스턴스를 생성하지 못해 실패했습니다.

```
Caused by: org.hibernate.InstantiationException: Unable to locate constructor for embeddable
```

## 원인
임베디드 타입에 기본 생성자와 `@Embeddable`이 없었고, final 필드로 인해 JPA가 리플렉션으로 인스턴스화할 수 없었습니다.

## 해결 방법
임베디드 타입을 `@Embeddable`로 지정하고 기본 생성자를 추가했습니다.

```java
@Embeddable
public class NotificationContent {
    private String title;
    private String content;
    private String targetUrl;

    protected NotificationContent() {
    }

    public NotificationContent(String title, String content, String targetUrl) {
        this.title = validateAndNormalizeTitle(title);
        this.content = validateContent(content);
        this.targetUrl = targetUrl;
    }
}
```

```java
@Embeddable
public class UserId {
    private String value;

    protected UserId() {
    }

    public UserId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
        this.value = value.trim();
    }
}
```
