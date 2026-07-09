# Daily Priorities

Query DIGI Jira via the Atlassian MCP to build a prioritized daily work plan.
The report has two parts: **suggested priorities** (the recommendation) and
**full context** (everything you need to evaluate whether the suggestions are
right and what else is on the docket).

After presenting, suggest `/start-ticket <KEY>` to begin work.

## User Input

```text
$ARGUMENTS
```

**Flags:**

- `/daily-priorities` → Your tickets + unassigned across main projects
- `/daily-priorities --project OGC` → Filter to one project
- `/daily-priorities --mine-only` → Skip unassigned query
- `/daily-priorities --confluence` → Also pull recent Confluence context
- `/daily-priorities --deep` → Fetch full descriptions for top tickets

## Atlassian Config

- **Cloud ID:** `57b4e32d-23d4-4a71-8985-82ac0274d145`
- **Site:** `uwdigi.atlassian.net`
- **Main projects:** OGC, MG, OEDIGI, HAITI (used for unassigned query)

## Workflow

### Step 1: Fetch Tickets

Run **three** JQL queries in parallel using
`mcp__claude_ai_Atlassian__searchJiraIssuesUsingJql` (cloud ID above).

**Query A — Your assigned open tickets:**

```
assignee = currentUser() AND status != Done ORDER BY status DESC, priority ASC, updated DESC
```

Fields:
`["summary", "status", "priority", "issuetype", "duedate", "labels", "sprint", "updated"]`
Max: 50

**Query B — Your overdue tickets:**

```
assignee = currentUser() AND status != Done AND duedate < now() ORDER BY duedate ASC
```

Fields: same. Max: 20

**Query C — Unassigned tickets across main projects** (skip if `--mine-only`):

```
assignee is EMPTY AND status != Done AND project in (OGC, MG, OEDIGI, HAITI) ORDER BY priority ASC, updated DESC
```

Fields: same. Max: 50

If `--project` is specified, append `AND project = "<KEY>"` to queries A and B,
and use only that project for query C.

### Step 1b: Confluence (only with `--confluence`)

Use `mcp__claude_ai_Atlassian__search` with query:
`"sprint planning" OR "weekly priorities" OR "standup"`

Read the top 1-2 relevant pages for context.

### Step 1c: Deep Fetch (only with `--deep`)

For the top 5 highest-priority tickets from queries A+C, use
`mcp__claude_ai_Atlassian__getJiraIssue` to get full descriptions and comments.

### Step 2: Process Results

**Because Jira descriptions can be very large**, extract results using a
lightweight approach — pipe the JSON through Python or jq to pull only the
fields needed for the report tables. Do NOT try to read the full raw JSON
inline.

Example extraction (adapt path to actual result file):

```bash
cat $RESULT_FILE | python3 -c "
import json, sys
data = json.load(sys.stdin)
for i in data['issues']['nodes']:
    f = i['fields']
    print('|'.join([
        i['key'],
        f['issuetype']['name'],
        f['priority']['name'],
        f['status']['name'],
        f.get('duedate') or '-',
        ', '.join(f.get('labels') or []),
        f.get('updated','')[:10],
        f['summary'][:80]
    ]))
"
```

Deduplicate by issue key across all queries.

### Step 3: Build the Report

The report has **two major sections**: a short recommendation up front, then the
full data so the user can evaluate it. Structure it exactly like this:

