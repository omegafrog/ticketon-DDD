# Session Token-Efficiency Analysis

## 1. Scope

This report analyzes the 2026-06-19 session that attempted to implement active ChangeSet `CHG-20260619-001`, with emphasis on:

- parent-agent discovery, approval handling, orchestration, and recovery;
- harness runs `run-52748bad6665` and `run-221e9f6a1bfd`;
- UC-030 planning, security review, artifact review, execution, and verification attempts;
- token accounting quality and workflow-level waste.

Exact token counters exist only in nested Codex JSON event streams. Parent-chat token use is not exposed in repository artifacts. Therefore, exact totals below cover harness subprocesses only. Parent-session observations are qualitative unless explicitly labeled as estimates.

## 2. Executive Assessment

Token efficiency was poor despite a high prompt-cache hit rate.

The two harness runs processed **12,459,290 input tokens**, including **11,685,504 cached input tokens**, and emitted **77,944 output tokens**. Non-cached input was still **773,786 tokens**. Overall cache hit rate was **93.79%**.

High cache reuse reduced marginal input cost, but did not make execution efficient. The executor repeatedly carried a growing transcript through 119 completed shell commands and 56 agent messages. UC-030 executor alone processed 10.75 million input tokens, then failed a deterministic scope gate that could have been checked before implementation. A second full workflow launch then spent another 581,640 input tokens even though its planner failed immediately on quota.

Core diagnosis:

1. **Late deterministic validation** caused most avoidable waste.
2. **Fresh-run semantics ignored valid stage artifacts** and repeated planning/review work.
3. **Failure propagation was broken or insufficiently strict** in second run.
4. **Executor interaction granularity was too fine**, causing quadratic transcript growth.
5. **Verification prerequisites were not preflighted** before expensive implementation.
6. **Usage/report telemetry was internally available but discarded from summary files.**

Result: substantial implementation progress, zero completed use cases, ChangeSet still active.

## 3. Measured Token Use

### 3.1 First Run: `run-52748bad6665`

| Stage | Input | Cached input | Non-cached input | Output | Reasoning output | Completed commands |
|---|---:|---:|---:|---:|---:|---:|
| Plan work item | 628,004 | 493,056 | 134,948 | 13,048 | 4,821 | 34 |
| Security review | 344,250 | 289,280 | 54,970 | 7,385 | 3,330 | 19 |
| Artifact review | 154,567 | 143,232 | 11,335 | 1,691 | 568 | 7 |
| Execute work item | 10,750,829 | 10,277,888 | 472,941 | 44,490 | 12,306 | 119 |
| **Total** | **11,877,650** | **11,203,456** | **674,194** | **66,614** | **21,025** | **179** |

Executor accounted for **90.51%** of first-run input tokens. Its cache hit rate was **95.60%**, yet it still consumed 472,941 non-cached input tokens.

### 3.2 Second Run: `run-221e9f6a1bfd`

| Stage | Input | Cached input | Non-cached input | Output | Reasoning output | Completed commands |
|---|---:|---:|---:|---:|---:|---:|
| Plan work item | unavailable | unavailable | unavailable | unavailable | unavailable | 0 |
| Security review | 310,185 | 255,104 | 55,081 | 6,898 | 3,816 | 20 |
| Artifact review | 271,455 | 226,944 | 44,511 | 4,432 | 1,685 | 19 |
| **Measured total** | **581,640** | **482,048** | **99,592** | **11,330** | **5,501** | **39** |

Planner exited with usage-limit error before reporting token counters. Security and artifact reviewers still ran, consuming 581,640 input tokens without a valid new planner result. Treat nearly all second-run measured usage as avoidable orchestration waste.

### 3.3 Combined Harness Total

| Metric | Total |
|---|---:|
| Input tokens processed | 12,459,290 |
| Cached input tokens | 11,685,504 |
| Non-cached input tokens | 773,786 |
| Output tokens | 77,944 |
| Reasoning output tokens | 26,526 |
| Cache hit rate | 93.79% |
| Completed shell commands | 218 |

These figures undercount total session usage because parent-agent conversation and the failed second-run planner expose no counters.

## 4. Execution Timeline and Efficiency

### 4.1 Discovery and Approval

Parent agent correctly found one active ChangeSet and three affected use cases. It also found pending technical-decision approvals and waited for explicit approval before changing status.

Efficiency positives:

- targeted `rg`/document reads instead of broad repository dumps;
- one approval covered UC-030, UC-031, and UC-032;
- approval edits were small and parser-compatible (`Pending Decisions: - None`).

Efficiency concern:

- workflow readiness was not fully validated after approval. `affected-files.md` still contained placeholders, Docker was unavailable, and repository-wide gates already had unrelated failures. All three were discoverable before an LLM executor started.

### 4.2 Planning and Review

UC-030 plan grew to roughly 26 KB / 2,704 words. Relevant UC-030 source documents add roughly 35 KB. Every stage also loaded agent instructions, skill instructions, repository rules, ChangeSet context, and growing tool transcript.

