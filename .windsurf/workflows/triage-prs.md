---
description: Workflow for triage-prs
---

# triage-prs

# Triage PRs

Perform a robust, constitution-aligned triage session of open Pull Requests in
OpenELIS Global 2.

## User Input

First argument is optional. Use `--all` to triage all open PRs, or specify a PR
number (e.g. `/triage-prs 3324`) to focus triage on a single PR.

## OpenELIS Contextual Constraints (Smell Test Checklist)

Before triaging, internalize the core Constitution rules (AGENTS.md):

1. **No Spring Boot / No JUnit 5**: Reject any inclusion of Spring Boot or
   `org.junit.jupiter.*`.
2. **5-Layer Boundaries**: DAOs/Services/Controllers must stay in their lanes.
   Ensure `@Transactional` is constrained to the Service layer.
3. **No Carbon Deviations**: Any UI code _must_ utilize Carbon Design System
   (`@carbon/react`) — no custom CSS frameworks allowed.
4. **React Intl Mandate**: PRs cannot contain hardcoded UI text; all
   `.js`/`.jsx` text additions must use `intl.formatMessage`.
5. **No Migration Hacks**: Database changes _must_ use Liquibase XML format.

## Workflow

### Step 1: Execute PR Telemetry Fetch

Use the built-in script to fetch open PR telemetry safely:

```bash
python3 scripts/triage-prs.py $ARGUMENTS
```

This returns an AI-parsable JSON artifact summarizing the current open PRs,
their state, labels, author, and associated CI check status.

### Step 2: Formulate Triage Snapshot (Checkpoint 1)

Analyze the telemetry from Step 1. Filter out PRs that are already thoroughly
triaged unless directed otherwise. For the remaining/requested PRs, categorize
them into:

- 🔵 **Unlabeled PRs**: Lacking basic architectural labels (`frontend`,
  `backend`, etc.).
- 🟢 **Merge-Ready**: CI passes fully, and architectural bounds look solid.
- 🔴 **Blocked / Action Required**: CI fails or blatant Constitution violations
  suspected.

_Checkpoint:_ Present this snapshot to the user and ask which PRs they would
like to deeply drill into.

### Step 3: Deep Triage & Audit (Checkpoint 2)

For the selected PR, read the diff via standard workspace tools (e.g.
`gh pr diff <number>`).

- Run the PR against the **Smell Test Checklist**.
- Apply labels using `gh pr edit <number> --add-label "<label>"`.
- Present a review summary and draft recommendations to the user, blocking on
  their approval BEFORE merging or commenting.

## Safety & Ethics

- **NEVER** merge a PR without affirmative human review.
- Always check `statusCheckRollup` before asserting stability.
- Treat PHI boundary PRs with extreme scrutiny; recommend manual auditing for
  security.

---

_SpecKit workflow for OpenELIS Global 2_
