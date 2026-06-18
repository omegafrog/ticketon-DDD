<!-- harness-reverse-engineered:v1 -->
# Event Storming
Commands: Request Payment Confirmation, Process Confirmation. Events: Confirmation Accepted, Payment Confirmed or Failed, Tickets Issued. Policy: deduplicate message/command before PG call. Systems: purchase, RabbitMQ, Toss, seat, notification.
