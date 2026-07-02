# UC-032. `Notification Sender`가 `Notification Recipient`용 `Notification`을 생성한다 이벤트 스토밍

## 1. 시작 유스케이스
- 유스케이스: `UC-032. Notification Sender creates a Notification for a Notification Recipient`
- 액터: `Notification Sender`
- 목표: 지정한 `Recipient User ID`를 가진 `Notification Recipient`에게 `Unread` 상태의 `Notification` 1건을 생성한다.
- 초기 커맨드: 🟦 `Notification Recipient`용 `Notification` 생성을 요청하라

### 사전 조건
- 요청자는 인증되어 있다.
- 요청자는 `ADMIN` 또는 `MANAGER` 권한을 가진다.
- 요청에는 `Recipient User ID`, `Notification type`, `title`, `content`가 포함된다.

### 종료 조건
- 성공: 지정한 `Recipient User ID`에 대해 `Unread` 상태의 `Notification` 1건이 저장되고 대상 `Notification Inbox`에서 조회 가능해진다.
- 실패: 비인증 요청, `USER` 권한 요청, 빈 `Recipient User ID`, 필수 입력 누락 요청은 저장 없이 거절된다.

---
## 2. 흐름
### [Flow: `Notification` 생성 성공]
🟦 `Notification Recipient`용 `Notification` 생성을 요청하라
→ 🟧 `Notification` 생성이 요청되었다
→ 🟪 요청자가 인증된 경우
→ 🟦 요청자의 `Notification` 생성 권한을 확인하라
→ 🟧 요청자의 `Notification` 생성 권한이 확인되었다
→ 🟪 요청자가 `ADMIN` 또는 `MANAGER`인 경우
→ 🟦 `Recipient User ID`와 필수 입력을 검증하라
→ 🟧 `Recipient User ID`와 필수 입력이 검증되었다
→ 🟪 `Recipient User ID`가 비어 있지 않고 필수 입력이 모두 제공된 경우
→ 🟦 `Unread Notification` 1건을 저장하라
→ 🟧 `Unread Notification` 1건이 저장되었다
→ 🟦 저장된 `Notification`을 대상 `Notification Inbox`에서 조회 가능하게 하라
→ 🟧 저장된 `Notification`이 대상 `Notification Inbox`에서 조회 가능해졌다
→ 🟦 `Notification` 생성 결과를 반환하라
→ 🟧 `Notification` 생성 결과가 반환되었다

---
### [Flow: 비인증 `Notification` 생성 요청 거절]
🟦 `Notification Recipient`용 `Notification` 생성을 요청하라
→ 🟧 `Notification` 생성이 요청되었다
→ 🟪 요청자가 인증되지 않은 경우
→ 🟧 `Notification` 생성 요청이 인증 부족으로 거절되었다

---
### [Flow: `USER` 권한 `Notification` 생성 요청 거절]
🟦 `Notification Recipient`용 `Notification` 생성을 요청하라
→ 🟧 `Notification` 생성이 요청되었다
→ 🟪 요청자가 인증된 경우
→ 🟦 요청자의 `Notification` 생성 권한을 확인하라
→ 🟧 요청자의 `Notification` 생성 권한이 확인되었다
→ 🟪 요청자가 `ADMIN` 또는 `MANAGER`가 아닌 경우
→ 🟧 `Notification` 생성 요청이 권한 부족으로 거절되었다

---
### [Flow: 빈 `Recipient User ID` 또는 필수 입력 누락으로 생성 요청 거절]
🟦 `Notification Recipient`용 `Notification` 생성을 요청하라
→ 🟧 `Notification` 생성이 요청되었다
→ 🟪 요청자가 인증된 경우
→ 🟦 요청자의 `Notification` 생성 권한을 확인하라
→ 🟧 요청자의 `Notification` 생성 권한이 확인되었다
→ 🟪 요청자가 `ADMIN` 또는 `MANAGER`인 경우
→ 🟦 `Recipient User ID`와 필수 입력을 검증하라
→ 🟧 `Recipient User ID`와 필수 입력이 검증되었다
→ 🟪 `Recipient User ID`가 비어 있거나 필수 입력이 누락된 경우
→ 🟧 `Notification` 생성 요청이 입력 검증 실패로 거절되었다

