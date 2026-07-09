# Finish Ticket

Final polish, audit, and validation before marking a ticket ready for review.
Chains existing commands (`/audit-branch`, formatting, tests) and syncs Jira
status to "In Review."

This is the **exit gate** of the development cycle — run it when you believe
your work is complete and you want a thorough check before requesting review.

## User Input

```text
$ARGUMENTS
```

**Flags:**

- `/finish-ticket` → Full audit + format + test + Jira sync + PR ready
- `/finish-ticket --quick` → Format + compile only (skip audit and tests)
- `/finish-ticket --no-jira` → Skip Jira status transition
- `/finish-ticket --no-pr` → Skip PR readiness marking
- `/finish-ticket --deep` → Pass `--deep` to audit-branch (Tier 3 semantic)
- `/finish-ticket OGC-337` → Explicit Jira key (otherwise auto-detected from
  branch name)

## Atlassian Config

- **Cloud ID:** `57b4e32d-23d4-4a71-8985-82ac0274d145`
- **Site:** `uwdigi.atlassian.net`

## Workflow

### Step 1: Preflight

Gather context (all parallel):

```bash
git branch --show-current          # Branch name
git status --porcelain             # Uncommitted changes
gh pr view --json number,title,baseRefName,url,isDraft  # PR info
```

- Extract **Jira key** from branch name (pattern: `<type>/<KEY>-<slug>`) or from
  the `$ARGUMENTS`
- If uncommitted changes exist, **warn and ask**: commit first or continue?
- If no PR exists, warn but continue (user may want to create one after)

Report:

```
Branch:  feat/OGC-337-implement-generic-astm-plugin-v1
PR:      #42 (draft) → develop
Jira:    OGC-337
Status:  2 uncommitted files (warn)
```

### Step 2: Format (mandatory)

Run formatting per CLAUDE.md rules — this is non-negotiable before any audit.

**Backend (if Java files changed in branch):**

```bash
mvn spotless:apply
```

**Frontend (if JS/JSX/TS/TSX files changed in branch):**

```bash
cd frontend && npm run format && cd ..
```

Check if formatting produced changes:

```bash
git diff --stat
```

If formatting changed files, **stage and commit them** with message:
`style: apply formatting (spotless + prettier)`

### Step 3: Compile & Unit Tests

**Skip if `--quick` and no test files changed.**

**Backend:**

```bash
mvn test
```

If tests fail → stop and report. Don't proceed to audit with failing tests.

**Frontend (if frontend files changed):**

```bash
cd frontend && npm test -- --watchAll=false --coverage=false && cd ..
```

If tests fail → stop and report.

### Step 4: Audit Branch

**Skip if `--quick`.**

Run the `/audit-branch` command logic (Tier 1+2 by default, Tier 3 if `--deep`
was passed).

This checks for:

- Debug statements, TODOs, placeholder code
- Merge conflict markers, swallowed exceptions
- Constitution violations (layered architecture, Carbon DS, i18n)
- Scope creep, over-engineering, unused imports

**If CRITICAL or HIGH findings exist:** Stop and present findings. The user must
address them before proceeding.

**If only MEDIUM/LOW/INFO findings:** Present findings as advisory but continue.
Ask: "Address these now, or proceed to PR?"

### Step 5: PR Readiness

**Skip if `--no-pr` or no PR exists.**

Check PR is up to date with base branch:

```bash
git fetch origin
git log --oneline origin/$BASE_BRANCH..HEAD  # Commits ahead
git log --oneline HEAD..origin/$BASE_BRANCH  # Commits behind
```

If behind base branch, suggest:

- `/careful-rebase` to bring up to date

If PR is currently draft, offer to mark as ready:

```bash
gh pr ready
```

Ensure PR description includes a link to the Jira ticket. If missing, update:

```bash
gh pr edit --body "..."   # Append Jira link if not present
```

### Step 6: Jira Transition

**Skip if `--no-jira` or no Jira key detected.**

Use `mcp__claude_ai_Atlassian__getTransitionsForJiraIssue` (cloud ID:
`57b4e32d-23d4-4a71-8985-82ac0274d145`) to find a transition to "In Review",
"Review", or "Code Review" (try common variants).

If found, use `mcp__claude_ai_Atlassian__transitionJiraIssue` to move it.

If no review-like transition exists, list available transitions and ask the user
which one to use (or skip).

### Step 7: Summary

```
## Finish Ticket — OGC-337

| Check         | Result                     |
| ------------- | -------------------------- |
| Formatting    | Applied (1 file changed)   |
| Unit tests    | 142 passed, 0 failed       |
| Branch audit  | 0 critical, 1 medium       |
| PR #42        | Marked ready for review    |
| Jira OGC-337  | Transitioned to In Review  |

### Audit Findings (advisory)
- [M4] LOW: Hedging language in comment (ServiceImpl.java:89)

### What's Next
- Wait for reviewer feedback
- When comments arrive: `/address-pr-comments`
- If CI fails after push: `/fix-ci`
- After merge: Jira ticket will need manual transition to Done
  (or set up automation in Jira)
```

## Lifecycle Reference

This command is part of the DIGI development workflow:

```
/daily-priorities             ← Pick what to work on (Jira → you)
/start-ticket OGC-337        ← Branch + PR + Jira "In Progress"
  /speckit.specify            ← Define the feature
  /speckit.plan               ← Design approach
  /speckit.tasks              ← Break into tasks
  /speckit.implement          ← Code with TDD
  /fix-ci                     ← Fix CI failures
  /careful-rebase             ← Stay current with base
/finish-ticket                ← YOU ARE HERE: audit + Jira "In Review"
  /address-pr-comments        ← Handle reviewer feedback
  /fix-ci                     ← Fix post-review CI issues
/finish-ticket                ← Re-run after addressing comments
```

## Safety

- Never force-push
- Never skip formatting
- Stop on CRITICAL audit findings — don't mark PR ready with known issues
- Ask before transitioning Jira status
- Ask before marking PR as ready (removing draft)
