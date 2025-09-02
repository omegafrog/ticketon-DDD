# 🎫 Ticketon-DDD

도메인 주도 설계(DDD) 원칙을 적용한 마이크로서비스 기반 티켓 예약 시스템입니다.

## 📋 개요

Spring Boot와 Java 21을 사용하여 구축된 고가용성 티켓 예매 시스템으로, 이벤트 관리, 사용자 인증, 결제 처리, 좌석 관리, 대기열 시스템을 지원합니다.

## 🏗️ 시스템 아키텍처

```
                    ┌─────────────────────────────────┐
                    │      API Gateway (8080)         │
                    │    Spring Cloud Gateway         │
                    └─────────────────┬───────────────┘
                                      │
                    ┌─────────────────┼───────────────┐
                    │    Service Discovery            │
                    │      Eureka (8761)              │
                    └─────────────────┬───────────────┘
                                      │
    ┌─────────────────────────────────┼─────────────────────────────────┐
    │                                 │                                 │
    ▼                                 ▼                                 ▼
┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐
│ Auth    │     │ Event   │     │ User    │     │Purchase │     │ Seat    │
│(9001)   │     │         │     │         │     │         │     │         │
└─────────┘     └─────────┘     └─────────┘     └─────────┘     └─────────┘
    │               │               │               │               │
    └─────┬─────────┼─────────────┬─│─────────────┬─│─────────────┬─┘
          │         │             │ │             │ │             │
          ▼         ▼             ▼ ▼             ▼ ▼             ▼
    ┌─────────┐ ┌─────────┐   ┌─────────────────────────────────┐
    │ Broker  │ │Dispatcher│   │           Infrastructure        │
    │   SSE   │ │ Queue   │   │   MySQL + Redis + Kafka        │
    └─────────┘ └─────────┘   └─────────────────────────────────┘
```

## 🔧 주요 기술 스택

- **Framework**: Spring Boot 3.5, Spring Cloud Gateway
- **Database**: MySQL with JPA/Hibernate  
- **Cache/Queue**: Redis
- **Message Broker**: Kafka (Bitnami)
- **Service Discovery**: Eureka
- **Payment**: Toss Payments API
- **Security**: JWT, OAuth2 (Google/Kakao)

## 📦 마이크로서비스 모듈

### 비즈니스 서비스
- **auth** - 인증/인가 서비스 (OAuth2, JWT)
- **user** - 사용자 프로필 관리
- **event** - 이벤트 생성, 관리, 조회
- **seat** - 좌석 레이아웃 및 가용성 관리  
- **purchase** - 결제 처리 및 티켓 발급
- **broker** - SSE 연결 및 대기열 관리
- **dispatcher** - Redis 기반 큐 승급 시스템

### 인프라 서비스  
- **gateway** - API 게이트웨이 (Spring Cloud Gateway)
- **eureka** - 서비스 디스커버리
- **app** - 메인 애플리케이션 오케스트레이터
- **batch** - 통계 정보 최적화 배치 시스템 ⭐ NEW

### 공통 모듈
- **common** - 공통 유틸리티, Redis 서비스
- **message** - 서비스 간 이벤트 메시지 
- **security-aop** - AOP 기반 보안 어노테이션
- **category-id** - 이벤트 카테고리 관리

## 🔄 MySQL ANALYZE 배치 시스템

### 아키텍처
```
┌─────────────────┐
│     MySQL       │
│  (Port: 3306)   │ ◄─── 기존 애플리케이션 (ticketon 사용자)
│                 │
│  + batch_analyze│ ◄─── 배치 시스템 (ANALYZE 전용)
└─────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────┐
│                Batch Service                           │
│  • 매주 일요일 새벽 2시 ANALYZE 실행                      │
│  • Spring Batch + Quartz Scheduler                    │  
│  • REST API 모니터링 (/api/batch/*)                   │
└─────────────────────────────────────────────────────────┘
```

### 주요 기능
- **간단한 구성**: 기존 MySQL에 배치 사용자만 추가
- **자동 통계 갱신**: 주요 테이블의 통계 정보 주간 업데이트
- **옵티마이저 최적화**: 정확한 통계로 쿼리 실행 계획 개선  
- **안전한 분리**: 읽기 전용 권한으로 운영 영향 최소화

