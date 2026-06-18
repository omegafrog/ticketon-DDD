<!-- harness-reverse-engineered:v1 -->
# Ubiquitous Language

| Term | Meaning | Evidence |
|---|---|---|
| Security User | Authentication account, role, status, credentials | auth/domain/SecurityUser.java |
| User | Customer profile linked to security identity | user/domain/User.java |
| Event | Ticketed performance and booking policy | event/domain/Event.java |
| Seat Layout | Event seating arrangement and seat availability | seat/domain/SeatLayout.java |
| Queue Entry | Redis waiting registration for one user/event | broker/infra/WaitingQueueRedisRepository.java |
| Entry Token | Time-limited permission to proceed toward purchase | broker/app/PollingEntryDispatchService.java |
| Purchase | Payment-bearing reservation record | purchase/domain/Purchase.java |
| Ticket | Issued item for a confirmed purchase | purchase/domain/Ticket.java |
| Refund | Full-payment reversal request and result | purchase/domain/Refund.java |
| Notification | User-addressed persisted message | notification/domain/entity/Notification.java |
| Manager | Role permitted to operate owned events | platform/common/src/main/java/org/codenbug/common/Role.java |
| Administrator | Role permitted to operate backoffice accounts | auth/ui/AdminBackofficeController.java |
| Payment Provider | External PG implementation, currently Toss adapter | purchase/infra/TossPaymentPgApiService.java |