Planning used 628,004 input tokens across 34 commands. This is high for producing one 26 KB plan. Security review then used 344,250 input tokens, followed by 154,567 for artifact review.

Review loop found real defects, including an incorrect foreign-access verification and later a missing debugger-based runtime verification requirement. Reviews added value. Cost problem came from ordering and repetition:

- machine-checkable plan contract and affected-file scope were not validated before semantic reviewers;
- security and artifact reviewers independently re-read overlapping context;
- review rejection triggered expensive replanning rather than a narrow patch request;
- review state was overwritten into one shared `plan-review.md`, obscuring chronology and making recovery harder.

### 4.3 Implementation

Executor produced meaningful UC-030 code, tests, and runtime scripts. Focused notification tests and ArchUnit passed.

However, executor used 119 completed commands. Coarse command distribution:

- 64 `sed` reads;
- 20 `rg` searches;
- 7 focused notification test runs;
- 2 full builds;
- 2 ArchUnit runs;
- additional repository tests, Semgrep, Docker checks, runtime launch, evidence generation, and status inspection.

This command pattern indicates incremental rediscovery. Each new turn replayed most prior transcript through model context. Cached input softened cost but did not prevent 10.75 million processed tokens.

Seven focused test executions were excessive unless each followed a meaningful code change. Full build and full test both hit the same known `nplus1-test` compile failure, duplicating an 81-second failure path. ArchUnit also ran twice. Docker availability was checked late, after runtime scripts and substantial verification preparation.

### 4.4 Late Scope Failure

Executor completed implementation, then scope validator blocked four files:

- `docs/plans/active/UC-030/verification.md`;
- `notification/build.gradle`;
- `NotificationCommandService.java`;
- `NotificationListProjection.java`.

The source of truth, `docs/use-cases/UC-030/affected-files.md`, still had incomplete placeholders. Parent agent repaired it only after executor finished.

This was largest preventable failure. Scope validation depends on static file declarations and could run before executor launch. Even better, planner should generate exact allowed paths and validator should reject placeholders before semantic review.

### 4.5 Verification Blockers

Three blockers surfaced after expensive implementation:

1. Full build/test: out-of-scope `nplus1-test` compile error at `EventManagerEventsNoNPlusOneTest.java:123`.
2. Semgrep: two existing out-of-scope findings in purchase module.
3. Runtime/E2E: Docker CLI unavailable in current WSL distro.

All were preflightable. A deterministic preflight should have classified them as baseline/environment blockers before any coding agent ran. That would allow either:

- user-approved scoped verification policy;
- blocker remediation ChangeSet;
- immediate stop before token-heavy execution.

### 4.6 Retry and Quota Failure

After repairing affected-file scope, parent agent launched a new `implementation --apply` run. New run restarted planning instead of resuming from repaired UC-030 artifacts.

Planner immediately hit usage limit. Despite that, security and artifact reviewer stages consumed another 581,640 input tokens. State then marked all three use cases blocked with same UC-030 planner error, even though UC-031 and UC-032 had not started.

This reveals four runtime defects:

- no quota preflight;
- no strict downstream cancellation after dependency failure;
- no artifact reuse/resume from last valid checkpoint;
- blocker propagation incorrectly paints untouched work items as attempted failures.

## 5. Token Accounting Defects

`usage-*.json` files contain `null` for every token field even though `stdout.txt` contains exact `turn.completed.usage` data. Reports also omit token totals, cache rates, elapsed time, command count, and stage cost.

`report.md` shows only generic `blocked` status. It omits failed stage and next command. `started_at` and `completed_at` are null. `artifacts.json` references paths that do not exist, while real step artifacts live elsewhere.

Consequences:

- operators cannot see expensive stages without custom parsing;
- automated token budgets cannot stop runaway agents;
- retries cannot optimize based on prior cost;
- cache-hit percentage may create false confidence because total processed context remains enormous;
- no reliable cost-per-completed-work-item KPI exists.

Existing `docs/agent/token-reduction-report.md` measures static word counts only. That is useful for bootstrap context, but insufficient for runtime efficiency. It should not be interpreted as end-to-end token optimization evidence.

## 6. Root Causes

### 6.1 Quadratic Transcript Growth

Executor's 119 command completions and 56 agent messages accumulated into one long session. If each turn resends prior transcript, total processed input grows approximately with sum of transcript prefixes, not final prompt size. This explains 10.75 million input tokens from an initial prompt of only about 19 KB.

### 6.2 Overly Broad Agent Responsibilities

One executor handled code discovery, implementation, unit tests, architecture checks, full build, full test, Semgrep, runtime launcher design, Docker diagnosis, evidence authoring, plan updates, and final reporting. Broad scope increased tool turns and context retention.

### 6.3 LLM Use for Deterministic Checks

Placeholders, missing paths, Docker absence, known baseline build failures, and Semgrep baseline findings do not require LLM reasoning. Discovering them late paid premium model cost for validator work.

