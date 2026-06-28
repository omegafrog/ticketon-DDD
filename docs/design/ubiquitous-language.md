# Project Context

## 1. Ubiquitous Language

| Canonical Term | Korean | English | Type | Definition | Aliases | Forbidden Terms | Source |
|---|---|---|---|---|---|---|---|
| Notification | 알림 | Notification | Domain Concept | A persisted message addressed to one Notification Recipient. It contains a type, title, content, optional target URL, read state, and recipient identifier. In this ChangeSet, `Notification` always means a persisted notification; real-time push delivery is outside its meaning boundary. | persisted notification | push notification, message | 요구사항 Scope, FR-001–FR-004 |
| Notification Recipient | 알림 수신자 | NotificationRecipient | Actor Role | The authenticated user to whom a Notification is addressed. The recipient may view and delete only Notifications addressed to their own user ID. This role does not define a separate User/Profile domain object inside the Notification bounded context. | recipient, owner | user, notification user | 요구사항 Scope, FR-001, FR-002 |
| Notification Sender | 알림 발신자 | NotificationSender | Actor Role | An authenticated ADMIN or MANAGER acting in the role that manually creates one Notification for a specified Notification Recipient. It does not include payment/refund listener inputs. | sender, ADMIN, MANAGER | creator, author | 요구사항 Scope, FR-003 |
| Notification Inbox | 알림함 | NotificationInbox | User-Visible Concept | The recipient-scoped view of persisted Notifications addressed to one Notification Recipient. It supports all, unread, count, detail, and deletion operations but is not a separate stored container. | inbox | mailbox, notification box | 요구사항 Scope, FR-001, FR-002 |
| Unread | 읽지 않음 | Unread | State Label | Notification state before successful detail retrieval by its Notification Recipient. List, unread-list, and unread-count retrieval do not change this state. | unread notification | new, unopened | 요구사항 FR-001, FR-003 |
| Read | 읽음 | Read | State Label | Notification state after successful detail retrieval by its Notification Recipient. Only the retrieved Notification changes from Unread to Read. | read notification | checked, opened | 요구사항 FR-001 |
| Recipient User ID | 수신자 사용자 ID | RecipientUserId | Identifier | The user ID value identifying the Notification Recipient. It is used for addressing, ownership isolation, and recipient-scoped lookup; it is not a User/Profile existence reference owned by the Notification bounded context. | userId, recipientId | ownerId, targetUserId | 요구사항 Scope, FR-001–FR-003 |
| Source Key | 소스 키 | SourceKey | Identifier | A deterministic identifier derived from an approved payment/refund listener payload and used to prevent duplicate Notification rows when the same source payload is replayed. | deterministic source key | eventId, messageId | 요구사항 FR-004 |

## 2. Naming Rules

- Documents must use `Canonical Term`.
- Code class, method, package, command, event, and policy identifiers must use `English`.
- User-facing text should use `Korean`.
- `Forbidden Terms` must not be used in new documents, plans, tests, or code identifiers.
- Aliases are recorded only for migration/search context and must not be introduced as new canonical language.
- Use `Notification Recipient` for the actor role and `Recipient User ID` for its identifier.
- Use ownership only to describe the recipient-addressing rule; do not introduce `Owner` as a separate actor or domain concept.
- Use `Notification` without a `Persisted` prefix inside this ChangeSet because persistence is part of its confirmed meaning boundary.

## 3. Blocking Open Language Questions

- None.

## 4. Deferred Language Questions

- Aggregate, domain event, command, and state-transition names are deferred to event storming and DDD design.
