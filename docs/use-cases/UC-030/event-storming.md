# UC-030. Notification Recipient가 Notification Inbox를 조회한다 이벤트 스토밍

## 1. 시작 유스케이스
- 유스케이스: `UC-030. Notification Recipient views their Notification Inbox`
- 액터: `Notification Recipient`
- 목표: 자신의 `Recipient User ID`로 수신된 `Notification Inbox` 목록, `Unread Notification` 목록, `Unread Notification` 개수, `Notification` 상세를 조회한다.
- 초기 커맨드: 🟦 `Notification Inbox` 목록을 조회하라

### 사전 조건
- 요청자는 인증되어 있다.
- 요청자에게 속한 `Notification`이 0건 이상 저장되어 있다.

### 종료 조건
- 성공: 요청자는 자신의 `Notification Inbox` 목록, `Unread Notification` 목록, `Unread Notification` 개수, `Notification` 상세를 확인한다.
- 실패: 비인증 조회 요청이 거절되거나, 존재하지 않거나 타인 소유인 `Notification` 상세 조회가 거절된다.

---
## 2. 흐름
### [Flow: `Notification Inbox` 목록 조회]
🟦 `Notification Inbox` 목록을 조회하라
→ 🟧 `Notification Inbox` 목록 조회가 요청되었다
→ 🟪 요청자가 인증된 경우
→ 🟦 Recipient 범위 `Notification Inbox` 목록을 최신순 페이지로 반환하라
→ 🟧 Recipient 범위 `Notification Inbox` 목록이 최신순 페이지로 반환되었다

---
### [Flow: `Unread Notification` 목록 조회]
🟦 `Unread Notification` 목록을 조회하라
→ 🟧 `Unread Notification` 목록 조회가 요청되었다
→ 🟪 요청자가 인증된 경우
→ 🟦 Recipient 범위 `Unread Notification` 목록을 반환하라
→ 🟧 Recipient 범위 `Unread Notification` 목록이 반환되었다

---
### [Flow: `Unread Notification` 개수 조회]
🟦 `Unread Notification` 개수를 조회하라
→ 🟧 `Unread Notification` 개수 조회가 요청되었다
→ 🟪 요청자가 인증된 경우
→ 🟦 Recipient 범위 `Unread Notification` 개수를 반환하라
→ 🟧 Recipient 범위 `Unread Notification` 개수가 반환되었다

---
### [Flow: `Notification` 상세 조회]
🟦 `Notification` 상세를 조회하라
→ 🟧 `Notification` 상세 조회가 요청되었다
→ 🟪 요청자가 인증된 경우
→ 🟦 `Notification` 상세 조회 대상을 식별하라
→ 🟧 `Notification` 상세 조회 대상이 식별되었다
→ 🟦 `Notification` 존재 여부를 확인하라
→ 🟧 `Notification` 존재 여부가 확인되었다
→ 🟪 조회 대상 `Notification`이 존재하는 경우
→ 🟦 `Notification` 소유권을 확인하라
→ 🟧 `Notification` 소유권이 확인되었다
→ 🟪 조회 대상 `Notification`이 요청자에게 속하는 경우
→ 🟦 `Notification` 상세를 반환하라
→ 🟧 `Notification` 상세가 반환되었다
→ 🟪 반환된 `Notification`이 `Unread` 상태인 경우
→ 🟦 `Notification`을 `Read` 상태로 변경하라
→ 🟧 `Notification`이 `Read` 상태로 변경되었다

---
### [Flow: 비인증 조회 요청 거절]
🟦 `Notification Inbox` 목록을 조회하라
→ 🟧 `Notification Inbox` 목록 조회가 요청되었다
→ 🟪 요청자가 인증되지 않은 경우
→ 🟧 `Notification Inbox` 목록 조회 요청이 인증 부족으로 거절되었다

