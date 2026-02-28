# Start Ticket

Set up a development environment for a Jira ticket: fetch details, create
branch, optionally create a draft PR and transition Jira status. Integrates with
SpecKit for spec-driven development.

## User Input

```text
$ARGUMENTS
```

First argument is the Jira issue key. Additional flags:

- `/start-ticket OGC-337` → Create branch, draft PR, transition to In Progress
- `/start-ticket OGC-337 --speckit` → Also bootstrap SpecKit spec from ticket
- `/start-ticket OGC-337 --worktree` → Use a git worktree (parallel dev)
- `/start-ticket OGC-337 --no-pr` → Skip PR creation
- `/start-ticket OGC-337 --no-transition` → Don't move Jira status
- `/start-ticket OGC-337 --base demo/madagascar` → Branch from non-default base

**Default base branch:** `develop`

## Atlassian Config

- **Cloud ID:** `57b4e32d-23d4-4a71-8985-82ac0274d145`
- **Site:** `uwdigi.atlassian.net`

## Workflow

### Step 1: Fetch Jira Ticket

Use `mcp__claude_ai_Atlassian__getJiraIssue` with cloud ID above. Fields:
`["summary", "description", "status", "priority", "issuetype", "labels", "sprint", "parent", "issuelinks"]`

Display a brief summary:

```
OGC-337 | Story | Medium | Selected for Development
Implement Generic ASTM Plugin v1.1/1.2
Labels: Madagascar | Parent: OGC-49
```

### Step 2: Create Branch

**Branch name pattern:** `<type>/<KEY>-<slug>`

| Jira Type | Prefix |
| --------- | ------ |
| Bug       | fix    |
| Story     | feat   |
| Task      | chore  |
| Epic      | epic   |
| Sub-task  | feat   |

Slugify: lowercase, hyphens, truncate to ~50 chars at word boundary.

Example: `feat/OGC-337-implement-generic-astm-plugin-v1`

**Confirm** the branch name with the user before creating.

```bash
git fetch origin $BASE_BRANCH
git checkout -b "$BRANCH_NAME" "origin/$BASE_BRANCH"
```

**If `--worktree`:**

```bash
WORKTREE_DIR="../worktrees/$(basename $(pwd))-$TICKET_KEY"
mkdir -p "$(dirname $WORKTREE_DIR)"
git worktree add -b "$BRANCH_NAME" "$WORKTREE_DIR" "origin/$BASE_BRANCH"
```

Report the worktree path so the user can `cd` there.

### Step 3: Draft PR (unless `--no-pr`)

Push and create a draft PR:

```bash
git push -u origin "$BRANCH_NAME"
```

**PR title:** `<type>(<project>): <short summary> (<KEY>)` **PR body:**
Generated from Jira description — summary bullets, link to Jira ticket,
acceptance criteria if available.

```bash
gh pr create --draft --title "$TITLE" --body "$BODY" --base "$BASE_BRANCH"
```

### Step 4: Transition Jira (unless `--no-transition`)

Use `mcp__claude_ai_Atlassian__getTransitionsForJiraIssue` to find "In Progress"
transition. If available, use `mcp__claude_ai_Atlassian__transitionJiraIssue`.

If already In Progress, skip silently.

### Step 5: SpecKit Bootstrap (only with `--speckit`)

Determine the next spec number from `specs/` directory. Then invoke
`/speckit.specify` using the Jira ticket description as the feature input.

This creates `specs/NNN-<feature>/spec.md` seeded from the Jira ticket.

After SpecKit completes, remind the user of the next steps:

```
SpecKit pipeline:
  /speckit.plan      → Design the implementation
  /speckit.tasks     → Generate task breakdown
  /speckit.implement → Execute tasks with TDD
```

### Step 6: Summary

```
Started: OGC-337 — Implement Generic ASTM Plugin v1.1/1.2

  Branch:  feat/OGC-337-implement-generic-astm-plugin-v1
  PR:      #42 (draft) — https://github.com/...
  Jira:    Transitioned to In Progress
  SpecKit: specs/012-astm-plugin-v1/spec.md created

  Next: /speckit.plan
```

## Lifecycle & Jira Sync

This command is the **entry point** of the development cycle. It syncs Jira to
"In Progress." Other commands handle later transitions:

```
/daily-priorities             ← Pick what to work on
/start-ticket OGC-337        ← YOU ARE HERE: Jira → "In Progress"
  /speckit.* pipeline         ← Spec → Plan → Tasks → Implement
  /fix-ci                     ← Fix CI failures
  /careful-rebase             ← Stay current with base
/finish-ticket                ← Audit + Jira → "In Review"
  /address-pr-comments        ← Handle reviewer feedback
```

## Safety

- Never force-push or push to protected branches
- Always create **draft** PRs
- Confirm branch name before creating
- If branch exists, offer to check it out instead
