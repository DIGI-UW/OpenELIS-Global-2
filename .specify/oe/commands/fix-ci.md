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

Interpret arguments best-effort. Support these patterns:

- `/fix-ci` → current branch, latest failed run
- `/fix-ci --pr 123` → target a specific PR
- `/fix-ci --run-id 12345678` → target a specific run
- `/fix-ci --branch develop` → target a specific branch
- `/fix-ci --max-iterations 3` → override max attempts (default: 5)
- `/fix-ci --dry-run` → diagnose only, do not apply fixes or push

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
- **Always** use `npm run cy:spec` scripts for Cypress, never raw `npx cypress`
  (ELECTRON_RUN_AS_NODE breaks it in Claude Code).
- **Never** force-push. If a force-push seems needed, BREAK and ask user.
- **Cap iterations** at 5 (configurable via `--max-iterations`). After max
  iterations, produce a final diagnostic report and stop.

## Workflow

### 0) Preflight — gather facts, no changes

Run these and build a situational picture:

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

Check agent memory (`/home/ubuntu/.claude/projects/*/memory/MEMORY.md`) for
known CI failure patterns relevant to this project.

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

Run the appropriate validation based on what was changed:

**Frontend changes:**

```bash
# Format check
cd frontend && npx prettier ./ --check

# Unit tests (fast — always run)
cd frontend && npm test -- --watchAll=false --coverage=false
```

**Backend changes:**

```bash
# Compile + unit tests
mvn test
```

**E2E test changes (if a specific test file was modified):**

```bash
# Run the specific test that was failing (Cypress)
cd frontend && npm run cy:spec "cypress/e2e/<failing-test>.cy.js"

# Or for Playwright
cd frontend && npm run pw:test -- <failing-test>.spec.ts
```

**Validation checklist:**

- [ ] Formatting passes (no diffs)
- [ ] Relevant unit tests pass
- [ ] If E2E test was modified: specific test passes locally
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

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

**Push to remote:**

```bash
git push
```

**Report what was pushed** (NOTIFY level or higher):

- Files changed
- Summary of fix
- Expected CI outcome

**Wait 15 minutes for CI to run:**

Run `sleep 900` in the background, then:

```bash
gh run list --branch $BRANCH --limit 1 --json databaseId,status,conclusion
```

Poll every 60 seconds after the 15-minute mark until the run completes:

```bash
gh run watch --exit-status
```

**After CI completes:**

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