🟦 `Unread Notification` 목록을 조회하라
→ 🟧 `Unread Notification` 목록 조회가 요청되었다
→ 🟪 요청자가 인증되지 않은 경우
→ 🟧 `Unread Notification` 목록 조회 요청이 인증 부족으로 거절되었다

🟦 `Unread Notification` 개수를 조회하라
→ 🟧 `Unread Notification` 개수 조회가 요청되었다
→ 🟪 요청자가 인증되지 않은 경우
→ 🟧 `Unread Notification` 개수 조회 요청이 인증 부족으로 거절되었다

🟦 `Notification` 상세를 조회하라
→ 🟧 `Notification` 상세 조회가 요청되었다
→ 🟪 요청자가 인증되지 않은 경우
→ 🟧 `Notification` 상세 조회 요청이 인증 부족으로 거절되었다

---
### [Flow: `Notification` 상세 조회 실패]
🟦 `Notification` 상세를 조회하라
→ 🟧 `Notification` 상세 조회가 요청되었다
→ 🟪 요청자가 인증된 경우
→ 🟦 `Notification` 상세 조회 대상을 식별하라
→ 🟧 `Notification` 상세 조회 대상이 식별되었다
→ 🟦 `Notification` 존재 여부를 확인하라
→ 🟧 `Notification` 존재 여부가 확인되었다
→ 🟪 조회 대상 `Notification`이 존재하지 않는 경우
→ 🟧 `Notification` 상세 조회가 미존재 대상으로 거절되었다

🟦 `Notification` 상세를 조회하라
→ 🟧 `Notification` 상세 조회가 요청되었다
→ 🟪 요청자가 인증된 경우
→ 🟦 `Notification` 상세 조회 대상을 식별하라
→ 🟧 `Notification` 상세 조회 대상이 식별되었다
→ 🟦 `Notification` 존재 여부를 확인하라
→ 🟧 `Notification` 존재 여부가 확인되었다
→ 🟪 조회 대상 `Notification`이 존재하는 경우
→ 🟦 `Notification` 소유권을 확인하라
→ 🟧 `Notification` 소유권이 확인되었다
→ 🟪 조회 대상 `Notification`이 요청자에게 속하지 않는 경우
→ 🟧 `Notification` 상세 조회가 소유권 불일치로 거절되었다

