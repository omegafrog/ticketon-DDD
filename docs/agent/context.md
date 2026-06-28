# Agent Context Map

## Repository Purpose

Make existing persisted notification features operational

## Static Analysis

- Description: Make existing persisted notification features operational
- Technologies: Java/Gradle
- Manifests: `build.gradle`, `settings.gradle`
- Source roots: `app`, `harness_codex`
- Test roots: `tests`
- Docs roots: `docs`, `README.md`, `AGENTS.md`
- Config files: `.codex/config.toml`, `.codex/openai.yaml`, `.github`, `.gitignore`, `.harness/workflows`, `AGENTS.md`
- Workflow docs: `docs/design`, `docs/changes`, `docs/use-cases`, `docs/maintenance`, `docs/plans`, `.harness/workflows`
- Commands:
  - List files: `rg --files`
  - Search text: `rg -n "<pattern>"`
  - Git status: `git status --porcelain=v1 -uno`
  - Diff stat: `git diff --stat`
  - Python tests: `./venv/bin/python3 -m pytest -q -s`
  - Harness CLI help: `python3 -m harness_codex --help`
  - Gradle tests: `./gradlew test`

## Main Paths

- `AGENTS.md`: hot-path agent rules.
- `docs/agent/`: cold-path agent context, commands, session state, and token reports.
- `docs/design/`: canonical requirements and design documents when present.
- `docs/changes/`: active and completed ChangeSet documents when present.
- `docs/use-cases/`: executor-facing use-case slices when present.
- `docs/maintenance/`: executor-facing maintenance slices when present.
- `docs/plans/`: active and completed implementation plans when present.
- Source and test paths: discover with `rg --files`, package manifests, and build config.

## Context Loading Guidance

Start with the nearest `AGENTS.md`, then read only the smallest relevant file from `docs/agent/`. Prefer targeted search and symbol tools. Avoid broad design-doc or source dumps unless needed for the current decision.

## Harness Workflow Guidance

When ChangeSet docs exist, use the active ChangeSet and selected work-item slice as the primary scope. Read canonical design docs only when the slice points there or shared design context is required.
