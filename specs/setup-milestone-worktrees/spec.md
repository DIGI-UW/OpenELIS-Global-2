# Spec: Setup Milestone Worktrees

## Problem

When using SpecKit milestone-based development (M1/M2 worktrees), agents and
developers often create milestone branches and worktrees _before_ committing
specs on the feature branch. The milestone branches are created from the feature
branch at that moment — so they do not include the specs. When
`/speckit.implement` runs in a worktree, it fails because `specs/{NNN}-*/` is
missing.

## Solution

Add a `/setup-milestone-worktrees` command that encodes the **spec-first**
workflow:

1. Commit specs on the feature branch _before_ creating milestone branches.
2. Create milestone branches from the feature branch (which now has specs).
3. If worktrees already exist but lack specs: rebase them onto the feature
   branch (`--recover`).

## Deliverables

- `.specify/oe/commands/setup-milestone-worktrees.md` — Command definition
- `.specify/oe/commands/speckit.implement.md` — Override to check for specs and
  prompt for `/setup-milestone-worktrees --recover` when missing

## Acceptance Criteria

- [ ] `/setup-milestone-worktrees` installs via
      `python3 scripts/install-speckit-commands.py`
- [ ] Command documents: commit specs first, create branches, create worktrees,
      rebase if needed
- [ ] `--recover` mode rebases existing worktrees onto feat branch
- [ ] `speckit.implement` OE override prompts for recovery when specs missing in
      worktree
