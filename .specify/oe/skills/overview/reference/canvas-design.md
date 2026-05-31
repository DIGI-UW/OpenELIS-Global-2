# Canvas Design — Tab Inventory + When to Add/Remove Tabs

**Purpose:** Document what each canvas tab is for, what data it consumes, and
the decision rule for adding or removing tabs.

## Canonical 9-tab layout

| Tab          | Purpose                                                                                   | Required spec source                                         | KPI badge                 |
| ------------ | ----------------------------------------------------------------------------------------- | ------------------------------------------------------------ | ------------------------- |
| Overview     | Executive summary; "why this exists" + 6-card KPI hero + spec PR + FRS pin                | spec.md frontmatter + Overview section                       | none                      |
| Stories      | User stories with priority, milestone, AC count, surfaces, key takeaway                   | spec.md User Scenarios & Testing section                     | count of stories          |
| Data Model   | ERD diagram (SVG) + per-table cards + JSONB snapshot shape                                | data-model.md §2 + §4                                        | count of new tables       |
| Roadmap      | Milestone dependency graph (SVG) + per-milestone expandable cards + PR discipline callout | plan.md Milestone Table + Mermaid graph                      | count of milestones       |
| Decisions    | Resolved Open Questions + deliberate divergences                                          | spec.md Clarifications + research.md §2 + §3                 | count of Qs + divergences |
| API          | REST endpoints grouped by surface                                                         | contracts/openapi.yaml paths                                 | count of endpoints        |
| Testing      | Video proof callout + AC × Milestone coverage matrix + anti-mocking discipline            | spec.md FRs + plan.md Testing Strategy + tasks.md task table | count of ACs              |
| Spec Files   | Inline markdown rendering of each spec file with sidebar navigation                       | filesystem listing of spec dir + markdown content            | count of files            |
| PR + Commits | PR link + commit chronology + ready-for-review checklist                                  | git log + PR API                                             | count of commits          |

## Hide-when-empty rules

A tab is **hidden from the canvas** if its source data is missing. Examples:

- No `data-model.md` → Data Model tab hidden
- No `contracts/openapi.yaml` → API tab hidden
- Spec is pre-tasks (no `tasks.md`) → AC × Milestone coverage matrix on Testing
  tab omits the matrix; tab still renders

This means a spec that's mid-authoring still produces a useful canvas — it just
shows fewer tabs.

## When to add a new tab

Add a tab when:

- A new concern emerges that doesn't fit any existing tab AND would force >5
  expandable cards inside one of the existing tabs (chunking pressure)
- A specific stakeholder audience needs a focused entry point (e.g., "Security"
  tab if a feature has substantial security surface — RBAC scopes, threat model,
  audit log)

**Budget:** 9 tabs is current; adding tabs beyond ~11 forces the tab bar to
scroll, hurting "strong information scent". If we'd exceed 11, consolidate
existing tabs first.

## When to remove a tab

Remove a tab when:

- It consistently has near-empty content across features (signaling it isn't
  earning its slot)
- It's been replaced by a more focused tab (transient tab during a transition is
  fine; long-lived empty tab is not)

## Anti-patterns to avoid

- **Nested expandables.** If a card's content needs sub-sections, split the
  card; don't expand-within-expand.
- **Numbers-only summaries.** Every KPI card must have a label + sub-text +
  icon, not just a number.
- **Tabs that compete with each other.** Each tab is a distinct concern. If two
  tabs cover overlapping concerns, merge them.
- **Implementation details in the Overview.** Overview is for stakeholders who
  may not be engineers; defer code, paths, and Java/Python type details to the
  Data Model / API tabs.
- **Walls of paragraph text.** Use cards, chips, tables, diagrams. The canvas
  exists to AVOID forcing readers through dense prose; if we replicate dense
  prose inside it, we've failed.

## Color palette (Tailwind tones used semantically)

| Tone    | Used for                                                   |
| ------- | ---------------------------------------------------------- |
| sky     | Informational (FRS pin, PR link, default chips)            |
| emerald | Shipped / done / passing / resolved                        |
| amber   | In progress / draft / warning                              |
| rose    | Blocked / critical / destructive operations / dialog gates |
| purple  | Milestones / decisions / divergences                       |
| indigo  | Demo videos / a11y / cross-cutting                         |
| slate   | Neutral / metadata / borders                               |

Color is NEVER the sole status indicator (AC-27 / accessibility principle) —
text + icon always accompany color.

## SVG diagrams — current patterns

- **ERD** — boxes-and-arrows over a 760×420 viewBox. Solid borders for new
  entities; dashed borders for legacy entities referenced. Color-coded by
  category (emerald = new, slate = legacy, sky = snapshot-store, purple = link
  tables).
- **Dependency graph** — left-to-right milestone flow. Rectangles for nodes.
  Parallel milestones tagged `[P]`. Single arrowhead per edge.
- **AC coverage matrix** — semantic HTML table with monospace AC IDs, Chip
  components for milestone and story.

Future diagrams (architecture, sequence) should follow the same constraints:
viewBox-based SVG, color-coded by semantic category, no animation in the spec
canvas (the triage canvas had animation; the overview canvas does not —
different audiences).
