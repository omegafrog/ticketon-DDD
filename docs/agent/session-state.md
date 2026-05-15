# Agent Session State

Use this file as a compact handoff between long sessions. Keep it current when work spans multiple turns or agents.

## Active Goal

Reduce agent context/token usage in this repo with repo-isolated docs and compact `AGENTS.md` files.

## Current Changes

- Root `AGENTS.md` now contains hard rules and pointers only.
- Nested `AGENTS.md` files are compact module guides.
- Detailed reusable context lives under `docs/agent/`.
- Report for harness reuse lives at `docs/agent/token-reduction-report.md`.

## Verification Snapshot

Update after each relevant run:
- Word count before compaction: 7,203 words across 21 `AGENTS.md` files.
- Word count after compaction: 1,472 words across 21 `AGENTS.md` files.
- New cold-path docs under `docs/agent/`: 1,454 words total.
- Net hot-path reduction: 5,731 words, about 79.6%.

## Next Useful Commands

```bash
find . -name AGENTS.md -print | sort | xargs -r wc -w
git diff --stat
git status --porcelain=v1 -uno
```

## Open Risks

- `AGENTS.md` compaction changes local agent context only; it does not affect application runtime.
- If an agent needs full repo orientation, it must read `docs/agent/context.md` explicitly.