---
## 3. 도메인 요소 (통합)
|유형|내용|트리거|결과|시스템|비고|
|---|---|---|---|---|---|
|🟦|`Notification Inbox` 목록을 조회하라|`Notification Recipient`|`Notification Inbox` 목록 조회가 요청되었다|`Gateway 인증 계층`|목록 조회 시작 커맨드|
|🟦|Recipient 범위 `Notification Inbox` 목록을 최신순 페이지로 반환하라|요청자가 인증된 경우|Recipient 범위 `Notification Inbox` 목록이 최신순 페이지로 반환되었다|`Notification 조회 시스템`|최신순, 페이징, 요청자 범위 제한|
|🟦|`Unread Notification` 목록을 조회하라|`Notification Recipient`|`Unread Notification` 목록 조회가 요청되었다|`Gateway 인증 계층`|`Unread` 목록 조회 시작 커맨드|
|🟦|Recipient 범위 `Unread Notification` 목록을 반환하라|요청자가 인증된 경우|Recipient 범위 `Unread Notification` 목록이 반환되었다|`Notification 조회 시스템`|상태 변경 없이 반환|
|🟦|`Unread Notification` 개수를 조회하라|`Notification Recipient`|`Unread Notification` 개수 조회가 요청되었다|`Gateway 인증 계층`|`Unread` 개수 조회 시작 커맨드|
|🟦|Recipient 범위 `Unread Notification` 개수를 반환하라|요청자가 인증된 경우|Recipient 범위 `Unread Notification` 개수가 반환되었다|`Notification 조회 시스템`|상태 변경 없이 반환|
|🟦|`Notification` 상세를 조회하라|`Notification Recipient`|`Notification` 상세 조회가 요청되었다|`Gateway 인증 계층`|상세 조회 시작 커맨드|
|🟦|`Notification` 상세 조회 대상을 식별하라|요청자가 인증된 경우|`Notification` 상세 조회 대상이 식별되었다|`Notification 조회 시스템`|요청된 `Notification ID` 기준|
|🟦|`Notification` 존재 여부를 확인하라|`Notification` 상세 조회 대상이 식별되었다|`Notification` 존재 여부가 확인되었다|`Notification 조회 시스템`|미존재 상세 차단 전 단계|
|🟦|`Notification` 소유권을 확인하라|조회 대상 `Notification`이 존재하는 경우|`Notification` 소유권이 확인되었다|`Notification 조회 시스템`|요청자 소유 여부 확인|
|🟦|`Notification` 상세를 반환하라|조회 대상 `Notification`이 요청자에게 속하는 경우|`Notification` 상세가 반환되었다|`Notification 조회 시스템`|정상 상세 응답|
|🟦|`Notification`을 `Read` 상태로 변경하라|반환된 `Notification`이 `Unread` 상태인 경우|`Notification`이 `Read` 상태로 변경되었다|`Notification 조회 시스템`|조회 성공 후 해당 건만 변경|
|🟧|`Notification Inbox` 목록 조회가 요청되었다|`Notification Inbox` 목록을 조회하라|인증 정책 평가|`Gateway 인증 계층`|목록 조회 진입|
|🟧|Recipient 범위 `Notification Inbox` 목록이 최신순 페이지로 반환되었다|Recipient 범위 `Notification Inbox` 목록을 최신순 페이지로 반환하라|목록 응답 완료|`Notification 조회 시스템`|정상 목록 결과|
|🟧|`Unread Notification` 목록 조회가 요청되었다|`Unread Notification` 목록을 조회하라|인증 정책 평가|`Gateway 인증 계층`|`Unread` 목록 조회 진입|
|🟧|Recipient 범위 `Unread Notification` 목록이 반환되었다|Recipient 범위 `Unread Notification` 목록을 반환하라|`Unread` 목록 응답 완료|`Notification 조회 시스템`|상태 불변|
|🟧|`Unread Notification` 개수 조회가 요청되었다|`Unread Notification` 개수를 조회하라|인증 정책 평가|`Gateway 인증 계층`|`Unread` 개수 조회 진입|
|🟧|Recipient 범위 `Unread Notification` 개수가 반환되었다|Recipient 범위 `Unread Notification` 개수를 반환하라|`Unread` 개수 응답 완료|`Notification 조회 시스템`|상태 불변|
|🟧|`Notification` 상세 조회가 요청되었다|`Notification` 상세를 조회하라|인증 정책 평가|`Gateway 인증 계층`|상세 조회 진입|
|🟧|`Notification` 상세 조회 대상이 식별되었다|`Notification` 상세 조회 대상을 식별하라|존재 여부 확인|`Notification 조회 시스템`|상세 대상 식별 완료|
|🟧|`Notification` 존재 여부가 확인되었다|`Notification` 존재 여부를 확인하라|존재/미존재 정책 평가|`Notification 조회 시스템`|상세 조회 분기 기준|
|🟧|`Notification` 소유권이 확인되었다|`Notification` 소유권을 확인하라|소유/비소유 정책 평가|`Notification 조회 시스템`|수신자 범위 최종 검증|
|🟧|`Notification` 상세가 반환되었다|`Notification` 상세를 반환하라|`Read` 전환 정책 평가|`Notification 조회 시스템`|정상 상세 결과|
|🟧|`Notification`이 `Read` 상태로 변경되었다|`Notification`을 `Read` 상태로 변경하라|상세 조회 성공 종료|`Notification 조회 시스템`|반환된 단일 `Notification`만 변경|
|🟧|`Notification Inbox` 목록 조회 요청이 인증 부족으로 거절되었다|요청자가 인증되지 않은 경우|조회 거절 응답 반환|`Gateway 인증 계층`|비인증 목록 차단|
|🟧|`Unread Notification` 목록 조회 요청이 인증 부족으로 거절되었다|요청자가 인증되지 않은 경우|조회 거절 응답 반환|`Gateway 인증 계층`|비인증 `Unread` 목록 차단|
|🟧|`Unread Notification` 개수 조회 요청이 인증 부족으로 거절되었다|요청자가 인증되지 않은 경우|조회 거절 응답 반환|`Gateway 인증 계층`|비인증 `Unread` 개수 차단|
|🟧|`Notification` 상세 조회 요청이 인증 부족으로 거절되었다|요청자가 인증되지 않은 경우|조회 거절 응답 반환|`Gateway 인증 계층`|비인증 상세 차단|
|🟧|`Notification` 상세 조회가 미존재 대상으로 거절되었다|조회 대상 `Notification`이 존재하지 않는 경우|상세 조회 실패 응답 반환|`Notification 조회 시스템`|미존재 상세 차단|
|🟧|`Notification` 상세 조회가 소유권 불일치로 거절되었다|조회 대상 `Notification`이 요청자에게 속하지 않는 경우|상세 조회 실패 응답 반환|`Notification 조회 시스템`|타인 소유 상세 차단|
|🟪|요청자가 인증된 경우|각 조회 요청 이벤트|Recipient 범위 조회 처리|`Gateway 인증 계층`|공통 인증 통과|
|🟪|요청자가 인증되지 않은 경우|각 조회 요청 이벤트|인증 부족 거절 이벤트|`Gateway 인증 계층`|공통 인증 차단|
|🟪|조회 대상 `Notification`이 존재하는 경우|`Notification` 존재 여부가 확인되었다|`Notification` 소유권을 확인하라|`Notification 조회 시스템`|상세 조회 계속|
|🟪|조회 대상 `Notification`이 존재하지 않는 경우|`Notification` 존재 여부가 확인되었다|`Notification` 상세 조회가 미존재 대상으로 거절되었다|`Notification 조회 시스템`|상세 조회 실패|
|🟪|조회 대상 `Notification`이 요청자에게 속하는 경우|`Notification` 소유권이 확인되었다|`Notification` 상세를 반환하라|`Notification 조회 시스템`|정상 상세 허용|
|🟪|조회 대상 `Notification`이 요청자에게 속하지 않는 경우|`Notification` 소유권이 확인되었다|`Notification` 상세 조회가 소유권 불일치로 거절되었다|`Notification 조회 시스템`|타인 소유 상세 차단|
|🟪|반환된 `Notification`이 `Unread` 상태인 경우|`Notification` 상세가 반환되었다|`Notification`을 `Read` 상태로 변경하라|`Notification 조회 시스템`|조회 성공 후 단건 상태 전환|
|🟩|없음|없음|없음|외부|이 유스케이스는 외부 시스템 협력이 필요 없다|

---
## 4. 외부 시스템
|시스템|연동 목적|관련 유스케이스|비고|
|---|---|---|---|
|없음|없음|`UC-030`|내부 `Gateway 인증 계층`과 `Notification 조회 시스템`만 사용|

---
## 5. 규칙 (Invariant)
- 모든 조회 결과는 인증된 요청자의 `Recipient User ID` 범위로만 제한된다.
- `Notification Inbox` 목록 응답은 항상 최신순이며 페이징된 결과여야 한다.
- `Unread Notification` 목록 조회와 `Unread Notification` 개수 조회는 `Read`/`Unread` 상태를 변경하지 않는다.
- 성공한 `Notification` 상세 조회는 반환된 단일 `Notification`만 `Unread`에서 `Read`로 변경할 수 있다.
- 비인증 조회 요청은 `Notification 조회 시스템`으로 전달되기 전에 `Gateway 인증 계층`에서 차단된다.

---
## 6. 확인 필요
- 없음
