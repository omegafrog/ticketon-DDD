# Agent Commands

## Discovery

- List files: `rg --files`
- Search text: `rg -n "<pattern>"`
- Git status: `git status --porcelain=v1 -uno`
- Diff stat: `git diff --stat`
- Python tests: `./venv/bin/python3 -m pytest -q -s`
- Harness CLI help: `python3 -m harness_codex --help`
- Gradle tests: `./gradlew test`

## Harness Commands

- Start requirements stage: `python3 -m harness_codex requirements-definition <CHG-ID> --title "<title>" --idea "<idea>"`
- List active ChangeSets: `python3 -m harness_codex changes list`
- Initialize repo context: `python3 -m harness_codex init --description "<repo description>"`
- Create ChangeSet and run affected workflows: `python3 -m harness_codex ultrawork --title "<title>" --preview`
- Run use-case stage: `python3 -m harness_codex use-case-definition <CHG-ID>`
- Preview ChangeSet implementation with one execution loop per affected UC: `python3 -m harness_codex implementation <CHG-ID> --preview`
- Bootstrap agent context: `python3 -m harness_codex agent-context init --description "<repo description>"`


## Agent Context Verification

```bash
find . -name AGENTS.md -print | sort | xargs -r wc -w
wc -w docs/agent/*.md
rg -n -P "\p{Hangul}" AGENTS.md docs/agent || true
git diff --stat
git status --porcelain=v1 -uno
```

## Output Budget

Use concise status commands first. Use diff stats before targeted diffs. Summarize logs and failures instead of pasting full output. Cap routine command output near 4k tokens.