```markdown
## Daily Priorities — [Date]

### Suggested Focus (top 3)

1. **KEY** — Summary _[Why: deadline in N days / only High priority / active
   momentum / unblocks N other tickets / production issue]_

2. **KEY** — Summary _[Why: ...]_

3. **KEY** — Summary _[Why: ...]_

> `/start-ticket KEY` to begin | `/start-ticket KEY --speckit` for spec-driven

---

### Your Tickets ([N] assigned to you)

**[M] In Progress | [M] To Do / Selected | [M] Backlog | [M] Overdue**

#### In Progress

_Finish these before starting new work. Context switching is expensive._

| Key | Summary | Priority | Due | Labels | Updated |
| --- | ------- | -------- | --- | ------ | ------- |
| ... | ...     | ...      | ... | ...    | ...     |

#### Overdue

_Past due date — address or reschedule._

| Key | Summary | Due | Days Late | Priority |
| --- | ------- | --- | --------- | -------- |
| ... | ...     | ... | ...       | ...      |

_(If none: "None — you're current on deadlines.")_

#### To Do + Selected for Development

_Ready to start. Sorted by priority then recency._

| Key | Summary | Priority | Type | Labels | Updated |
| --- | ------- | -------- | ---- | ------ | ------- |
| ... | ...     | ...      | ...  | ...    | ...     |

#### Backlog (To be assigned status / Low priority)

_Parked. Don't start these unless the above sections are empty._

| Key | Summary | Type | Labels | Updated |
| --- | ------- | ---- | ------ | ------- |
| ... | ...     | ...  | ...    | ...     |

---

### Unassigned Pool ([N] tickets without an owner)

_These are open tickets across OGC, MG, OEDIGI, HAITI with no assignee. Items
here may need your attention or could be claimed for the sprint._

#### Needs Attention (recently created, high priority, or production issues)

| Key | Summary | Priority | Status | Project | Updated |
| --- | ------- | -------- | ------ | ------- | ------- |
| ... | ...     | ...      | ...    | ...     | ...     |

_(Flag: crash reports, "Down" in title, Highest/High priority, created today)_

#### Unassigned In Progress (orphaned — someone started but no owner)

| Key | Summary | Status | Project | Labels | Updated |
| --- | ------- | ------ | ------- | ------ | ------- |
| ... | ...     | ...    | ...     | ...    | ...     |

_(If none: skip this section)_

#### Unassigned Backlog (informational)

**[N] more unassigned tickets** in backlog across OGC ([n]), MG ([n]), HAITI
([n]), OEDIGI ([n]).

[Top 5-10 by recency, then "and N more..."]

---

### Flags & Observations

- **Stale tickets:** [Any assigned tickets with no update in 14+ days]
- **Junk/test tickets:** [Any obvious test data that should be deleted]
- **Past-due dates:** [Tickets with due dates in the past that aren't overdue
  status — Jira metadata mismatch]
- **Dependency chains:** [If ticket X unblocks Y, Z — note it]
- **Deployment labels:** Country labels (Madagascar, Indonesia, PNG) = active
  deployments with real users. Note these as higher urgency.
```

### Step 4: Offer Follow-ups

After the report, offer these actions:

- "Want details on a specific ticket? I can fetch the full description."
- "Ready to start one? `/start-ticket OGC-337`"
- "Want to claim an unassigned ticket? I can assign it to you in Jira."
- "Need Confluence context? Re-run with `--confluence`"

## Report Design Principles

- **Suggestions up front, context below.** The user should see the
  recommendation in 5 seconds, then have everything they need to challenge it
  without asking follow-up questions.
- **Make assignment crystal clear.** Every table should make it obvious whether
  tickets are yours or unassigned. Never mix them in the same table.
- **Surface anomalies.** Orphaned In Progress tickets, stale items, test junk,
  broken due dates — these are the things a human would miss scanning Jira
  boards.
- **Country labels = urgency.** Madagascar, Indonesia, PNG labels indicate real
  deployments. Always factor these into suggestions.
- **Counts everywhere.** Header counts let the user gut-check scope instantly:
  "23 assigned, 50 unassigned, 0 overdue" tells a story before reading any
  table.

## Lifecycle & Jira Sync

This command is the **triage step** — read-only against Jira. The full
development cycle syncs Jira status at key milestones:

```
/daily-priorities             ← YOU ARE HERE: read tickets from Jira
/start-ticket OGC-337        ← Jira → "In Progress"
  /speckit.* pipeline         ← Spec → Plan → Tasks → Implement
  /fix-ci                     ← Fix CI failures
/finish-ticket                ← Jira → "In Review"
  /address-pr-comments        ← Handle reviewer feedback
```