### 6.4 Fresh Execution Instead of Incremental Resume

Second run repeated plan-security-review chain. Runtime had checkpoints and provider session IDs but did not use them to resume from failed scope gate.

### 6.5 High Reasoning Effort on Routine Stages

Planner and security reviewer used `model_reasoning_effort="high"`. Artifact reviewer and executor used `medium`. High effort may be justified for difficult architecture decisions, but routine regeneration and checklist augmentation should use lower-cost modes after first accepted plan.

### 6.6 Weak Baseline Isolation

Repo-wide tests and Semgrep could not distinguish pre-existing failures from ChangeSet regressions. Executor reran known failing global gates, then could not complete plan despite focused tests passing.

## 7. Recommended Changes

### P0: Before Next Run

1. Add deterministic preflight before any agent stage:
   - exact affected-file syntax and path existence;
   - no placeholders;
   - technical decisions approved;
   - Docker/required tools present;
   - baseline full build/test/static-analysis status captured once;
   - quota/provider availability checked where API permits.
2. Stop dependency graph immediately when any required stage fails. Never run security/review after planner failure.
3. Resume UC-030 from failed scope/verification checkpoint. Do not regenerate accepted plan unless source inputs changed.
4. Parse `turn.completed.usage` into `usage-*.json` and aggregate into run report.

### P1: Reduce Executor Cost

1. Split executor into bounded phases with compact handoffs:
   - code discovery and patch plan;
   - implementation;
   - focused verification;
   - global/runtime verification.
2. Limit each phase to one structured repository snapshot. Prefer batched reads over 64 separate `sed` calls.
3. Run focused tests once after coherent patch batch, then once after fixes. Target maximum: three focused runs.
4. Cache known baseline failures by Git HEAD + command hash. Skip identical global command reruns until relevant files change.
5. Check `docker --version` and required ports before generating runtime evidence.
6. Generate evidence files mechanically from command results, not through repeated agent-authored shell heredocs.

### P1: Improve Planning Pipeline

1. Run contract/schema validator before security and semantic review.
2. Make security reviewer return a patch/delta, not rewrite or reread full plan when no blocker exists.
3. Preserve immutable review history per attempt: `plan-review-attempt-01.md`, etc.
4. On review rejection, send planner only blocking findings plus relevant plan sections.
5. Use content hashes. Skip stage when inputs and accepted output hashes match prior run.

### P2: Improve Model Routing

1. Use smaller/lower-effort model for contract checks, scope classification, and review-format validation.
2. Reserve high reasoning for first architecture plan or genuine ambiguity.
3. Downgrade repeated plan patch attempts to medium/low reasoning.
4. Add hard per-stage token budgets and command limits with explicit escalation.

## 8. Proposed Efficiency Budgets

Suggested initial budgets per UC:

| Stage | Non-cached input target | Output target | Command target |
|---|---:|---:|---:|
| Planner | <= 60,000 | <= 6,000 | <= 15 |
| Security review | <= 25,000 | <= 3,000 | <= 8 |
| Artifact review | <= 20,000 | <= 2,000 | <= 6 |
| Executor | <= 180,000 | <= 20,000 | <= 50 |
| Retry after deterministic failure | <= 10,000 | <= 2,000 | <= 8 |

First-run non-cached input was 674,194 versus proposed 285,000 UC budget: **2.37x target**. Executor non-cached input was 472,941 versus 180,000 target: **2.63x target**.

Budgets should trigger compact checkpointing, not silent termination. Checkpoint must record completed tasks, changed files, last passing tests, blockers, and exact resume stage.

## 9. Success Metrics

Track per run and per work item:

- total, cached, and non-cached input tokens;
- output and reasoning tokens;
- tokens per completed checklist task;
- tokens per changed production line and test line;
- command count and duplicate command count;
- stage retry count and reused-stage count;
- baseline failures avoided through cache;
- deterministic-gate failures detected before first agent call;
- cost per completed work item;
- percentage of runs resumed versus restarted.

Near-term targets:

- 100% usage files populated;
- zero downstream stages after failed dependency;
- zero executor scope failures caused by placeholder declarations;
- >= 60% reduction in non-cached tokens per UC;
- <= 50 executor commands for comparable backend UC;
- no repeated global gate on unchanged Git state;
- run report includes failed stage, blocker, token totals, elapsed time, and exact resume command.

## 10. Final Judgment

Session was technically productive but operationally inefficient. Reviews caught legitimate issues and executor delivered substantial UC-030 code, but workflow spent model capacity discovering deterministic blockers too late. High cache hit masked extreme transcript amplification. Retry behavior then consumed more tokens after quota failure without advancing state.

Highest-value fix is not shorter prose. It is workflow restructuring:

**preflight deterministic constraints -> reuse accepted artifacts -> execute in bounded phases -> verify incrementally -> resume exact failed stage -> report real token data.**

Until these changes land, rerunning full `implementation --apply` risks repeating planning/review cost and exhausting quota before UC-031 or UC-032 begins.
