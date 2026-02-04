# Careful Rebase Workflow

When the user invokes `/careful-rebase` (optionally with arguments), perform a
careful, checkpointed rebase workflow with:

- A **backup branch** created up-front
- A **rebase plan** (what will move, onto what)
- A **conflict risk** report (where conflicts are likely)
- An **answer session** to resolve ambiguities before/while resolving conflicts
- An **optional squash-before-rebase** step
- **Validation** and a **post-rebase report** (including `git range-diff`)

This command is intentionally conservative: it should **plan first**, then
require explicit user confirmation before any history-rewriting actions.

## User Input

```text
$ARGUMENTS
```

Interpret arguments best-effort. Support these patterns:

- `/careful-rebase` → default base branch: `develop` if present, else `main`
- `/careful-rebase <base>` → base branch/ref (e.g., `develop`, `origin/develop`)
- Optional flags (if present in user input):
  - `--squash` → squash the branch to **one** commit before rebasing
  - `--no-squash` → do not squash (default)
  - `--preserve-merges` → use `git rebase --rebase-merges`
  - `--validate {quick|standard|thorough}` → pick validation depth

If any inputs are unclear, ask a single concise question and continue with safe
defaults.

## Safety Rules (non-negotiable)

- **Do not start** if there are uncommitted changes.
- **Always** create a backup branch before rewriting history.
- **Never** force-push automatically. If a force-push is required, explain why
  and ask the user explicitly before running it (prefer `--force-with-lease`).
- If a rebase is already in progress, **stop** and ask whether to continue or
  abort.

## Workflow

### 0) Preflight (gather facts, no changes yet)

Run these and summarize the results:

- `git rev-parse --show-toplevel`
- `git status --porcelain`
- `git branch --show-current` (if detached, stop and ask user what branch to
  use)
- `git remote -v`
- `git fetch --prune` (safe; ensures base is current)

Determine:

- **Current branch** (the branch being rebased)
- **Base ref** (target branch/ref to rebase onto)
- **BASE_SHA**: `git rev-parse <base>`
- **FORK_SHA**: `git merge-base HEAD <base>`
- Ahead/behind counts: `git rev-list --left-right --count <base>...HEAD`

If working tree is not clean, stop and ask the user to commit/stash first.

### 1) Create a backup branch (checkpoint #1)

Create a backup ref that points to the current `HEAD` before any rewrite.

- Compute a timestamp like `YYYYMMDD-HHMMSS`
- Create backup branch name:
  - `backup/<current-branch-sanitized>/<timestamp>-<shortsha>`
  - Replace `/` in the branch name with `-` for sanitization.
- Run: `git branch <backup-branch> HEAD`

Print the “restore” options clearly:

- `git switch <backup-branch>` (to inspect)
- `git switch <current-branch> && git reset --hard <backup-branch>` (to restore)

### 2) Build a careful rebase plan (checkpoint #2)

Produce a short plan the user can review before any rebase starts:

- **Commits that will be replayed**:
  - `git log --oneline --decorate <base>..HEAD`
- **High-level change summary**:
  - `git diff --stat <FORK_SHA>..HEAD`

If there are merge commits on the branch (detect via
`git rev-list --merges <base>..HEAD`):

- Recommend `--preserve-merges` by default
- Ask the user whether to preserve merges or linearize history

### 3) Identify likely conflict hotspots

Estimate conflict risk by overlapping touched files since the fork-point:

- Our files: `git diff --name-only <FORK_SHA>..HEAD`
- Their files: `git diff --name-only <FORK_SHA>..<base>`
- Hotspots: intersection of those two lists

Report:

- Count of hotspot files
- The list of hotspot file paths (grouped by extension/area if helpful)

Important: Call out any “special” hotspots (examples):

- Lockfiles (`package-lock.json`, `yarn.lock`, `pnpm-lock.yaml`)
- Localization JSON (`frontend/src/languages/*.json`)
- Submodule pointer changes (e.g., `plugins` as a submodule)

### 4) Answer session (resolve ambiguities up-front)

If hotspots exist (or if the user requests), run an “answer session”:

- Ask the user for **resolution preferences** for hotspot categories, e.g.:
  - “If we hit a lockfile conflict, should we regenerate post-rebase rather than
    hand-merge?”
  - “For translation JSON conflicts, should we merge keys and then format?”
  - “If a file is deleted on one side and modified on the other, do we keep the
    file or accept deletion?”
- For any single hotspot file that looks high-risk, ask:
  - “Should we prefer our changes, their changes, or do a manual merge?”

Record these decisions in the chat and apply them consistently if conflicts
occur.

### 5) Optional squash-before-rebase (checkpoint #3)

Only do this if the user explicitly requested `--squash` (or answers “yes” when
asked).

Explain the trade-off briefly:

- **Pros**: one conflict-resolution pass; cleaner history
- **Cons**: loses commit granularity; harder to bisect

Squash method (non-interactive, safe with backup):

- Confirm the commit message to use (ask the user if unclear).
- Run:
  - `git reset --soft <FORK_SHA>`
  - `git commit -m "<chosen message>"`

### 6) Execute the rebase (checkpoint #4)

Before running any rebase, ask for a clear “go / stop” from the user.

Choose the command based on earlier decisions:

- Default: `git rebase <base>`
- Preserve merges: `git rebase --rebase-merges <base>`

If conflicts occur:

- Run `git status` and list conflicted paths
- For each conflict, apply the “answer session” preferences; if still ambiguous,
  stop and ask the user a targeted question before editing
- After resolving a conflict:
  - `git add <files...>`
  - `git rebase --continue`

If the user wants to abandon:

- `git rebase --abort`
- Offer to restore from the backup branch

### 7) Validation (checkpoint #5)

Always run these:

- `git status`
- `git log -n 10 --oneline --decorate`
- `git range-diff <FORK_SHA>..<backup-branch> <BASE_SHA>..HEAD`

Then run validation level:

- **quick**: compile/build check (skip tests):  
  `mvn clean install -DskipTests -Dmaven.test.skip=true`
- **standard**: quick + backend unit/integration tests:  
  `mvn test`  
  (If available, prefer CI-equivalent checks via
  `./scripts/run-ci-checks.sh --skip-submodules`.)
- **thorough**: standard + frontend CI checks (and optionally E2E):  
  `./scripts/run-ci-checks.sh` and `./scripts/run-frontend-ci-checks.sh`  
  (If the user wants to skip E2E,
  `./scripts/run-frontend-ci-checks.sh --skip-e2e`.)

If any validation fails, stop and report failures clearly before suggesting
fixes.

### 8) Post-rebase report

Produce a concise report including:

- Current branch, base ref, `BASE_SHA`, `FORK_SHA`, backup branch name
- Whether squash was performed
- Conflicts encountered and how they were resolved (high-level)
- Key output from `git range-diff` (or a short summary)
- Validations run + pass/fail
- Next steps:
  - If the branch has an upstream, recommend `git push --force-with-lease` (only
    after user confirmation)
  - Otherwise, show `git push -u origin HEAD`
