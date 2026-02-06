# Verification Commands (No Auto-Run)

This project can freeze WSL if too much output/work runs at once.

This file lists recommended verification commands, but they should be run manually.

## Recommended (Minimal)

```bash
./gradlew :platform:common:test :broker:test
```

## Full Module Tests

```bash
./gradlew :platform:common:test :broker:test :dispatcher:test :seat:test :purchase:test
```

## Compile-Only (Faster Than Tests)

```bash
./gradlew :platform:common:compileJava :broker:compileJava :dispatcher:compileJava :seat:compileJava :purchase:compileJava
```

## Notes

- Do NOT run k6 scripts unless explicitly requested.
- If WSL is sensitive, prefer running one module at a time.
