# Setup Milestone Worktrees

When the user invokes `/setup-milestone-worktrees` (optionally with arguments),
perform the **spec-first milestone worktree setup** workflow. This ensures M1/M2
worktrees always have the specs, plan, tasks, and related artifacts before
implementation begins — avoiding the frustration of `/speckit.implement` failing
because worktrees were created before specs were committed.

**Core rule:** Specs MUST be committed on the feature branch BEFORE creating
milestone branches or worktrees. Milestone branches MUST be rebased onto the
feature branch that contains the committed specs.

## User Input

```text
$ARGUMENTS
```

Interpret arguments best-effort. Support these patterns:

- `/setup-milestone-worktrees` → Default: detect feature from current branch,
  commit specs on feat branch, create/update M1 and M2 worktrees
- `/setup-milestone-worktrees --feat <branch>` → Use specified feature branch
- `/setup-milestone-worktrees --m1-only` → Only create/update M1 worktree
- `/setup-milestone-worktrees --m2-only` → Only create/update M2 worktree
- `/setup-milestone-worktrees --dry-run` → Show plan only, no git operations
- `/setup-milestone-worktrees --recover` → Recovery mode: specs already
  committed, rebase existing worktrees onto feat branch

If any inputs are unclear, ask a single concise question and continue with safe
defaults.

## Safety Rules (non-negotiable)

- **Do not create worktrees** until specs are committed on the feature branch.
- **Always stash** uncommitted work in worktrees before rebasing; restore after.
- **Never force-push** automatically. If force-push is required, explain and ask
  the user.
- If a rebase is in progress in any worktree, **stop** and ask whether to
  continue or abort.

## Workflow

### 0) Preflight (gather facts, no changes yet)

Run these and summarize:

- `git rev-parse --show-toplevel` (main worktree root)
- `git branch --show-current` (current branch)
- `git status --porcelain` (uncommitted changes in main worktree)
- `git worktree list` (existing worktrees and their branches)
- Detect feature branch:
  - If on `feat/NNN-*` or `spec/NNN-*`, use that as feat branch
  - If on `feat/NNN-*-m1-*` or `feat/NNN-*-m2-*`, infer parent feat branch (e.g.
    `feat/012-ogc-337-generic-astm-plugin-profiles`)
  - If `--feat <branch>` provided, use it
- Detect spec directory: `specs/{NNN}-*/` (e.g.
  `specs/012-generic-astm-plugin-profiles/`)
- Check if specs are committed on feat branch:
  - `git log -1 --oneline feat/<branch> -- specs/` (or equivalent path)
  - If no commit touches specs, specs are NOT committed

If the main worktree has uncommitted changes that include specs, **stop** and
ask: "Specs appear uncommitted. Commit them on the feature branch first?
(yes/no)"

### 1) Commit specs on feature branch (REQUIRED before worktrees)

**If specs are not yet committed on the feature branch:**

1. Ensure you are on the feature branch (or switch to it).
2. Stage spec artifacts:
   - `specs/{NNN}-*/` (spec.md, plan.md, tasks.md, data-model.md, contracts/,
     checklists/, alignment-report.md, etc.)
   - `docs/` for feature-specific docs (e.g. `docs/analyzers/012/`)
3. Commit with a clear message, e.g.:
   - `feat(NNN): add spec, plan, tasks, contracts, and docs for <feature-name>`
4. Confirm: `git log -1 --stat` shows the spec commit.

**If specs are already committed:** Skip to step 2.

### 2) Identify or create milestone branches

Convention (Constitution IX):

- M1: `feat/{NNN}[-{jira}]-{name}-m1-{desc}`
- M2: `feat/{NNN}[-{jira}]-{name}-m2-{desc}`

If milestone branches do not exist, create them from the feature branch (which
now has specs):

```bash
git checkout -b feat/NNN-...-m1-... feat/<feature-branch>
git checkout -b feat/NNN-...-m2-... feat/<feature-branch>
```

If they already exist, they may have been created before the spec commit. In
that case, they must be **rebased** onto the feature branch (step 4).

### 3) Create worktrees (if they do not exist)

For each milestone (M1, M2):

- Worktree path: e.g. `../OpenELIS-{NNN}-m1`, `../OpenELIS-{NNN}-m2` (sibling of
  main repo)
- Branch: the corresponding milestone branch
- Command: `git worktree add <path> <branch>`

If worktrees already exist, skip creation.

### 4) Rebase worktrees onto feature branch (if specs were just committed)

**If milestone branches were created before the spec commit**, they do not
contain the specs. Rebase each onto the feature branch:

For each worktree (M1, M2):

1. **Stash uncommitted work** (if any):
   - `cd <worktree-path>`
   - `git status --porcelain`
   - If dirty: `git stash push -u -m "WIP: <brief description>"`
2. **Rebase** onto feature branch:
   - `git rebase feat/<feature-branch>`
3. **Restore stash** (if stashed):
   - `git stash pop`
4. **Verify specs exist**:
   - `ls specs/{NNN}-*/spec.md` (or equivalent)

### 5) Verification

For each worktree, confirm:

- `specs/{NNN}-*/spec.md` exists
- `specs/{NNN}-*/tasks.md` exists
- `specs/{NNN}-*/plan.md` exists
- Branch is up to date with feat branch (or ahead by milestone-specific commits)

### 6) Post-setup report

Produce a concise report:

```
## Milestone Worktree Setup Complete

**Feature branch:** feat/<name> (specs committed)
**Main worktree:** <path> (branch: <current>)
**M1 worktree:** <path> (branch: feat/...-m1-...)
**M2 worktree:** <path> (branch: feat/...-m2-...)

Specs verified in all worktrees. You can run `/speckit.implement` in M1 or M2.
```

## Recovery Mode (`--recover`)

Use when:

- Specs were committed on the feature branch after worktrees were created
- Worktrees exist but lack `specs/{NNN}-*/` (or have stale versions)

Steps:

1. Confirm specs are committed on feat branch (step 0)
2. For each worktree: stash → rebase onto feat → pop stash (step 4)
3. Verify specs in each worktree (step 5)
4. Report (step 6)

## Integration with `/speckit.implement`

Before running `/speckit.implement` in a milestone worktree, the agent MUST
verify that `specs/{NNN}-*/` exists and contains spec.md, plan.md, tasks.md. If
not, prompt:

"Specs are missing in this worktree. Run `/setup-milestone-worktrees --recover`
from the main worktree to rebase milestone branches onto the feature branch that
has the committed specs."

## Troubleshooting

### "Worktrees don't have specs"

Cause: Milestone branches were created from the feature branch before specs were
committed.

Fix: Commit specs on the feature branch, then run
`/setup-milestone-worktrees --recover`.

### "Cannot rebase: uncommitted changes"

Cause: Worktree has local changes.

Fix: Stash first (`git stash push -u -m "WIP"`), rebase, then `git stash pop`.

### "Feature branch has no specs"

Cause: Specs were never committed.

Fix: Stage `specs/{NNN}-*/` and `docs/` (if applicable), commit on the feature
branch, then proceed with worktree setup.
