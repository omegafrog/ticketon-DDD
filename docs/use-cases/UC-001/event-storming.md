<!-- harness-reverse-engineered:v1 -->
# Event Storming
Command: Register User. Event: User Registration Requested; Security User Registered; User Profile Created. Policy: when auth registration commits, publish profile creation message. Systems: auth, user, RabbitMQ, MySQL. Invariant: public registration creates USER role.