---
## 3. 도메인 요소 (통합)
|유형|내용|트리거|결과|시스템|비고|
|---|---|---|---|---|---|
|🟦|`Notification Recipient`용 `Notification` 생성을 요청하라|`Notification Sender`|`Notification` 생성이 요청되었다|`Gateway 인증 계층`|`Notification` 생성 시작 커맨드|
|🟦|요청자의 `Notification` 생성 권한을 확인하라|요청자가 인증된 경우|요청자의 `Notification` 생성 권한이 확인되었다|`Gateway 인증 계층`|`ADMIN` 또는 `MANAGER` 여부 확인|
|🟦|`Recipient User ID`와 필수 입력을 검증하라|요청자가 `ADMIN` 또는 `MANAGER`인 경우|`Recipient User ID`와 필수 입력이 검증되었다|`Notification 생성 시스템`|필수 입력: `Recipient User ID`, `Notification type`, `title`, `content`|
|🟦|`Unread Notification` 1건을 저장하라|`Recipient User ID`가 비어 있지 않고 필수 입력이 모두 제공된 경우|`Unread Notification` 1건이 저장되었다|`Notification 생성 시스템`|지정한 `Recipient User ID` 기준 단건 저장|
|🟦|저장된 `Notification`을 대상 `Notification Inbox`에서 조회 가능하게 하라|`Unread Notification` 1건이 저장되었다|저장된 `Notification`이 대상 `Notification Inbox`에서 조회 가능해졌다|`Notification 생성 시스템`|저장 성공 후 대상 수신자 inbox 노출|
|🟦|`Notification` 생성 결과를 반환하라|저장된 `Notification`이 대상 `Notification Inbox`에서 조회 가능해졌다|`Notification` 생성 결과가 반환되었다|`Notification 생성 시스템`|성공 응답 반환|
|🟧|`Notification` 생성이 요청되었다|`Notification Recipient`용 `Notification` 생성을 요청하라|인증 정책 평가|`Gateway 인증 계층`|생성 요청 진입|
|🟧|요청자의 `Notification` 생성 권한이 확인되었다|요청자의 `Notification` 생성 권한을 확인하라|권한 허용 또는 권한 거절 정책 평가|`Gateway 인증 계층`|역할 기반 생성 가능 여부 판단|
|🟧|`Recipient User ID`와 필수 입력이 검증되었다|`Recipient User ID`와 필수 입력을 검증하라|저장 실행 또는 입력 검증 실패 정책 평가|`Notification 생성 시스템`|입력 유효성 판단 완료|
|🟧|`Unread Notification` 1건이 저장되었다|`Unread Notification` 1건을 저장하라|대상 inbox 조회 가능 처리|`Notification 생성 시스템`|`Unread` 상태 단건 생성 완료|
|🟧|저장된 `Notification`이 대상 `Notification Inbox`에서 조회 가능해졌다|저장된 `Notification`을 대상 `Notification Inbox`에서 조회 가능하게 하라|생성 결과 반환|`Notification 생성 시스템`|대상 수신자 관찰 가능 상태|
|🟧|`Notification` 생성 결과가 반환되었다|`Notification` 생성 결과를 반환하라|`Notification` 생성 흐름 종료|`Notification 생성 시스템`|성공 응답 완료|
|🟧|`Notification` 생성 요청이 인증 부족으로 거절되었다|요청자가 인증되지 않은 경우|생성 실패 응답 반환|`Gateway 인증 계층`|비인증 요청 차단|
|🟧|`Notification` 생성 요청이 권한 부족으로 거절되었다|요청자가 `ADMIN` 또는 `MANAGER`가 아닌 경우|생성 실패 응답 반환|`Gateway 인증 계층`|`USER` 권한 요청 차단|
|🟧|`Notification` 생성 요청이 입력 검증 실패로 거절되었다|`Recipient User ID`가 비어 있거나 필수 입력이 누락된 경우|생성 실패 응답 반환|`Notification 생성 시스템`|저장 없이 실패|
|🟪|요청자가 인증된 경우|`Notification` 생성이 요청되었다|요청자의 `Notification` 생성 권한을 확인하라|`Gateway 인증 계층`|인증 통과 시에만 권한 확인 진행|
|🟪|요청자가 인증되지 않은 경우|`Notification` 생성이 요청되었다|`Notification` 생성 요청이 인증 부족으로 거절되었다|`Gateway 인증 계층`|비인증 요청 즉시 거절|
|🟪|요청자가 `ADMIN` 또는 `MANAGER`인 경우|요청자의 `Notification` 생성 권한이 확인되었다|`Recipient User ID`와 필수 입력을 검증하라|`Gateway 인증 계층`|허용된 생성 주체만 진행|
|🟪|요청자가 `ADMIN` 또는 `MANAGER`가 아닌 경우|요청자의 `Notification` 생성 권한이 확인되었다|`Notification` 생성 요청이 권한 부족으로 거절되었다|`Gateway 인증 계층`|`USER` 권한 생성 차단|
|🟪|`Recipient User ID`가 비어 있지 않고 필수 입력이 모두 제공된 경우|`Recipient User ID`와 필수 입력이 검증되었다|`Unread Notification` 1건을 저장하라|`Notification 생성 시스템`|선택 입력인 `target URL` 부재는 허용|
|🟪|`Recipient User ID`가 비어 있거나 필수 입력이 누락된 경우|`Recipient User ID`와 필수 입력이 검증되었다|`Notification` 생성 요청이 입력 검증 실패로 거절되었다|`Notification 생성 시스템`|단건 저장 금지|
|🟩|없음|없음|없음|외부|이 유스케이스는 외부 시스템 협력이 필요 없다|

---
## 4. 외부 시스템
|시스템|연동 목적|관련 유스케이스|비고|
|---|---|---|---|
|없음|없음|`UC-032`|내부 `Gateway 인증 계층`과 `Notification 생성 시스템`만 사용|

---
## 5. 규칙 (Invariant)
- `Notification` 생성은 인증된 `ADMIN` 또는 `MANAGER`만 수행할 수 있다.
- 유효한 생성 요청은 지정한 `Recipient User ID`에 대해 `Unread` 상태의 `Notification` 정확히 1건만 저장한다.
- 빈 `Recipient User ID` 또는 필수 입력 누락 요청은 `Notification`을 저장하지 않는다.
- `target URL`은 선택 입력이며 제공되지 않아도 다른 필수 입력이 유효하면 생성할 수 있다.
- 저장된 `Notification`은 대상 `Notification Recipient`의 `Notification Inbox`에서 조회 가능해져야 한다.

---
## 6. 확인 필요
- 없음
