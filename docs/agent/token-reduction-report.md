# Token Reduction Report

## Baseline

- `AGENTS.md` word count before bootstrap: 249 words.

## Result

- `AGENTS.md` word count after bootstrap: 249 words.
- Existing root `AGENTS.md` was preserved because it was not harness-managed.
- Detailed repo context now lives under `docs/agent/`.
- Analyzer mode: static repository scan.
- LLM summary status: completed.
- Detected technologies: Java/Gradle.

## Agent Doc Counts

- `docs/agent/codebase-artifacts.md`: 164 words
- `docs/agent/commands.md`: 204 words
- `docs/agent/context.md`: 290 words
- `docs/agent/design-conformance-report.md`: 115 words
- `docs/agent/session-state.md`: 100 words
- `docs/agent/token-reduction-report.md`: 99 words

## Verification Commands

```bash
find . -name AGENTS.md -print | sort | xargs -r wc -w
wc -w docs/agent/*.md
rg -n -P "\p{Hangul}" AGENTS.md docs/agent || true
git diff --stat
git status --porcelain=v1 -uno
```
