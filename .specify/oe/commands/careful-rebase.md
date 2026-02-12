# Careful Rebase Workflow

When the user invokes `/careful-rebase` (optionally with arguments), perform a
careful, checkpointed rebase workflow with:

- A **backup branch** created up-front
- A **rebase plan** (what will move, onto what) with a **projected change
  summary**
- A **conflict risk** report (where conflicts are likely) including **base-only
  improvements**
- An **answer session** to resolve ambiguities before/while resolving conflicts
  (only if needed)
- An **optional squash-before-rebase** step
- **Validation** and a **post-rebase report** (including `git range-diff`)

This command is intentionally conservative: it should **plan first**, then
require explicit user confirmation before any history-rewriting actions.

## Review Cycle (required gates)

This workflow must have a clear, repeatable review cycle:

1. **Pre-rebase confirmation (REQUIRED):** show the projected changes (commits
   to replay, diffstat, hotspots, base-only improvements, and whether a
   force-push will be needed) and stop until the user explicitly says **“Proceed
   with rebase”**.
2. **QA session checkpoint (REQUIRED):** after the rebase, produce a short QA
   checklist and stop until the user explicitly confirms **“QA approved”**
   before suggesting any force-push.

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
- **Upstream tracking ref** (if any):
  `git rev-parse --abbrev-ref --symbolic-full-name @{u}`

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

Produce a short plan the user can review before any rebase starts.

- **Commits that will be replayed**:
  - `git log --oneline --decorate <base>..HEAD`
- **Commits from base you will pick up**:
  - `git log --oneline --decorate <FORK_SHA>..<base>`
- **High-level change summary**:
  - `git diff --stat <FORK_SHA>..HEAD`
- **Upstream divergence (if tracking ref exists)**:
  - `git log --oneline --decorate @{u}..HEAD` (what we would push)
  - `git log --oneline --decorate HEAD..@{u}` (what we'd be overwriting if
    force-push)

If there are merge commits on the branch (detect via
`git rev-list --merges <base>..HEAD`):

- Recommend `--preserve-merges` by default
- Ask the user whether to preserve merges or linearize history

### 3) Identify likely conflict hotspots and base-only improvements (projected risk)

Compute the file sets (ours vs base) from the fork point:

```bash
OUR_FILES=$(git diff --name-only <FORK_SHA>..HEAD)
THEIR_FILES=$(git diff --name-only <FORK_SHA>..<base>)
```

Then report only what matters for review:

- **Hotspots (both modified)**: likely conflicts
- **Base-only improvements (base changed, we didn’t)**: easy to accidentally
  lose

#### 3a) Report hotspots (both-modified files)

- Count of hotspot files
- The list of hotspot file paths (grouped by extension/area if helpful)

Important: Call out any "special" hotspots (examples):

- Lockfiles (`package-lock.json`, `yarn.lock`, `pnpm-lock.yaml`)
- Localization JSON (`frontend/src/languages/*.json`)
- Submodule pointer changes (e.g., `plugins` as a submodule)

#### 3b) Report base-only improvements (CRITICAL — easy to lose)

List **all files that the base branch modified but our branch did not touch**.
These are improvements from upstream collaborators (code review fixes, new
features, documentation, scripts, CI configs) that should survive the rebase
unchanged.

```bash
# Files modified on base but NOT on our branch
comm -23 <(echo "$THEIR_FILES" | sort) <(echo "$OUR_FILES" | sort)
```

Group base-only improvements by area and highlight non-compiled/non-tested files
(easy to miss):

- Shell scripts (`*.sh`)
- CI/CD configs (`.github/`, `Dockerfile`, `docker-compose*.yml`)
- Documentation (`.md`, `.specify/`)
- Config files (`.xml`, `.yml`, `.json` outside `src/`)
- Any file type not covered by `mvn compile` or `mvn test`

**Present this list to the user explicitly** — do not bury it in verbose output.

#### Pre-rebase i18n duplicate key scan (MANDATORY)

Before rebasing, **always** scan all localization JSON files (whether or not
they appear as hotspots) for pre-existing duplicate keys:

```bash
python3 .specify/scripts/python/check-json-duplicate-keys.py \
  frontend/src/languages/en.json \
  frontend/src/languages/fr.json
```

If duplicates are found, **report them to the user and ask whether to
deduplicate before or after the rebase**. Do NOT silently proceed — duplicate
keys in JSON cause CI failures (the `i18n-check` workflow rejects them) and can
mask data loss when `JSON.parse` silently takes the last value for each key.

### 4) Pre-rebase confirmation (REQUIRED GATE)

Before any history rewrite (squash/reset/rebase), provide a short “projected
changes” summary:

