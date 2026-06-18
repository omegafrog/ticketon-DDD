<!-- harness-reverse-engineered:v1 -->
# Event Storming
Commands: Search Events, View Event, List Categories, Fetch Image. Event: Event Viewed. Policy: when detail is viewed, increment Redis view count. Systems: gateway, event, app, Redis, MySQL. Invariant: deleted events excluded.