### 빠른 시작
```bash
# 1. MySQL 시작 (이미 실행 중이면 생략)
cd docker
docker compose up -d mysql

# 2. 배치 사용자 설정
./setup-simple-batch.sh

# 3. 배치 시스템 실행
./gradlew :batch:bootRun

# 4. 헬스 체크
curl http://localhost:8080/api/batch/health
```

📖 **간단한 설정 가이드**: [`docs/simple-batch-setup.md`](docs/simple-batch-setup.md)  
📖 **고급 설정 (레플리케이션)**: [`docs/mysql-replication-and-batch-setup.md`](docs/mysql-replication-and-batch-setup.md)

### 핵심 비즈니스 서비스
- **`user`** - 사용자 프로필 관리  
- **`seat`** - 좌석 레이아웃 및 가용성 관리
- **`purchase`** - 결제 처리 및 티켓 구매
- **`broker`** - SSE 연결 및 대기열 관리
- **`dispatcher`** - Redis 기반 대기열 승급 시스템

### 인프라 서비스  
- **`gateway`** - API Gateway (포트 8080)
- **`eureka`** - 서비스 디스커버리
- **`app`** - 메인 애플리케이션 오케스트레이터

### 공유 모듈
- **`common`** - 공통 유틸리티, 예외처리, Redis 서비스
- **`message`** - 서비스 간 이벤트 메시지
- **`security-aop`** - AOP 기반 보안 애노테이션
- **`category-id`** - 이벤트 카테고리 관리

## 🚀 주요 변경사항

### 최근 업데이트 (2024-2025)

#### 캐시 시스템 최적화
- **LRU 정책 적용**: 메모리 효율성 개선
- **캐시 무효화 시스템**: 실시간 데이터 일관성 보장
- **Redis ViewCount 시스템**: 이벤트 조회수 분산 처리

#### 성능 최적화  
- **N+1 문제 해결**: QueryDSL 적용으로 쿼리 최적화
- **배치 처리**: ViewCount 동기화 배치 작업
- **멀티스레딩**: Dispatcher의 병렬 대기열 승급 처리

#### 구조 개선
- **DDD 원칙 강화**: 애그리거트 경계 명확화
- **프로젝트 구조 표준화**: 모듈 간 의존성 정리
- **파일명 표준화**: 일관된 네이밍 컨벤션 적용

## ⚡ 실행 방법

### 개발 환경 실행
```bash
# 인프라 서비스 시작
docker-compose -f docker/docker-compose.yml up -d

# 전체 빌드
./gradlew build

# 특정 서비스 실행
./gradlew :gateway:bootRun
./gradlew :auth:bootRun
./gradlew :event:bootRun
```

### 서비스 포트
| 서비스 | 포트 | 설명 |
|--------|------|------|
| Gateway | 8080 | 메인 진입점 |
| Eureka | 8761 | 서비스 디스커버리 |
| Auth | 9001 | 인증 서비스 |
| App | 9000 | 메인 애플리케이션 |

### 인프라 포트  
| 서비스 | 포트 | 설명 |
|--------|------|------|
| MySQL | 3306 | 데이터베이스 |
| Redis | 6379 | 캐시/세션 |
| Kafka | 29092 | 메시지 브로커 |

## 🎯 핵심 기능

### 대기열 시스템
- **멀티스레드 승급 처리**: 동시성 제어로 안전한 좌석 예약
- **Redis 분산 락**: 중복 예약 방지
- **SSE 실시간 알림**: 대기열 상태 실시간 업데이트

### 결제 플로우
- **Toss Payments 통합**: 안전한 결제 처리
- **이벤트 기반 업데이트**: 서비스 간 비동기 통신
- **분산 락**: 좌석 중복 판매 방지

### 보안
- **OAuth2 통합**: Google, Kakao 소셜 로그인
- **JWT 토큰**: 무상태 인증
- **AOP 보안**: 선언적 보안 처리

## 📊 성능 최적화

- **QueryDSL**: 복잡한 쿼리 최적화 및 N+1 문제 해결
- **Redis 캐시**: 조회 성능 향상 및 ViewCount 분산 처리  
- **배치 처리**: 대용량 데이터 효율적 처리
- **분산 시스템**: 마이크로서비스 기반 확장성

## 🛠️ 개발 및 테스트

```bash
# 전체 테스트 실행
./gradlew test

# Docker 이미지 빌드
./gradlew :SERVICE_NAME:bootBuildImage
```