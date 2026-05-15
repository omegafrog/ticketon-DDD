# Security AOP Module

Purpose: reusable security annotations/aspects and logged-in user context.

Look here:
- Aspects/annotations: `security-aop/src/main/java/org/codenbug/securityaop/aop/`
- User context: `security-aop/src/main/java/org/codenbug/securityaop/aop/LoggedInUserContext.java`
- Sensitive config: `security-aop/src/main/resources/application-secret.yml`

Rules:
- Keep module generic; avoid service-specific dependencies.
- Aspects should fail closed.
- Do not parse tokens ad hoc in each controller when shared aspect fits.

Command:
- `./gradlew :security-aop:test`