- Base ref + `BASE_SHA`
- Fork point `FORK_SHA`
- Backup branch name
- Number of commits to replay (`git rev-list --count <base>..HEAD`)
- Number of base commits to pick up (`git rev-list --count <FORK_SHA>..<base>`)
- Diffstat (`git diff --stat <FORK_SHA>..HEAD`)
- Hotspots count + list (Step 3a)
- Base-only improvements count + list (Step 3b)
- Rebase mode: `--preserve-merges` yes/no
- Squash-before-rebase: yes/no
- Push impact (if upstream tracking exists): whether a force-push is likely

Then stop and wait for the user to explicitly say: **“Proceed with rebase”**.

### 5) Answer session (resolve ambiguities up-front, only if needed)

If hotspots exist (or if the user requests), run an "answer session":

#### 5a) Hotspot resolution preferences

- Ask the user for **resolution preferences** for hotspot categories, e.g.:
  - "If we hit a lockfile conflict, should we regenerate post-rebase rather than
    hand-merge?"
  - "For translation JSON conflicts, should we merge keys and then format?"
  - "If a file is deleted on one side and modified on the other, do we keep the
    file or accept deletion?"
- For any single hotspot file that looks high-risk, ask:
  - "Should we prefer our changes, their changes, or do a manual merge?"

#### 5b) Base-only improvement protection policy (MANDATORY)

For base-only improvements identified in Step 3b, establish a clear policy:

- **Default policy: NEVER "take ours" for base-only files.** If a base-only file
  shows up as a conflict, it means context lines shifted near our changes. The
  correct resolution is almost always "take theirs" (the base version) since our
  branch never intentionally modified the file.
- Ask the user to confirm this default, or override for specific files.
- **Explicitly list high-risk base-only files** (shell scripts, CI configs,
  docs) and confirm: "These files were modified on the base branch but not on
  ours. After the rebase, I will verify they match the base branch version
  exactly."

Record these decisions in the chat and apply them consistently if conflicts
occur.

### 6) Optional squash-before-rebase (checkpoint #3)

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

### 7) Execute the rebase (checkpoint #4)

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

### 8) Post-rebase review + validation (checkpoint #5)

Always run these:

- `git status`
- `git log -n 10 --oneline --decorate`
- `git range-diff <FORK_SHA>..<backup-branch> <BASE_SHA>..HEAD`

#### Post-rebase i18n duplicate key check (MANDATORY)

After the rebase completes (even if there were no conflicts on i18n files),
**always** re-run the duplicate key scan on all localization JSON files:

```bash
python3 .specify/scripts/python/check-json-duplicate-keys.py \
  frontend/src/languages/en.json \
  frontend/src/languages/fr.json
```

If duplicates are found:

1. Report the count and sample keys to the user
2. Offer to auto-deduplicate (keep last occurrence per key, which matches
   `JSON.parse` behavior — so no functional change)
3. The dedup fix should be included in the rebased commit (amend) rather than as
   a separate commit, since the duplicates are a merge artifact

**Why this matters:** Git's text-based merge can cleanly auto-merge two sets of
additions to a JSON file, producing valid JSON with duplicate keys. The merge
succeeds with no conflicts, but the result has silent duplicates that break CI
(`i18n-check` workflow) and can cause subtle bugs where the "wrong" translation
wins depending on key order.

#### Post-rebase regression scan (MANDATORY)

After the rebase completes, verify that **base-only improvements survived**. For
every file identified in Step 3b (base-only), confirm our rebased branch matches
the base branch version:

```bash
# For each base-only file, check if our version matches the base
comm -23 <(git diff --name-only <FORK_SHA>..<base> | sort) \
         <(git diff --name-only <FORK_SHA>..<backup-branch> | sort) | \
  while IFS= read -r f; do
    if ! git diff --quiet <base> HEAD -- "$f" 2>/dev/null; then
      echo "REGRESSION: $f differs from base branch!"
    fi
  done
```

If any regressions are found:

1. **Stop and report** the affected files with a side-by-side summary
2. For each regression, show what the base branch has vs what our branch has
3. Ask the user whether to restore from base (`git checkout <base> -- <file>`)
   or keep the current version
4. **Do not proceed to push** until all regressions are resolved

Additionally, scan for **unintended net deletions** across ALL files (not just
base-only). This catches cases where conflict resolution on hotspot files
accidentally dropped content:

```bash
git diff <base>..HEAD --numstat | awk '$2 > $1 && $1 != "-" {
  printf "NET DELETION: -%d lines in %s\n", $2 - $1, $3
}'
```

For each file with net deletions, verify the deletions are intentional (e.g.,
removing a deleted entity) rather than accidental (reverting an improvement).

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

### 9) Official QA session checkpoint (REQUIRED GATE)

Run (or confirm CI ran) the agreed validation level, then produce a short QA
checklist summary. At minimum, include:

- What validations were run (quick/standard/thorough) and pass/fail
- Any manual smoke steps performed (if applicable)
- Any files restored from base due to regressions (if any)

Then stop and wait for the user to explicitly confirm: **“QA approved”**.

### 10) Post-rebase report

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
