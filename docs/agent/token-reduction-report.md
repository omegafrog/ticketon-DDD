# Agent Token Reduction Report

Date: 2026-05-14
Repo: `/home/jiwoo/workspace/ticketon-DDD`
Scope: repo-isolated agent-context changes only.

## Executive Summary

Previous local sessions spent most tokens on repeated context and oversized tool output, not on new reasoning. Existing cache hit rate was already high, but useful context was bloated by long `AGENTS.md` files, full skill bodies, large docs reads, and unbounded command output.

This change makes the repo cheaper for future agents by keeping hot-path instructions short and moving bulky reference material into explicit, opt-in docs under `docs/agent/`.

## Baseline Observations

Sampled 3 largest local rollout sessions for this repo:

| Metric | Approx Value |
| --- | ---: |
| Total tokens | 106.7M |
| Input tokens | 106.3M |
| Cached input tokens | 101.9M |
| Cache hit rate | 95.9% |
| Output tokens | 312k |
| Reasoning tokens | 50k |
| `exec_command` calls | 868 |
| `write_stdin` calls | 231 |
| Compactions | 8 |

Main waste sources:
- `AGENTS.md` stack: 21 files, 7,203 words before compaction.
- Docs corpus: 15 markdown files, about 42,753 words.
- Repeated skill invocations: `harness-plan-executor` 15 times, `harness-bootstrap` 6 times, `harness-requirements-usecases` 6 times.
- Large command outputs, including single outputs near 54k, 30k, 18k, and 17k tokens.

## Implemented Changes

1. Root `AGENTS.md` was reduced to hard rules, pointers, PR requirements, and output-budget rules.
2. Nested `AGENTS.md` files were reduced to compact module purpose, lookup paths, local rules, and command hints.
3. Bulky shared context moved to `docs/agent/context.md`.
4. Reusable command and runtime verification guidance moved to `docs/agent/commands.md`.
5. Compact handoff file added at `docs/agent/session-state.md`.
6. This report added for reuse by harness and other repositories.

Measured result:

| Area | Before | After |
| --- | ---: | ---: |
| `AGENTS.md` word count | 7,203 | 1,472 |
| Hot-path reduction | - | 5,731 words / 79.6% |
| New cold-path docs | - | 1,538 words |

The detailed context is still available, but future sessions do not pay for it unless they need it.

## Design Pattern

Use a two-tier context model:

| Tier | Content | Read Frequency |
| --- | --- | --- |
| Hot path | Hard rules, hazards, exact pointers | Every session |
| Cold path | Architecture map, commands, reports, long policies | Only when needed |

`AGENTS.md` should be hot-path only. Detailed reference docs should live in repo-local files and be referenced by path.

## Recommended Harness Template

For other repositories, generate:

```text
AGENTS.md
docs/agent/context.md
docs/agent/commands.md
docs/agent/session-state.md
docs/agent/token-reduction-report.md
```

Root `AGENTS.md` should contain:
- Language/output policy.
- Tool/runtime policy.
- 5-10 hard repo rules.
- Pointers to cold-path docs.
- PR body requirements.
- Output budget defaults.

Nested `AGENTS.md` should contain only:
- Module purpose.
- 4-8 lookup paths.
- 3-6 module-specific rules.
- 1-3 commands.

## MCP and Skill Recommendations

Use Serena MCP for code navigation:
- Activate project once.
- Use symbol overview/reference tools before reading whole source files.
- Store repo memories for architecture, test commands, runtime ports, and current decisions.

Use Graphify when architecture and file relationship queries recur:
- Build graph once.
- Query graph instead of repeatedly scanning docs/source.
- Keep graph output out of hot prompt unless specifically needed.

Use Caveman mode for routine work:
- Reduces assistant output tokens.
- Keep detailed explanations in reports, not every chat turn.

Refactor heavy skills:
- Skill body should be short workflow only.
- Put long standards/examples in referenced files.
- Load reference files by need, not on every invocation.

## Command Output Policy

Default command shape:

```bash
git status --porcelain=v1 -uno
git diff --stat
rg -n "pattern" path -m 80
sed -n '120,220p' file
./gradlew :module:test --console=plain
```

For large logs:

```bash
command > /tmp/task.log 2>&1
tail -120 /tmp/task.log
rg -n "FAILED|Exception|BUILD FAILED" /tmp/task.log -m 80
```

For Gradle test results, parse XML or summarize failing test names instead of dumping full reports.

## Cache Hit Guidance

Cache hit was already high because repeated context stayed stable. The bigger win is reducing total input size and avoiding unique oversized tool outputs.

Do:
- Keep root instructions stable.
- Put volatile task details near the end of prompt.
- Avoid editing hot context files during unrelated work.
- Reuse same model/settings for long tasks.

Avoid:
- Full `git status` in dirty worktrees.
- Full docs/source dumps.
- Re-reading skill files every turn.
- Including generated summaries/logs directly in prompt.

## Expected Impact

Expected impact for future repo sessions:
- Lower initial context cost from compact `AGENTS.md` stack.
- Fewer compactions during long implementation loops.
- Better cache usefulness because large one-off outputs stay out of context.
- Easier transfer to harness because pattern is repo-local and does not require global Codex config.

## Rollback

Runtime rollback is not needed; changes affect only agent guidance docs.

If needed, restore previous `AGENTS.md` content from git history and remove `docs/agent/`.
