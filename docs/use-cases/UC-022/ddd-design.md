<!-- harness-reverse-engineered:v1 -->
# DDD Design
Aggregates: Refund, Purchase. VOs: RefundAmount, RefundReason. ManagerRefundService verifies ownership and invokes RefundDomainService/PG/message adapters.
