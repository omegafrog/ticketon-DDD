# Notification 모듈 App 통합 가이드

## 개요
Notification 모듈이 별도 서버가 아닌 App 모듈 내의 Bean으로 통합되었습니다.

## 변경사항

### 1. 아키텍처 변경
- **이전**: Notification 독립 서버 (port 9003)
- **이후**: App 모듈 내 라이브러리로 통합

### 2. 모듈 구조
```
app (port 9000)
├── notification (라이브러리)
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── dto/
├── controller/notification/
└── service/
```

### 3. 데이터베이스
- App 모듈의 H2/MySQL에 notification 테이블 자동 생성
- 기존 notification 테이블과 동일한 스키마

## API 엔드포인트

### 기본 알림 API (포트 9000)
```bash
# 알림 목록 조회
GET http://localhost:9000/api/v1/notifications

# 알림 상세 조회
GET http://localhost:9000/api/v1/notifications/{id}

# 미읽은 알림 조회
GET http://localhost:9000/api/v1/notifications/unread

# 알림 생성 (관리자)
POST http://localhost:9000/api/v1/notifications

# 알림 삭제
DELETE http://localhost:9000/api/v1/notifications/{id}

# SSE 구독
GET http://localhost:9000/api/v1/notifications/subscribe
```

### 테스트 API
```bash
# 직접 알림 생성 테스트
POST http://localhost:9000/api/v1/test-notifications/direct?userId=1&title=테스트&content=내용

# 티켓 구매 알림 테스트
POST http://localhost:9000/api/v1/test-notifications/ticket-purchase?userId=1&eventTitle=콘서트&ticketInfo=A석

# 결제 완료 알림 테스트
POST http://localhost:9000/api/v1/test-notifications/payment-completed?userId=1&amount=50000

# 알림 개수 조회
GET http://localhost:9000/api/v1/test-notifications/count/1
```

### 환불 알림 테스트 (Purchase 모듈)
```bash
# 사용자 환불 테스트
POST http://localhost:9000/api/v1/test-refund-notifications/user-refund?userId=1&refundAmount=50000

# 매니저 환불 테스트
POST http://localhost:9000/api/v1/test-refund-notifications/manager-refund?userId=1&managerName=관리자
```

## Kafka 이벤트

### 설정
- 그룹 ID: `app-notification-service`
- 브로커: `localhost:29092`

### 토픽
- `notification.refund.completed`: 사용자 환불 완료
- `notification.manager.refund.completed`: 매니저 환불 완료

## 테스트 시나리오

### 1. Bean 주입 확인
```bash
curl -X POST "http://localhost:9000/api/v1/test-notifications/direct?userId=1"
```

### 2. Kafka 이벤트 확인
```bash
# Purchase에서 환불 이벤트 발행
curl -X POST "http://localhost:9000/api/v1/test-refund-notifications/user-refund?userId=1"

# Notification 생성 확인
curl "http://localhost:9000/api/v1/notifications?userId=1"
```

### 3. SSE 연결 확인
```bash
curl -N -H "Accept: text/event-stream" "http://localhost:9000/api/v1/notifications/subscribe"
```

## 주요 클래스

### App 모듈
- `NotificationConfig`: 설정 클래스 Import
- `NotificationController`: API 엔드포인트
- `NotificationClientService`: 다른 서비스에서 사용
- `TestNotificationController`: 테스트 API

### Notification 모듈 (라이브러리)
- `NotificationApplicationService`: 비즈니스 로직
- `PurchaseNotificationEventListener`: Kafka 이벤트 수신
- `Notification`: 도메인 엔티티

## 장점

1. **단순화된 배포**: 하나의 App 서버만 실행
2. **직접 호출**: 네트워크 오버헤드 없음
3. **트랜잭션 일관성**: 같은 DB 사용
4. **개발 편의성**: 하나의 애플리케이션에서 디버깅

## 주의사항

1. **메모리 사용량**: 알림 기능이 App 메모리 사용
2. **확장성**: 알림 트래픽이 많을 경우 고려 필요
3. **장애 격리**: App 장애 시 알림도 함께 영향

## 향후 확장

필요시 다시 별도 서비스로 분리 가능:
1. Notification 모듈의 독립성 유지
2. 설정만 변경하면 분리 가능
3. API 스펙 동일하게 유지