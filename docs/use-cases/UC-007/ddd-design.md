<!-- harness-reverse-engineered:v1 -->
# DDD Design
Implemented model is Redis queue state, not QueueEntryAggregate. Services: PollingWaitingQueueService, PollingEntryDispatchService, dispatcher workers. EventClient supplies event status/capacity.
