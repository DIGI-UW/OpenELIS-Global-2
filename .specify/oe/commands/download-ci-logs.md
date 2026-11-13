# Download CI Logs

When the user invokes `/download-ci-logs` with optional arguments, run the shim
script so that CI logs are downloaded with safe defaults when the user does not
specify a target.

## Behavior

- **Explicit target**: If the user passes `--pr <number>`, `--branch <name>`, or
  `--run-id <id>`, pass all arguments through to the downloader script.
- **Free text / no target**: If the user passes only free text or no arguments,
  run the shim with **current branch** and **failed-only** (so the downloader is
  invoked with `--branch <current-branch>` and `--failed`).

**Before downloading logs, ALWAYS run a holistic status check first:**

```bash
# A PR: the same checks as the PR page
gh pr checks <PR>

# No PR: the runs behind the checks on the branch's newest commit
./scripts/download-ci-logs.sh --branch "$(git branch --show-current)" --list
```

This ensures you identify ALL failing workflows (Backend, Frontend, E2E, etc.)
before downloading logs for just one. A formatting failure in `01 - Backend` is
just as blocking as an E2E failure in `03 - E2E`.

### OpenELIS E2E Pipeline — Finding the Right Run

This project's E2E tests run in a **`workflow_run`-triggered** workflow
(`E2E Tests`) that fires after the `03 - E2E` build workflow completes.

`workflow_run` runs on the default branch, so GitHub files every PR's
`E2E / Tests` run (and every Dependabot run) under develop's name and newest
commit, even though it tests the PR's code. This means:

- Listing runs by branch or commit (`gh run list --branch`/`--commit`,
  `gh api .../actions/runs?head_sha=`), `check-runs` and `check-suites` show
  other PRs' E2E failures as develop's and find no E2E run on a PR branch.
  `commits/<sha>/status` leaves out check runs.
- The downloader reads the commit's `statusCheckRollup`, the checks GitHub
  shows, and takes each run from a check's link. Use `gh pr checks` to see
  the same checks yourself.
- `03 Checkpoint - E2E` stays pending until every suite finishes. To see a
  shard that already failed, open the run it links: `gh run view <RUN_ID>`
  lists each job and its result.

**To find the correct E2E run for a PR:**

```bash
# Get the E2E / Tests run URL from the PR's check status
gh pr checks $PR_NUMBER 2>&1 | grep "03 Checkpoint - E2E[^-]"
# Output: 03 Checkpoint - E2E  fail  0  https://github.com/.../actions/runs/<RUN_ID>  ...

# Extract the run ID
RUN_ID=$(gh pr checks $PR_NUMBER 2>&1 | grep "03 Checkpoint - E2E[^-]" | grep -o 'runs/[0-9]*' | cut -d/ -f2)

# Then download logs for that specific run
.specify/scripts/bash/download-ci-logs-shim.sh --run-id $RUN_ID
```

When downloading E2E artifacts, use the run ID from `03 Checkpoint - E2E`, not
from `03 - E2E`. The former is the test executor; the latter is just the build.

**Artifact naming conventions:**

| Artifact                                   | Contents                                |
| ------------------------------------------ | --------------------------------------- |
| `playwright-core-blob-report-{N}`          | Playwright shard N test results (JSONL) |
| `playwright-core-traces-{N}`               | Failure traces for shard N              |
| `playwright-core-screenshots-{N}`          | Failure screenshots for shard N         |
| `analyzer-playwright-blob-report-demo-{N}` | Analyzer harness shard N results        |
| `analyzer-playwright-traces-demo-{N}`      | Analyzer harness failure traces         |
| `cypress-screenshots-{shard}`              | Cypress failure screenshots by shard    |

**Blob reports use JSONL format.** Extract failures with:

```bash
python3 -c "
import json
with open('report/report.jsonl') as f:
    for line in f:
        obj = json.loads(line)
        if obj.get('method') == 'onTestEnd':
            r = obj['params']['result']
            if r['status'] != 'passed':
                errs = r.get('errors', [])
                msg = errs[0].get('message','')[:300] if errs else 'no error'
                print(f'{r[\"status\"]}: {msg}')
"
```

## Invocation

The agent should parse user arguments and construct a safe command call. Only
pass recognized flags - do NOT pass raw user input directly to the shell.

Run from the repository root:

```bash
.specify/scripts/bash/download-ci-logs-shim.sh [parsed-flags]
```

Where `[parsed-flags]` are validated options from the table below. The shim will
call `scripts/download-ci-logs.sh` with the appropriate options.

## Examples

| User input                            | Effect                           |
| ------------------------------------- | -------------------------------- |
| `/download-ci-logs`                   | Current branch, failed runs only |
| `/download-ci-logs latest failures`   | Current branch, failed runs only |
| `/download-ci-logs --pr 123`          | PR 123                           |
| `/download-ci-logs --pr 123 --failed` | PR 123, failed only              |
| `/download-ci-logs --run-id 12345678` | Specific run by ID               |
| `/download-ci-logs --branch develop`  | Branch develop                   |

## Options (pass-through)

| Option              | Description                                                                             |
| ------------------- | --------------------------------------------------------------------------------------- |
| `--pr <number>`     | PR number to get logs for                                                               |
| `--branch <name>`   | Branch whose newest commit's checks to get logs for                                     |
| `--run-id <id>`     | Download a specific run by ID                                                           |
| `--workflow <name>` | Filter to specific workflow (e.g., `backend.yml`, `frontend.yml`, `e2e-playwright.yml`) |
| `--failed`          | Only download failed runs                                                               |
| `--list`            | List available runs without downloading                                                 |

## Output

Logs are saved under `.cursor/ci-logs/{identifier}-{timestamp}/`. Each workflow
run has its own directory with `summary.txt` and job log files.

## Prerequisites

- `gh` CLI installed and authenticated
- Run from within the repository
