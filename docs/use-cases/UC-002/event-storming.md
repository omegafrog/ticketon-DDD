<!-- harness-reverse-engineered:v1 -->
# Event Storming
Commands: Log In, Start Social Login, Complete Social Login. Events: Credentials Accepted, Social Identity Verified, Tokens Issued. Policy: if account unusable, reject login. Systems: auth, OAuth provider, Redis. Invariant: only enabled non-deleted accounts receive tokens.
