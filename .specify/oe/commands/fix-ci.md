# Fix CI

When the user invokes `/fix-ci` (optionally with arguments), perform an
**autonomous, async-first** CI remediation loop that:

- Downloads CI failure logs and artifacts (screenshots, traces)
- Diagnoses failures using evidence (logs, screenshots, code)
- Applies targeted, minimal fixes
- Validates locally before pushing
- Pushes, waits 15 minutes, and re-checks CI
- Iterates until CI is green or escalation is required

This command is **action-oriented and autonomous**: it runs the full loop
without user interaction unless it hits an escalation boundary. The user can
walk away and come back to a green CI or a clear diagnostic report.

## User Input

```text
$ARGUMENTS
```

Interpret arguments best-effort. Parse and validate each flag before use. Only
construct shell commands using recognized flag values — never interpolate raw
user text directly into shell commands. Support these patterns:

**Target selection (mutually exclusive):**

- `/fix-ci` → current branch, latest failed run
- `/fix-ci --pr 123` → target a specific PR
- `/fix-ci --run-id 12345678` → target a specific run
- `/fix-ci --branch develop` → target a specific branch

**Behavior options:**

| Flag                    | Default                | Description                                                                            |
| ----------------------- | ---------------------- | -------------------------------------------------------------------------------------- |
| `--max-iterations N`    | 5                      | Max fix-push-check cycles before escalating                                            |
| `--dry-run`             | off                    | Diagnose only — no fixes, no pushes                                                    |
| `--local-e2e`           | off                    | Run full local E2E suite in parallel with CI after push                                |
| `--reset-env`           | off                    | Reset local E2E environment (fixtures) before local runs                               |
| `--compose-file <path>` | dev.docker-compose.yml | Docker Compose file for local E2E (use `build.docker-compose.yml` to match CI exactly) |
| `--flaky-retry N`       | 0                      | Re-run suspected flaky tests N times before diagnosing                                 |
| `--skip-local-validate` | off                    | Skip local validation (push immediately after fix)                                     |
| `--jobs <job-names>`    | all                    | Only fix specific jobs (e.g., `--jobs e2e-cypress`)                                    |
| `--notify`              | off                    | Force NOTIFY level (always summarize, even for AUTO)                                   |
| `--report-to-pr`        | off                    | Post resolution report as a PR comment when done                                       |

**Examples:**

```
/fix-ci                                    # Basic: fix current branch
/fix-ci --pr 123 --local-e2e              # Fix PR 123, run local E2E in parallel
/fix-ci --dry-run                          # Diagnose only, show what would be fixed
/fix-ci --local-e2e --reset-env            # Full local replication + CI in parallel
/fix-ci --local-e2e --compose-file build.docker-compose.yml  # Match CI exactly
/fix-ci --flaky-retry 2                    # Retry suspected flaky tests twice
/fix-ci --max-iterations 2 --jobs e2e-cypress  # Fix only Cypress, max 2 attempts
/fix-ci --notify --report-to-pr            # Verbose + post report to PR
```

## Autonomy Boundaries (non-negotiable)

This command operates at three escalation levels. **Default is AUTO** — only
escalate when conditions are met.

| Level      | Condition                                          | Action                                     |
| ---------- | -------------------------------------------------- | ------------------------------------------ |
| **AUTO**   | Clear diagnosis, ≤5 files, known pattern           | Fix, validate, push — no user interaction  |
| **NOTIFY** | Moderate changes, multiple fixes in one iteration  | Fix, push, then summarize what was changed |
| **BREAK**  | Same error 2+ iterations, ambiguous root cause,    | **Stop and ask user** for direction        |
|            | >5 files touched, dependency/config changes,       |                                            |
|            | architectural changes, workflow file modifications |                                            |

**BREAK triggers (must stop and ask):**

- [ ] Same error appears in 2+ consecutive iterations
- [ ] Root cause is ambiguous (multiple plausible explanations)
- [ ] Fix requires changing >5 files
- [ ] Fix involves dependency changes (`package.json`, `pom.xml`)
- [ ] Fix involves CI workflow files (`.github/workflows/`)
- [ ] Fix involves architectural/structural changes
- [ ] Fix involves changing environment configuration (`.env.example`, Docker)
- [ ] Local validation fails after 2 attempts to fix it

