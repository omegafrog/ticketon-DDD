<!-- harness-reverse-engineered:v1 -->
# Event Storming
Commands: Refresh Tokens, Log Out. Events: Tokens Refreshed, Refresh Token Blacklisted, User Logged Out. Policy: if refresh token is invalid or blacklisted, deny refresh. Systems: auth, Redis, gateway.
