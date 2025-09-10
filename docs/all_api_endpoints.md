# Ticketon DDD - 모든 API 엔드포인트 목록

게이트웨이를 통해 수집된 모든 서비스의 API 엔드포인트입니다.

## AUTH Service API

### Authentication

- **POST** `/api/v1/auth/login` - 로그인
  - 이메일과 비밀번호로 로그인합니다.
  - Operation ID: `login`
- **GET** `/api/v1/auth/logout` - 로그아웃
  - 현재 로그인된 사용자를 로그아웃합니다.
  - Operation ID: `logout`
- **POST** `/api/v1/auth/register` - 회원가입
  - 새로운 사용자를 등록합니다.
  - Operation ID: `register`
- **GET** `/api/v1/auth/social/{socialLoginType}` - 소셜 로그인 요청
  - 소셜 로그인 페이지로의 리다이렉션 URL을 반환합니다.
  - Operation ID: `request`
- **GET** `/api/v1/auth/social/{socialLoginType}/callback` - 소셜 로그인 콜백
  - 소셜 로그인 콜백을 처리하고 JWT 토큰을 발급합니다.
  - Operation ID: `callback`

---

## EVENT Service API

### Event Command

- **POST** `/api/v1/events` - 이벤트 등록
  - 새로운 이벤트를 등록합니다. 매니저 권한이 필요합니다.
  - Operation ID: `eventRegister`
- **DELETE** `/api/v1/events/{eventId}` - 이벤트 삭제
  - 이벤트를 삭제합니다. 매니저 또는 관리자 권한이 필요합니다.
  - Operation ID: `deleteEvent`
- **PATCH** `/api/v1/events/{eventId}` - 이벤트 상태 변경
  - 이벤트의 상태를 변경합니다. 매니저 또는 관리자 권한이 필요합니다.
  - Operation ID: `changeStatus`
- **PUT** `/api/v1/events/{eventId}` - 이벤트 수정
  - 기존 이벤트 정보를 수정합니다. 매니저 또는 관리자 권한이 필요합니다.
  - Operation ID: `updateEvent`

### Seat

- **DELETE** `/api/v1/events/{event-id}/seats`
  - Operation ID: `cancelSeat`
- **GET** `/api/v1/events/{event-id}/seats`
  - Operation ID: `getSeatLayout`
- **POST** `/api/v1/events/{event-id}/seats`
  - Operation ID: `selectSeat`

### Event Query

- **POST** `/api/v1/events/list` - 이벤트 목록 조회
  - 필터와 키워드를 기반으로 이벤트 목록을 조회합니다. 필터에서 categoryId(단일 카테고리) 또는 eventCategoryList(다중 카테고리)를 사용하여 카테고리별 필터링이 가능합니다.
  - Operation ID: `getEvents`
- **GET** `/api/v1/events/manager/me`
  - Operation ID: `getManagerEvents`
- **GET** `/api/v1/events/{id}`
  - Operation ID: `getEvent`

### image-upload-controller

- **POST** `/api/v1/events/image/url`
  - Operation ID: `generateImageUploadUrls`

### event-category-controller

- **GET** `/api/v1/categories`
  - Operation ID: `getAllCategories`

---

## PURCHASE Service API

### refund-controller

- **GET** `/api/v1/refunds/admin/by-status`
  - Operation ID: `getRefundsByStatus`
- **POST** `/api/v1/refunds/manager/batch`
  - Operation ID: `processBatchRefund`
- **POST** `/api/v1/refunds/manager/single`
  - Operation ID: `processManagerRefund`
- **GET** `/api/v1/refunds/my`
  - Operation ID: `getMyRefunds`
- **GET** `/api/v1/refunds/{refundId}`
  - Operation ID: `getRefundDetail`

### Purchase

- **POST** `/api/v1/payments/confirm` - 결제 승인
  - 결제를 최종 승인하고 티켓을 발급합니다. 대기열 인증 토큰이 필요합니다.
  - Operation ID: `confirmPayment`
- **POST** `/api/v1/payments/init` - 결제 준비
  - 티켓 구매를 위한 결제 준비 과정을 시작합니다. 대기열 인증 토큰이 필요합니다.
  - Operation ID: `initiatePayment`
- **POST** `/api/v1/payments/{paymentKey}/cancel` - 결제 취소
  - 결제를 취소하고 티켓을 환불 처리합니다.
  - Operation ID: `cancelPayment`

### purchase-query-controller

- **GET** `/api/v1/purchases/event/{eventId}`
  - Operation ID: `getEventPurchases`
- **GET** `/api/v1/purchases/history`
  - Operation ID: `getPurchaseHistory`

---

## USER Service API

### User

- **GET** `/api/v1/users/me`
  - Operation ID: `getMe`
- **PUT** `/api/v1/users/me`
  - Operation ID: `updateMe`

---

## SEAT Service API

### Seat

- **DELETE** `/api/v1/events/{event-id}/seats`
  - Operation ID: `cancelSeat`
- **GET** `/api/v1/events/{event-id}/seats`
  - Operation ID: `getSeatLayout`
- **POST** `/api/v1/events/{event-id}/seats`
  - Operation ID: `selectSeat`

---

## 통계

- **총 API 개수**: 32
- **AUTH Service**: 5개
- **EVENT Service**: 12개
- **PURCHASE Service**: 10개
- **SEAT Service**: 3개
- **USER Service**: 2개

### HTTP 메서드별 분포
- **DELETE**: 3개
- **GET**: 14개
- **PATCH**: 1개
- **POST**: 12개
- **PUT**: 2개
