# Canvas UX Research — Design Foundations for `/speckit.overview`

**Purpose:** Persist the UX research that drove the OGC-285 overview-canvas shape (and the `/speckit.overview` skill's standard template), so the design principles outlive the people who chose them.

**Date conducted:** 2026-05-19 (during OGC-285 spec PR #3628)
**Research method:** focused web search across 6 authoritative UX sources, synthesized into actionable canvas-design rules.

## TL;DR

A spec-overview canvas surfaces dense technical content for stakeholders who need BOTH skimmable summaries AND drill-down access. The dominant UX pattern is **progressive disclosure**:

> "Show users only a few of the most important options initially, with a larger set of specialized options upon request." — Nielsen Norman Group

Concrete implications for our canvas:

1. **Maximum 2 disclosure levels** (tabs → expandable cards). Three or more nesting levels measurably hurt usability.
2. **5–7 summary cards** as the entry hero. Aligns with the 4–7 chunks working-memory limit.
3. **Strong information scent** — clear labels + count badges + status chips so users know what's inside before expanding.
4. **Visual examples alongside written specs** — diagrams, tables, ERDs, dependency graphs are first-class.
5. **Chunk before nesting** — when content grows, split into more tabs rather than deeper expandables.
6. **Clean surface + depth on demand** — the default view is uncluttered; detail is one click away.

## Source synthesis

### 1. Nielsen Norman Group — *Progressive Disclosure*

Source: https://www.nngroup.com/articles/progressive-disclosure/

**Two foundational requirements:**

- **Feature prioritization:** show only the most important options initially; reveal specialized options on request.
- **Discoverability:** users must understand both the mechanics and purpose of accessing deeper layers. The system must provide "strong information scent" — clear labeling that sets expectations for what lies beneath.

**Critical constraint:**

> "Designs that go beyond 2 disclosure levels typically have low usability because users often get lost." — NN/G

For documentation exceeding 3 layers, **chunk features into meaningful groupings** rather than adding another nesting level.

**Concrete patterns from this article:**

- Default-collapsed sections
- "Advanced options" buttons for tertiary detail
- Hierarchical navigation with progressive drill-down paths
- Summary-to-detail links

**Applied to our canvas:** tabs are L1, expandable cards inside tabs are L2. We never nest expandables inside expandables. When content overflows a tab, we add a NEW tab (chunk) rather than deepening.

### 2. UXPin Studio — *What Is Progressive Disclosure in UX? (2026)*

Source: https://www.uxpin.com/studio/blog/what-is-progressive-disclosure/

Highlights:
- Product documentation and data-dense dashboards both face cognitive overload; progressive disclosure is the canonical mitigation.
- High-level overviews with expandable details for those who want depth.
- Implementation patterns include consistent formatting templates + visual examples alongside written specifications.

**Applied to our canvas:** the Overview tab is the high-level executive view; every other tab is a deeper layer for stakeholders who need that specific concern (data model, API, decisions, testing). Each tab uses a consistent card pattern.

### 3. B2B Dashboard Information Architecture (2026) — DAR Design

Source: https://dardesign.io/blog/b2b-dashboard-information-architecture-2026

**Cognitive comfort zone:**

> "Humans can hold about 4-7 chunks of information at once. By starting with 5-7 summary cards and letting users expand on demand, the design keeps them in their cognitive comfort zone."

**Applied to our canvas:** the Overview tab hero is exactly 6 KPI cards (within the 5–7 range). The 9 tabs are at the upper edge of acceptable chunking; we won't add more without splitting the canvas.

### 4. Smart SaaS Dashboard Design Guide (2026) — F1Studioz

Source: https://f1studioz.com/blog/smart-saas-dashboard-design/

Key insights:
- Progressive disclosure prevents cognitive overload AND improves perceived performance (less rendered means less to wait for).
- Starting with high-level summaries and revealing details only when users request them reduces decision fatigue.

**Applied to our canvas:** even though our canvas is static (no real performance constraint), the visual pattern matches — clean Overview view, detail behind expanders, fast initial paint.

### 5. Drill-down Navigation — HPE Design System

Source: https://design-system.hpe.design/templates/drill-down-navigation

**When drill-down works:** information tree structures traversed left-to-right; mobile-friendly; minimal scrolling.

**When drill-down DOES NOT work:** when users frequently move between levels (drill-down hides siblings, forcing back-traversal).

**Applied to our canvas:** we chose **tab navigation** (sibling-visible) over drill-down (sibling-hidden) precisely because stakeholders frequently jump between concerns. A reviewer wanting to check "decisions" then "API" then "testing" benefits from always-visible tabs. We use expandable cards within tabs (a one-level drill-down on a single concern) where it's cheap.

### 6. Interaction Design Foundation — *Progressive Disclosure (2026)*

Source: https://ixdf.org/literature/topics/progressive-disclosure

Highlights:
- "Information architecture in motion" — progressive disclosure reduces cognitive load without removing depth.
- Supports "skimmability so users can quickly identify the path forward."

**Applied to our canvas:** every tab is labeled with a noun + count badge so a 3-second scan of the tab bar tells the user where to go.

## Direct rules baked into the canvas template

| Rule | Manifestation in `templates/canvas.html.template` |
|---|---|
| Max 2 disclosure levels | Tabs (L1) + expandable cards (L2). No nested expandables. |
| 5–7 KPI cards | Overview hero is exactly 6 cards (User Stories / Milestones / ACs / Endpoints / Tables / Demo Videos) |
| Strong information scent | Every tab has a count badge in its label; every card has a status chip before expansion |
| Visual examples | SVG ERD on Data Model tab; SVG dependency graph on Roadmap tab; AC × Milestone coverage matrix on Testing tab |
| Chunk before nesting | 9 tabs for 9 distinct concerns rather than nested-expandable categories |
| Clean default | Overview tab opens by default; one expandable card is `defaultOpen` per tab to demonstrate the pattern |
| Discoverability | Top-of-page header + breadcrumb-style links to Jira / PR / FRS pin SHA |
| Sibling-visible navigation | Tabs always visible (rather than drill-down hide-siblings) so stakeholders can jump between concerns |
| Markdown inline rendering | Spec Files tab renders source markdown via `marked` + DOMPurify — no leave-the-canvas to read the source |
| Light + dark mode | System preference default + manual toggle + localStorage persistence — accommodates user choice without imposing one |

## When to revise

This research backs canvas-shape decisions for the foreseeable future. Revise when:

1. **User testing shows >2 levels would help** for a specific concern. (Unlikely; current rule is robust.)
2. **A category outgrows its tab** — chunk into a new tab; don't deepen.
3. **Stakeholder feedback identifies a missing concern** — add a tab (within the 9-tab budget; if we'd exceed 10, consolidate first).
4. **New UX research challenges any of the 6 sources** — re-synthesize, update this file, mark the date.

## References (verbatim citations)

- [Progressive Disclosure — Nielsen Norman Group](https://www.nngroup.com/articles/progressive-disclosure/) — canonical reference; 2-level rule; strong information scent
- [What Is Progressive Disclosure in UX? — UXPin Studio (2026)](https://www.uxpin.com/studio/blog/what-is-progressive-disclosure/) — practical patterns for technical documentation
- [B2B Dashboard Information Architecture 2026 — DAR Design](https://dardesign.io/blog/b2b-dashboard-information-architecture-2026) — 5–7 summary cards cognitive comfort zone
- [Smart SaaS Dashboard Design Guide — F1Studioz (2026)](https://f1studioz.com/blog/smart-saas-dashboard-design/) — disclosure improves perceived performance
- [Drill down navigation — HPE Design System](https://design-system.hpe.design/templates/drill-down-navigation) — when to use drill-down vs. when to avoid it
- [Progressive Disclosure — Interaction Design Foundation (2026)](https://ixdf.org/literature/topics/progressive-disclosure) — skimmability + path forward