## Safety Rules (non-negotiable)

- **Never** push without local validation passing.
- **Never** modify files outside the scope of the diagnosed failure.
- **Always** run formatting before commit (`mvn spotless:apply` for backend,
  `npm run format` for frontend) per CLAUDE.md.
- **Always** use both test-skip flags for backend builds:
  `mvn clean install -DskipTests -Dmaven.test.skip=true`
- **Always** use `npm run cy:*` scripts for Cypress, never raw `npx cypress`
  (`ELECTRON_RUN_AS_NODE` env var breaks Cypress in some agent environments; the
  npm scripts unset it automatically).
- **Never** push to `develop`, `main`, or other protected branches. If the
  current branch is a protected branch, BREAK immediately and instruct the user
  to switch to a feature branch.
- **Never** force-push. If a force-push seems needed, BREAK and ask user.
- **Cap iterations** at 5 (configurable via `--max-iterations`). After max
  iterations, produce a final diagnostic report and stop.

## Workflow

### 0) Preflight — gather facts, no changes

**Prerequisites (check before proceeding):**

- [ ] `gh` CLI is installed and authenticated (`gh auth status`)
- [ ] `git` is available and repo is initialized (`git rev-parse --git-dir`)
- [ ] Current directory is the repo root (`git rev-parse --show-toplevel`)
- [ ] Network connectivity (can reach GitHub API)

If any prerequisite fails, report clearly which tool is missing and how to
install/configure it, then BREAK.

**Gather situational context:**

```bash
git rev-parse --show-toplevel
git branch --show-current
git status --porcelain
gh pr list --head "$(git branch --show-current)" --json number,url --limit 1
```

Determine:

