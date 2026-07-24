# OpenELIS UAT Review Harness — agent context

Operating context for an LLM/agent asked to **author UAT checklists** or reason
about the **reviewer feedback loop** for the OpenELIS demo instances. Everything
below is the live contract, not aspiration.

## What this is

A self-hosted authoring + feedback loop that lets stakeholders review in-progress
OpenELIS features against a structured checklist, and lets humans *or* agents
author those checklists from one source of truth.

- **Source of truth:** a Grist document ("UAT Checklists"). Humans edit it in the
  Grist UI; agents edit it over Grist's native MCP. Neither clobbers the other
  (edits target specific rows).
- **Delivery:** a reviewer overlay injected into each demo site reads the
  checklist live from Grist and captures pass/fail/na + freeform feedback.
- **Return path:** the reviewer downloads a Markdown+JSON report and pastes it
  into Claude, which triages it into Jira/GitHub items.

Two instances today: `amr` (Microbiology MVP, OGC-782) and `analyzers` (Analyzer
Types & Mapping, OGC-1054).

## Authoring — native Grist MCP

- **Endpoint:** `https://grist.openelis-global.org/api/mcp` (Streamable HTTP MCP)
- **Auth:** a Grist API key as `Authorization: Bearer <key>`. The key is box-side
  at `/home/ubuntu/oe-grist/.api-key` (admin user; full access — treat as secret).
- **Connect (Claude Code CLI):**
  ```bash
  claude mcp add --transport http grist https://grist.openelis-global.org/api/mcp \
    --header "Authorization: Bearer <grist-api-key>"
  ```
  (claude.ai web auto-connect via Grist's OIDC server + CIMD is not enabled yet —
  see "Phase B" in the harness README.)

### The data model

- Document **"UAT Checklists"**, id `hvZ4rzsyGJuqggkZBko8gc`.
- Table **`UAT_Meta`** — one row per instance: `instance, title, intro, jira`.
- Table **`UAT_Steps`** — one row per step:
  `instance, section, section_order (int), step_order (int), do, expect, route`.
- Reviewers see steps grouped by `section` and ordered by
  `section_order` then `step_order`. `route` is the app path a reviewer opens
  for that step (e.g. `/MicrobiologyWorklist`).

### Tools you'll use (of 38 `grist_*` tools)

`grist_list_docs`, `grist_get_tables`, `grist_get_table_columns`,
`grist_query_document` (SQL SELECT), `grist_list_records`, `grist_add_records`,
`grist_update_records`, `grist_remove_records`.

Read before you write — pull current rows so you append/update rather than
duplicate:

```
grist_query_document(doc_id, "SELECT id, section, section_order, step_order, \"do\"
  FROM UAT_Steps WHERE instance='amr' ORDER BY section_order, step_order")
```

Add a step (append to a section = reuse its `section_order`, next `step_order`):

```
grist_add_records(doc_id, "UAT_Steps", [{
  instance: "amr", section: "Drive the workflow (reviewer-performed)",
  section_order: 2, step_order: 2,
  do: "…the action the reviewer performs…",
  expect: "…the expected result / what to flag if wrong…",
  route: "/MicrobiologyWorklist"
}])
```

Write UAT steps as **verifiable checks** — a `do` a reviewer performs and an
`expect` they judge against. A missing feature is a legitimate step: the reviewer
marks it Fail, which is useful signal.

## Delivery — how a checklist reaches reviewers

The reviewer overlay on each demo site fetches
`/__review/uat-<instance>.json`. The umbrella router proxies that to a small
box-side read service that reshapes live Grist rows into the widget's JSON
(30-second serve-stale cache). So an edit in Grist or via MCP shows up in the
overlay within ~30s — no publish step.

## Feedback — the report

The reviewer marks each step (pass/fail/na) + optional notes, then downloads
`oe-review-<instance>-<timestamp>.md` and `.json`. The Markdown carries a
per-section checklist with `[PASS]/[FAIL]/[N/A]/[----]` boxes, a summary line,
and freeform feedback; the JSON is the same data structured
(`{instance, summary, checklist:[{section, steps:[{do, expect, route, mark, note}]}], feedback}`).
The footer instructs the reviewer to paste it into Claude to triage into
Jira/GitHub.

**Your job on the return trip:** turn a pasted report into ranked, actionable
items — group by severity, map each FAIL / critical note to a concrete
story/task under the instance's Jira epic, and confirm what passed. Draft the
issues; don't file them unless asked.

## Gotchas

- Native MCP is **authed with a full-access key** — never expose it to a browser
  or an untrusted client. The widget read path is the only anonymous surface, and
  it is read-only.
- Grist runs the **full edition**, activated by `/persist/config.json`
  (`{"version":"1","edition":"enterprise"}`) + `GRIST_MCP_ENABLED=true`. Removing
  that file reverts to community (rollback-safe; data untouched).
- Don't `git checkout -f` the deploy checkout on the box — it reverts box-side
  secrets (the Dex login hash) and breaks Grist login.
