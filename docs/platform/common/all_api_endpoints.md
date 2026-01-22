# Ticketon DDD - 모든 API 엔드포인트 목록

컨트롤러 소스 기준으로 정리한 전체 엔드포인트 목록입니다. (내부/테스트/정적 포함)

## APP Service API

- **GET** `/static/events/images/{fileName}` - 이벤트 정적 이미지 제공

---

## AUTH Service API

- **POST** `/api/v1/auth/register` - 회원가입
- **POST** `/api/v1/auth/login` - 로그인
- **GET** `/api/v1/auth/logout` - 로그아웃
- **GET** `/api/v1/auth/social/{socialLoginType}` - 소셜 로그인 요청
- **GET** `/api/v1/auth/social/{socialLoginType}/callback` - 소셜 로그인 콜백
- **POST** `/api/v1/auth/refresh` - 토큰 재발급

---

## USER Service API

- **GET** `/api/v1/users/me` - 내 정보 조회
- **PUT** `/api/v1/users/me` - 내 정보 수정
- **POST** `/internal/users/validate` - 회원가입 입력값 내부 검증

---

## EVENT Service API

### Event Command
- **POST** `/api/v1/events` - 이벤트 등록
- **PUT** `/api/v1/events/{eventId}` - 이벤트 수정
- **DELETE** `/api/v1/events/{eventId}` - 이벤트 삭제
- **PATCH** `/api/v1/events/{eventId}` - 이벤트 상태 변경

### Event Query
- **POST** `/api/v1/events/list` - 이벤트 목록 조회
- **GET** `/api/v1/events/{id}` - 이벤트 상세 조회
- **GET** `/api/v1/events/manager/me` - 매니저 이벤트 목록 조회

### Event Category
- **GET** `/api/v1/categories` - 카테고리 목록 조회

### Image Upload
- **POST** `/api/v1/events/image/url` - 이미지 업로드 URL 발급
- **PUT** `/static/events/images/{fileName}` - 이미지 업로드

### Internal
- **GET** `/internal/events/{eventId}/summary` - 이벤트 요약 조회
- **GET** `/internal/events/{eventId}/version-check` - 이벤트 버전/상태 검증

### Test/Batch
- **POST** `/api/v1/test/cache-invalidation` - 캐시 무효화 테스트
- **POST** `/api/v1/batch/viewcount-sync` - 조회수 동기화 배치 수동 실행

---

## SEAT Service API

- **GET** `/api/v1/events/{event-id}/seats` - 좌석 레이아웃 조회
- **POST** `/api/v1/events/{event-id}/seats` - 좌석 선택
- **DELETE** `/api/v1/events/{event-id}/seats` - 좌석 선택 취소
- **GET** `/internal/seat-layouts/{layout-id}` - 좌석 레이아웃 내부 조회

---

## PURCHASE Service API

### Payment
- **POST** `/api/v1/payments/init` - 결제 준비
- **POST** `/api/v1/payments/confirm` - 결제 승인
- **POST** `/api/v1/payments/{paymentKey}/cancel` - 결제 취소

### Purchases
- **GET** `/api/v1/purchases/history` - 사용자 구매 내역 조회
- **GET** `/api/v1/purchases/event/{eventId}` - 이벤트 구매 내역 조회

### Refunds
- **GET** `/api/v1/refunds/my` - 내 환불 내역 조회
- **GET** `/api/v1/refunds/{refundId}` - 환불 상세 조회
- **POST** `/api/v1/refunds/manager/single` - 매니저 단일 환불 처리
- **POST** `/api/v1/refunds/manager/batch` - 매니저 일괄 환불 처리
- **GET** `/api/v1/refunds/admin/by-status` - 환불 상태별 조회

### Test
- **GET** `/api/test/purchase/original/{eventId}` - 기존 JPQL 쿼리 테스트
- **GET** `/api/test/purchase/optimized/{eventId}` - 최적화 쿼리 테스트
- **GET** `/api/test/purchase/compare/{eventId}` - 쿼리 성능 비교
- **GET** `/api/test/purchase/events` - 테스트 이벤트 목록

---

## NOTIFICATION Service API

- **GET** `/api/v1/notifications` - 알림 목록 조회
- **GET** `/api/v1/notifications/{id}` - 알림 상세 조회
- **POST** `/api/v1/notifications` - 알림 생성
- **GET** `/api/v1/notifications/unread` - 미읽은 알림 조회
- **GET** `/api/v1/notifications/subscribe` - 알림 SSE 구독
- **DELETE** `/api/v1/notifications/{id}` - 알림 삭제 (단건)
- **POST** `/api/v1/notifications/batch-delete` - 알림 삭제 (다건)
- **DELETE** `/api/v1/notifications/all` - 알림 전체 삭제
- **GET** `/api/v1/notifications/count/unread` - 미읽은 알림 개수 조회
- **POST** `/api/v1/notifications/test` - 테스트 알림 생성

---

## BROKER/MONITORING API

- **GET** `/api/v1/broker/events/{id}/tickets/waiting` - 대기열 진입 (SSE)
- **POST** `/api/v1/broker/events/{id}/tickets/disconnect` - 대기열 연결 해제
- **GET** `/api/v1/monitoring/threadpool` - 스레드 풀 상세 모니터링
- **GET** `/api/v1/monitoring/threadpool/summary` - 스레드 풀 요약

---

## 통계

- **총 API 개수**: 56
- **APP Service**: 1개
- **AUTH Service**: 6개
- **USER Service**: 3개
- **EVENT Service**: 14개
- **SEAT Service**: 4개
- **PURCHASE Service**: 14개
- **NOTIFICATION Service**: 10개
- **BROKER/MONITORING**: 4개

### HTTP 메서드별 분포
- **DELETE**: 4개
- **GET**: 29개
- **PATCH**: 1개
- **POST**: 17개
- **PUT**: 3개
