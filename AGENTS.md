# Agent Context

Write all agent input/output in English. Use `python3` for Python. If Python dependencies are needed, create/use `.venv/` at repository root.

This repo is a Gradle multi-module Java 21 / Spring Boot ticket-booking system. It uses DDD-style module boundaries and gateway-first client traffic.

## Fast Context
- Repo map: `docs/agent/context.md`
- Commands and verification: `docs/agent/commands.md`
- Current handoff state: `docs/agent/session-state.md`
- Token-reduction report: `docs/agent/token-reduction-report.md`
- Module-specific guidance: nearest nested `AGENTS.md`

Read only the smallest relevant context file. Prefer `rg`, targeted `sed`, Gradle task output summaries, and Serena symbol tools over full-file dumps.

## Hard Rules
- Do not print or commit secrets, especially `application-secret.yml`.
- Keep `app/` orchestration-only: config imports/wiring, no business logic/controllers/repositories.
- Keep service domain/app layers free of web types such as servlet request/response and controllers.
- Client traffic enters through `platform/gateway` on port `8080`; internal services register through Eureka.
- Treat `build/` and `bin/` as generated/non-source unless they are under `src/main` or `src/test`.

## PR Body Requirements
Each PR must include:
- Implementation intent: what the change is intended to implement.
- Implementation approach: how data flow or logic flow works.
- Verification method: how changes were tested and why safe to merge.
- Also include `## Summary`, `## Changes`, `## Verification`, and `## Risks and Rollback`.

## Output Budget
- Cap routine command output near 4k tokens.
- Use `git status --porcelain=v1 -uno`.
- Use `git diff --stat` before targeted file diffs.
- For logs/tests, summarize key lines instead of pasting full output.
