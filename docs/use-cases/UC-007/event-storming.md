<!-- harness-reverse-engineered:v1 -->
# Event Storming
Commands: Join Queue, Poll Queue, Promote Entrant. Events: User Queued, Position Returned, User Promoted, Entry Token Issued. Policy: if slot available, promote next user. Systems: broker, dispatcher, event, Redis. Invariant: token has TTL.
