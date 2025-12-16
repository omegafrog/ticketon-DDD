# Auth (인증) 서비스

## 1. 개요

Ticketon DDD 프로젝트의 인증 및 인가 기능을 담당하는 마이크로서비스입니다. 사용자 회원가입, 로그인, JWT 토큰 기반 인증, 소셜 로그인 등의 핵심 보안 기능을 제공합니다. 또한, 다른 서비스와의 비동기 통신을 위해 Kafka를 사용하여 `UserRegisteredEvent`와 같은 이벤트를 발행하고 수신합니다.

## 2. 패키지 구조

`org.codenbug.auth`를 루트 패키지로 사용하며, 주요 하위 패키지의 역할은 다음과 같습니다.

```
.
├── app/              # 애플리케이션 서비스, 인바운드/아웃바운드 포트
├── config/           # Security, Kafka, Redis 등 주요 설정 클래스
├── consumer/         # Kafka 메시지 컨슈머
├── domain/           # 핵심 도메인 모델 (예: Member, Role)
├── global/           # 전역적으로 사용되는 예외 처리, 유틸리티 등
├── infra/            # 외부 시스템 연동 (예: DB Repository, Kafka Producer)
├── ui/               # 외부 세계와의 인터페이스 (예: REST 컨트롤러)
└── AuthApplication.java # Spring Boot 시작 클래스
```

## 3. 주요 의존성 (Dependencies)

`build.gradle`에 명시된 주요 의존성은 다음과 같습니다.

-   **Spring Boot Starter Security**: 스프링 기반의 인증 및 인가 기능 제공
-   **Spring Boot Starter Web**: RESTful API 개발을 위한 웹 프레임워크
-   **Spring Boot Starter Data JPA**: 데이터베이스 연동을 위한 ORM
-   **Spring Boot Starter Data Redis**: Redis 연동을 위한 라이브러리 (예: 토큰 저장, 캐싱)
-   **Spring Kafka**: Apache Kafka와의 연동 지원
-   **QueryDSL**: 타입-세이프(Type-safe)한 동적 쿼리 생성을 위한 라이브러리
-   **MySQL Connector/J**: MySQL 데이터베이스 드라이버
-   **JJWT**: JWT(JSON Web Token) 생성 및 검증 라이브러리
-   **SpringDoc OpenAPI**: API 문서 자동화를 위한 Swagger UI 제공
-   **Project Modules**: `:common`, `:message`, `:security-aop` 등 내부 공통 모듈

## 4. application.yml 구조

`application.yml` 파일은 주요 설정 정보를 포함하며, 환경별(`dev`, `prod` 등) 프로필을 통해 설정을 분리합니다. (`secret` 프로필 포함)

```yaml
spring:
  # Kafka 설정 (Bootstrap 서버, Producer/Consumer 설정)
  kafka:
    bootstrap-servers: <kafka_bootstrap_servers>
    consumer:
      group-id: <consumer_group_id>
      # ...
    producer:
      # ...

  # 활성 프로필 및 외부 설정 파일 포함
  profiles:
    include: secret
    active: dev

  # 데이터베이스 설정 (Primary/Readonly 분리)
  datasource:
    primary:
      jdbc-url: <primary_db_url>
      username: <db_username>
      password: <db_password>
      # ...
    readonly:
      jdbc-url: <readonly_db_url>
      # ...
  
  # JPA 및 Hibernate 설정
  jpa:
    database-platform: org.hibernate.dialect.MySQL8Dialect
    hibernate:
      ddl-auto: update # 개발 환경에서는 update 사용

  # Redis 설정
  data:
    redis:
      port: <redis_port>
      host: <redis_host>

# SpringDoc (Swagger) 설정
springdoc:
  override-with-generic-response: false

# 내장 웹 서버 설정
server:
  port: <server_port>
  tomcat:
    threads:
      # ...
    # ...

# 로깅 레벨 설정
logging:
  level:
    org.codenbug: DEBUG
    org.springframework.security: DEBUG

# 커스텀 속성
custom:
  cookie:
    domain: <cookie_domain>
  sns:
    google:
      url: <google_oauth_url>

# Actuator 및 Prometheus 설정
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  prometheus:
    metrics:
      export:
        enabled: true
```