- **BRANCH**: Current branch name
- **PR_NUMBER**: Associated PR (if any)
- **HAS_UNCOMMITTED**: Whether working tree is dirty (warn but don't block)
- **ITERATION**: Set to 1
- **ERROR_HISTORY**: Empty list (tracks errors across iterations to detect
  loops)

**Branch protection gate:** If BRANCH is `develop`, `main`, or another protected
branch → BREAK immediately. Tell the user: "Cannot run /fix-ci on a protected
branch. Please switch to a feature branch first."

Check project knowledge for known CI failure patterns:

1. `.specify/memory/` — project-scoped memory (shared across all agents)
2. `AGENTS.md` — agent onboarding, architecture patterns, known issues
3. Agent-specific memory (e.g., `$HOME/.claude/projects/*/memory/MEMORY.md`) if
   available

Report the detected state before proceeding.

---

### 1) Download & Triage — identify what failed

**Download logs** using the project's CI log infrastructure:

```bash
.specify/scripts/bash/download-ci-logs-shim.sh --branch $BRANCH --failed
```

If a specific `--run-id` was provided, use that instead.

**Read the summary** from the downloaded logs directory:

- Find `summary.txt` in `.cursor/ci-logs/*/`
- Identify which jobs failed and which steps within those jobs

**Download artifacts** — screenshots are **MANDATORY** for E2E failures:

```bash
# Always attempt all artifact downloads
gh run download $RUN_ID -n cypress-screenshots -D .cursor/ci-logs/artifacts/ 2>/dev/null
gh run download $RUN_ID -n playwright-report -D .cursor/ci-logs/artifacts/ 2>/dev/null
gh run download $RUN_ID -n playwright-screenshots -D .cursor/ci-logs/artifacts/ 2>/dev/null
gh run download $RUN_ID -n playwright-traces -D .cursor/ci-logs/artifacts/ 2>/dev/null
```

**E2E screenshot gate (non-negotiable):**

If a Cypress or Playwright E2E job failed, you **MUST** download and visually
review every failure screenshot before proceeding to diagnosis. Screenshots are
the single most reliable evidence for E2E failures — log text alone is often
misleading (e.g., "element not visible" doesn't tell you _why_).

- [ ] List all downloaded screenshot files
      (`find .cursor/ci-logs/artifacts/ -name "*.png"`)
- [ ] Read **every** screenshot using the Read tool (which renders images)
- [ ] For each screenshot, note: what page is shown, what state the UI is in,
      whether the sidenav/overlay is blocking content, whether the correct page
      loaded, whether test data is present
- [ ] **Do NOT proceed to Phase 2 for any E2E failure without having reviewed
      its screenshot.** If no screenshot was uploaded by CI, note this as a gap
      and rely on logs + code reading instead, but flag reduced confidence.

**Classify each failure** into one of these categories:

| Category   | Indicators                                         | Typical Fix                       |
| ---------- | -------------------------------------------------- | --------------------------------- |
| **infra**  | Runner timeout, Docker pull failure, OOM           | Re-run (not a code fix)           |
| **build**  | Compilation error, missing import, type error      | Fix source code                   |
| **test**   | Assertion failure, element not found, timeout      | Fix test or source code           |
| **config** | Missing env var, auth failure, container unhealthy | Fix config files                  |
| **flaky**  | Passes locally, intermittent, timing-dependent     | Add retry/wait or skip with issue |

**Flaky test detection (with `--flaky-retry N`):**

If `--flaky-retry` is set and a test failure looks potentially flaky (e.g.,
timing-dependent, passes locally, or failed intermittently in recent runs),
re-run the failed CI job before diagnosing:

```bash
gh run rerun $RUN_ID --failed
```

Check if the same test passed in recent runs on this branch:

```bash
gh run list --branch $BRANCH --limit 5 \
  --json databaseId,conclusion --jq '.[] | select(.conclusion=="success")'
```

If the test passes on re-run → classify as **flaky**, log it, and move on. If it
fails again after N retries → classify as **test** and proceed to diagnosis.
Track flaky tests in the iteration report for follow-up.

**Triage checklist:**

- [ ] Read `summary.txt` — which jobs and steps failed?
- [ ] Read failed step logs — what is the FIRST error? (ignore cascading errors)
- [ ] For E2E failures: read screenshot PNGs (use Read tool on image files)
- [ ] Classify each failure by category
- [ ] Check ERROR_HISTORY — is this the same error as a previous iteration?

**If same error repeats → BREAK.** Present findings and ask user for direction.

**If failure is `infra` category → suggest `gh run rerun $RUN_ID --failed`**
instead of code changes. Ask user to confirm re-run.

---

### 2) Diagnose — find root cause for each failure

For each classified failure, perform targeted diagnosis:

**Build failures:**

- Read the compiler/bundler error output
- Identify the file and line number
- Read the source file to understand context
- Check if a recent commit introduced the issue (`git log --oneline -5`)

**Test failures (unit/integration):**

- Read the test output to find the assertion that failed
- Read the test file to understand expected vs actual behavior
- Read the source code under test
- Check if test data/fixtures are correct

**E2E test failures (Cypress/Playwright) — screenshot-first diagnosis:**

E2E failures **require** visual evidence. Follow this exact sequence:

1. **Review the screenshot** (Read tool renders PNG files). Describe what you
   see: which page, what UI state, any overlays or missing elements.
2. **Cross-reference with the error message** from logs. The screenshot shows
   _what happened_; the log shows _what the test expected_. Both are needed.
3. **Read the failing spec file** to understand the test's intent and flow.
4. **Read page object methods** used by the test (e.g., `HomePage.js`,
   `RoutineReportPage.js`) to trace the navigation and action chain.
5. **Identify the root cause** by matching screenshot evidence to code:

   | Screenshot Shows                             | Likely Root Cause                                  |
   | -------------------------------------------- | -------------------------------------------------- |
   | Sidenav overlaying page content              | SHOW vs LOCK state — `closeNavigationMenu()` issue |
   | Wrong page / still on home page              | Navigation method failed or page didn't load       |
   | Correct page but element missing             | Selector changed, data not loaded, timing          |
   | Page partially loaded / spinner visible      | Page load timeout, slow bundle, API delay          |
   | Login page showing                           | Session expired, auth config issue                 |
   | Blank/white page                             | JS crash, build error, missing env var             |
   | Correct page, element visible but test fails | Test logic issue, wrong assertion                  |

6. **Confirm diagnosis aligns with BOTH screenshot and log** before proceeding.
   If the screenshot contradicts the log message, trust the screenshot — it
   shows the actual browser state at failure time.

**Config failures:**

- Check `.env.example` for missing variables
- Check Docker Compose files for `${VAR}` substitution issues
- Check workflow files for missing steps

**Diagnosis checklist:**

- [ ] For E2E failures: reviewed ALL failure screenshots (mandatory gate)
- [ ] For E2E failures: screenshot evidence and log error are consistent
- [ ] Identified root cause file(s) and line(s)
- [ ] Understood why the current code fails
- [ ] Formulated a specific fix (not vague "investigate further")
- [ ] Assessed fix scope: how many files? What kind of change?
- [ ] Checked autonomy level: AUTO / NOTIFY / BREAK?

**If diagnosis is ambiguous → BREAK.** Present the evidence and possible
explanations, then ask user which direction to pursue.

---

### 3) Fix — apply targeted, minimal changes

**Before fixing, run the risk gate checklist:**

- [ ] Fix touches ≤5 files? (if no → BREAK)
- [ ] Fix does NOT change dependencies? (if it does → BREAK)
- [ ] Fix does NOT change workflow files? (if it does → BREAK)
- [ ] Fix does NOT change environment config? (if it does → BREAK)
- [ ] Fix is NOT the same as a previous iteration's fix? (if it is → BREAK)
- [ ] Fix is consistent with project architecture? (check constitution.md)

**Apply the fix:**

- Use Edit tool for targeted changes (preferred over Write for existing files)
- Keep changes minimal — fix only what's broken, don't refactor adjacent code
- Add comments only where the fix is non-obvious

**Run mandatory formatting:**

```bash
# Backend (if Java files changed)
mvn spotless:apply

# Frontend (if JS/JSX/TS/TSX files changed)
cd frontend && npm run format && cd ..
```

---

### 4) Local Validation — gate before pushing

Run validation in two tiers: **fast checks** (always) then **local E2E** (when
the failure was an E2E test).

#### Tier 1: Fast checks (always run)

**Frontend changes:**

```bash
# Format check (uses project-configured prettier via npm script)
cd frontend && npm run check-format

# Unit tests (fast — always run)
cd frontend && npm test -- --watchAll=false --coverage=false
```

**Backend changes:**

```bash
# Compile + unit tests
mvn test
```

#### Tier 2: Local E2E replication (when failure was E2E)

If the diagnosed failure was a Cypress or Playwright E2E test, replicate the CI
environment locally and run the specific failing test **before pushing**. This
catches fixes that pass unit tests but fail in the full browser context.

**Step 1 — Reset local E2E environment to match CI:**

If `--reset-env` is set, or if containers are unhealthy/stopped, reset the local
environment using the project's existing fixture scripts:

```bash
# Load the exact same fixtures CI loads (works with both dev and build compose)
./src/test/resources/load-ci-fixtures.sh

# OR for a full environment reset (if containers are stale/unhealthy):
./scripts/reset-dev-env.sh --skip-build
```

If containers are not running or are unhealthy, restart them first:

```bash
# Quick restart using configured compose file (preserves DB, reloads fixtures)
# Default: dev.docker-compose.yml. Use --compose-file build.docker-compose.yml
# to match CI exactly (builds from source instead of using pre-built images).
docker compose -f $COMPOSE_FILE up -d
./src/test/resources/load-ci-fixtures.sh
```

Without `--reset-env`, only reload fixtures if the test failure suggests stale
data (e.g., missing patient, missing analyzer config).

**Step 2 — Run the specific failing E2E test locally:**

```bash
# Cypress (MUST use npm scripts, never raw npx — see CLAUDE.md)
cd frontend && npm run cy:spec "cypress/e2e/<failing-test>.cy.js"

# Playwright
cd frontend && npm run pw:test -- <failing-test>.spec.ts

# For broader validation: run full Cypress suite with fail-fast
cd frontend && npm run cy:failfast
```

**Step 3 — Interpret local E2E result:**

- **Local passes** → Fix is likely correct. Proceed to push.
- **Local fails with same error** → Fix didn't work. Go back to Phase 3.
- **Local fails with different error** → New issue introduced. Investigate.

#### Validation checklist

- [ ] Formatting passes (no diffs)
- [ ] Relevant unit tests pass
- [ ] If E2E failure: local environment reset via `load-ci-fixtures.sh`
- [ ] If E2E failure: specific test passes locally against reset environment
- [ ] No new warnings or errors introduced

**If validation fails:**

- Attempt to fix the validation failure (1 retry)
- If validation still fails after retry → BREAK and report

**Gate: Do NOT proceed to push unless all validation checks pass.**

---

### 5) Push & Monitor — commit, push, wait, re-check

**Commit with a descriptive message:**

```bash
git add <specific-files-only>
git commit -m "$(cat <<'EOF'
fix(<scope>): <concise description of what was fixed>

<1-2 sentence explanation of root cause and fix>

Failing test: <test name or CI job name>
Iteration: $ITERATION of $MAX_ITERATIONS
EOF
)"
```

Follow your system prompt conventions for commit attribution (e.g.,
Co-Authored-By trailer).

**Push to remote:**

```bash
git push
```

**Report what was pushed** (NOTIFY level or higher):

- Files changed
- Summary of fix
- Expected CI outcome

#### Parallel tracks: Local E2E + CI monitoring

After pushing, always monitor CI. If `--local-e2e` is set, **also** run the full
local E2E suite in parallel — local results arrive in minutes while CI takes
15-30 minutes.

**Track A — Local E2E (only with `--local-e2e` flag):**

Reset the environment and run the **full E2E suite** (not just the single test)
to catch regressions before CI reports back.

```bash
# Reset fixtures to match CI (include if --reset-env is set, or always
# when --local-e2e is used for the first time in this iteration)
./src/test/resources/load-ci-fixtures.sh

# Run full Cypress suite with fail-fast (stops on first failure for faster feedback)
cd frontend && npm run cy:failfast

# Or full Playwright suite (background)
cd frontend && npm run pw:test
```

Run this in the background while monitoring CI. If local E2E fails before CI
completes, you have early signal to start diagnosing the next failure without
waiting for CI.

**Without `--local-e2e`:** Skip Track A entirely and only monitor CI.

**Track B — CI monitoring (exponential backoff):**

Poll CI status with exponentially escalating intervals, capped at 20 minutes.
Early checks catch fast failures (build errors); later checks accommodate slower
E2E jobs.

| Check | Wait   | Cumulative | Catches                     |
| ----- | ------ | ---------- | --------------------------- |
| 1st   | 3 min  | 3 min      | Build failures, lint errors |
| 2nd   | 6 min  | 9 min      | Unit test failures          |
| 3rd   | 12 min | 21 min     | Fast E2E failures           |
| 4th   | 20 min | 41 min     | Full E2E suite completion   |
| 5th+  | 20 min | 61 min+    | Long-running / queued jobs  |

```bash
# Exponential backoff loop
WAIT_SECS=180  # Start at 3 minutes
MAX_WAIT=1200  # Cap at 20 minutes

while true; do
  sleep $WAIT_SECS

  STATUS=$(gh run list --branch $BRANCH --limit 1 \
    --json databaseId,status,conclusion \
    --jq '.[0] | "\(.status) \(.conclusion)"')

  if [[ "$STATUS" == *"completed"* ]]; then
    break
  fi

  # Exponential escalation: 3 → 6 → 12 → 20 → 20 (capped)
  WAIT_SECS=$(( WAIT_SECS * 2 ))
  if [ $WAIT_SECS -gt $MAX_WAIT ]; then
    WAIT_SECS=$MAX_WAIT
  fi
done
```

#### Reconcile results

**After both tracks complete:**

| Local E2E | CI Result | Action                                         |
| --------- | --------- | ---------------------------------------------- |
| Pass      | Pass      | Proceed to Phase 6 (Resolution Report)         |
| Pass      | Fail      | New failure — download logs, loop to Phase 1   |
| Fail      | (pending) | Start diagnosing locally, don't wait for CI    |
| Fail      | Fail      | Confirm same error — increment ITERATION, loop |

- If **all green** → proceed to Phase 6 (Resolution Report)
- If **still failing** → increment ITERATION, loop back to Phase 1
- If **ITERATION > MAX_ITERATIONS** → proceed to Phase 6 (final report)

---

### 6) Resolution Report — success or escalation

Produce a structured report:

**If CI is green:**

```
## CI Remediation Complete

**Branch:** $BRANCH
**Iterations:** $ITERATION
**Total time:** ~X minutes

### Fixes Applied
| Iteration | Failure | Root Cause | Fix | Files Changed |
|-----------|---------|------------|-----|---------------|
| 1 | ... | ... | ... | ... |
| 2 | ... | ... | ... | ... |

### Commits
- abc1234 fix(test): ...
- def5678 fix(ci): ...

All CI checks are now passing.
```

**If CI is still red (max iterations reached):**

```
## CI Remediation — Escalation Required

**Branch:** $BRANCH
**Iterations attempted:** $MAX_ITERATIONS
**Status:** Still failing

### Unresolved Failures
| Job | Step | Error | Attempts | Notes |
|-----|------|-------|----------|-------|
| ... | ... | ... | ... | ... |

### What was tried
1. Iteration 1: [description] — result: [still failing because...]
2. Iteration 2: [description] — result: [different error / same error]

### Recommended next steps
- [ ] [Specific actionable recommendation]
- [ ] [Alternative approach to investigate]

### Diagnostic artifacts
- Logs: .cursor/ci-logs/[path]
- Screenshots: .cursor/ci-logs/artifacts/[path]
```

**Post report to PR (with `--report-to-pr`):**

If `--report-to-pr` is set and a PR_NUMBER was detected in preflight, post the
resolution report as a PR comment for team visibility:

```bash
gh pr comment $PR_NUMBER --body "$(cat <<'EOF'
<!-- fix-ci report -->
[paste the resolution report here]
EOF
)"
```

Use the `<!-- fix-ci report -->` HTML comment as a marker. If a previous
`/fix-ci` report exists on the PR, edit it instead of creating a new comment to
avoid clutter:

```bash
# Find existing report comment
COMMENT_ID=$(gh api repos/$OWNER/$REPO/issues/$PR_NUMBER/comments \
  --jq '.[] | select(.body | contains("<!-- fix-ci report -->")) | .id' \
  | tail -1)

if [ -n "$COMMENT_ID" ]; then
  gh api repos/$OWNER/$REPO/issues/comments/$COMMENT_ID \
    -X PATCH -f body="$REPORT_BODY"
else
  gh pr comment $PR_NUMBER --body "$REPORT_BODY"
fi
```

**Update agent memory** if a new pattern was discovered:

- Add to MEMORY.md if a CI failure pattern + fix was found that would be useful
  for future iterations

---

## Failure Classification Quick Reference

Use this to rapidly classify failures in Phase 1:

| Log Pattern                                      | Category | Strategy               |
| ------------------------------------------------ | -------- | ---------------------- |
| `FATAL: password authentication failed`          | config   | Check .env / env vars  |
| `container ... is unhealthy`                     | config   | Check .env / Docker    |
| `Cannot find module`                             | build    | Fix import path        |
| `TypeError: Cannot read properties of undefined` | build    | Fix null reference     |
| `expected ... to be 'visible'`                   | test     | Fix selector / timing  |
| `is being covered by`                            | test     | Fix z-index / overlay  |
| `Timed out after waiting`                        | test     | Increase timeout / fix |
| `No such file or directory`                      | config   | Check paths / fixtures |
| `Exit code 137` (OOM)                            | infra    | Suggest re-run         |
| `::error::The runner has received a shutdown`    | infra    | Suggest re-run         |

## Iteration State Tracking

Maintain this state across the loop (in your working memory, not in a file):

```
ITERATION: 1
MAX_ITERATIONS: 5 (or user-specified)
BRANCH: <current branch>
PR_NUMBER: <if applicable>
ERROR_HISTORY: [
  { iteration: 1, job: "...", error: "...", fix: "...", result: "..." },
]
FIXES_APPLIED: [
  { file: "...", change: "...", commit: "..." },
]
```

Before each iteration, compare current errors with ERROR_HISTORY. If the same
error signature appears in 2+ consecutive iterations → BREAK immediately.
