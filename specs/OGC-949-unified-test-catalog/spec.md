# Feature Specification: Unified Test Catalog Management Editor (Test Catalog v2.5)

**Feature Branch**: `spec/ogc-949-unified-test-catalog` (renamed from auto-created `949-unified-test-catalog`)
**Created**: 2026-06-10
**Status**: Draft (program spec authoring; awaiting non-Copilot human review)
**Issue**: [OGC-949 — Test Catalog Management v2.5 — Unified Editor (umbrella, v1 + v2)](https://uwdigi.atlassian.net/browse/OGC-949)
**Canonical FRS** (pinned at DIGI-UW/openelis-work @ `f04cce54ad8278f4504e9f454f7d61157a501931`):

- Full vision FRS v2.4 (~3,275 lines, all 14 sections): [`designs/admin-config/test-catalog/test-catalog-requirements-v2.4.md`](https://github.com/DIGI-UW/openelis-work/blob/f04cce54ad8278f4504e9f454f7d61157a501931/designs/admin-config/test-catalog/test-catalog-requirements-v2.4.md)
- Staging companion v2.5 (**authoritative** for the v1/v2 wave split + health-check fixes D-01..D-11): [`test-catalog-requirements-v2.5.md`](https://github.com/DIGI-UW/openelis-work/blob/f04cce54ad8278f4504e9f454f7d61157a501931/designs/admin-config/test-catalog/test-catalog-requirements-v2.5.md)
- v1 interactive mockup: [`test-catalog-preview-v2.5-v1.html`](https://github.com/DIGI-UW/openelis-work/blob/f04cce54ad8278f4504e9f454f7d61157a501931/designs/admin-config/test-catalog/test-catalog-preview-v2.5-v1.html)

**Confluence delivery plan** (stakeholder view, 20-epic structure): [Test Catalog Management v2.5 — Delivery Plan](https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/1313865740)

This specification is the engineering contract and the **integrator** of the
many sources above. The FRS is the authoritative design source; the Jira child
stories are the authoritative acceptance-criteria source. **Prose stays in the
FRS; per-section acceptance criteria stay in Jira; program-level requirements,
sequencing, and source traceability live here.**

## Overview

Replace OpenELIS Global's legacy **Test / Test Section / Panel / Method** admin
pages with a single **SideNav-routed Test Catalog editor** organized into 14
sections. A lab administrator selects a test from a filterable list view and
configures everything about that test — identity, sample types, result
components, methods, reference ranges, storage, display order, panels,
terminology, analyzers, and (in the second wave) labels, reagents, alerts,
reflex/calc cross-links, and compliance thresholds — without leaving the
editor or touching the old scattered admin surfaces.

Delivery is **two waves**:

- **v1** (12 Jira epics, 49 child stories): the schema foundation, the editor
  shell, the test list view, and 9 functional sections. v1 ships as a **direct
  replacement** — the legacy admin pages are decommissioned at v1 release with
  **no feature flag**. Rollback is a release revert; deprecated legacy columns
  retained for one release cycle are the data-level safety net (see
  [data-model.md](./data-model.md)).
- **v2** (8 Jira epics, 23 child stories): Labels, Reagents, Alerts, Reflex &
  Calc, Compliance, Sample Storage audit history, and Localization Hardening.
  v2 SideNav entries are **hidden entirely in v1** (not "Coming Soon" stubs).
  v2 is **named but not task-elaborated** in this feature; its milestones are
  carried for dependency reasoning and elaborated via a `/speckit.plan`
  revision at v2 kickoff.

Sibling story [OGC-757](https://uwdigi.atlassian.net/browse/OGC-757) (Sample
Storage display propagation to Order Entry / Results / Validation screens) is
**out of scope** for this feature — referenced for context only, as it touches
three non-catalog screens.

### Headline capabilities (v1)

- **Domain** attribute (CLINICAL / ENVIRONMENTAL / VECTOR) on every test, with
  domain-conditional section visibility.
- **AMR flag + WHONET configuration** for clinical-safety + GLASS surveillance
  (Madagascar).
- **Result Components** (multi-component tests, e.g. blood pressure,
  spirometry). FHIR mapping is **locked** (fix D-02): one `Observation` per
  component — never `Observation.component[]`.
- **Hour-granular reference ranges** with **Coverage Validation** and an
  **Activation Acknowledgment** modal: warn on save, never block save, but
  block _Activate_ until gaps are explicitly acknowledged and logged (fix
  H-03) — the neonatal-bilirubin patient-safety win.
- **"Save as new test…"** duplication workflow in the editor header.
- **`admin.testCatalog.manage`** permission gating the entire surface (UI hide
  - API 403).

## Source Map (milestone → Jira → FRS → mockup)

The integrator table. Each v1 milestone maps to exactly one Jira epic; detailed
acceptance criteria live in that epic's child stories (linked). FRS section
references point into the pinned v2.5 staging companion (and v2.4 for full
prose).

| MS          | Jira epic                                                                                                                                                                                                                                                                                                                                                                                                                        | Child stories                                                                                                                                                                                                                                                      | FRS § (v2.5)                  | Mockup section   |
| ----------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------- | ---------------- |
| **M0**      | _(reconciliation — no epic)_                                                                                                                                                                                                                                                                                                                                                                                                     | —                                                                                                                                                                                                                                                                  | §0.5, §3.2 (D-07/D-08), §2.10 | —                |
| **M1**      | [OGC-747](https://uwdigi.atlassian.net/browse/OGC-747) Schema Migrations + Backend Foundation                                                                                                                                                                                                                                                                                                                                    | 5 — [936](https://uwdigi.atlassian.net/browse/OGC-936)/[937](https://uwdigi.atlassian.net/browse/OGC-937)/[938](https://uwdigi.atlassian.net/browse/OGC-938)/[939](https://uwdigi.atlassian.net/browse/OGC-939)/[940](https://uwdigi.atlassian.net/browse/OGC-940) | §0.6                          | —                |
| **M2**      | [OGC-927](https://uwdigi.atlassian.net/browse/OGC-927) Editor Scaffold + Permissions + States                                                                                                                                                                                                                                                                                                                                    | 4 — [941](https://uwdigi.atlassian.net/browse/OGC-941)–[944](https://uwdigi.atlassian.net/browse/OGC-944)                                                                                                                                                          | §2.9, §2.10, §2.11            | Editor scaffold  |
| **M3**      | [OGC-928](https://uwdigi.atlassian.net/browse/OGC-928) Test List View + Filters + Pagination                                                                                                                                                                                                                                                                                                                                     | 4 — [945](https://uwdigi.atlassian.net/browse/OGC-945)–[948](https://uwdigi.atlassian.net/browse/OGC-948)                                                                                                                                                          | §2.9                          | Test List View   |
| **M4**      | [OGC-748](https://uwdigi.atlassian.net/browse/OGC-748) Basic Info                                                                                                                                                                                                                                                                                                                                                                | 4 — [950](https://uwdigi.atlassian.net/browse/OGC-950)–[953](https://uwdigi.atlassian.net/browse/OGC-953)                                                                                                                                                          | §2.1                          | Basic Info       |
| **M5**      | [OGC-749](https://uwdigi.atlassian.net/browse/OGC-749) Sample & Results                                                                                                                                                                                                                                                                                                                                                          | 8 — [961](https://uwdigi.atlassian.net/browse/OGC-961)–[968](https://uwdigi.atlassian.net/browse/OGC-968)                                                                                                                                                          | §2.2                          | Sample & Results |
| **M6**      | [OGC-750](https://uwdigi.atlassian.net/browse/OGC-750) Methods                                                                                                                                                                                                                                                                                                                                                                   | 3 — [954](https://uwdigi.atlassian.net/browse/OGC-954)–[956](https://uwdigi.atlassian.net/browse/OGC-956)                                                                                                                                                          | §2.3                          | Methods          |
| **M7**      | [OGC-751](https://uwdigi.atlassian.net/browse/OGC-751) Ranges + Coverage Validation                                                                                                                                                                                                                                                                                                                                              | 8 — [969](https://uwdigi.atlassian.net/browse/OGC-969)–[976](https://uwdigi.atlassian.net/browse/OGC-976)                                                                                                                                                          | §2.4                          | Ranges           |
| **M8**      | [OGC-752](https://uwdigi.atlassian.net/browse/OGC-752) Sample Storage                                                                                                                                                                                                                                                                                                                                                            | 3 — [977](https://uwdigi.atlassian.net/browse/OGC-977)–[979](https://uwdigi.atlassian.net/browse/OGC-979)                                                                                                                                                          | §2.5                          | Sample Storage   |
| **M9**      | [OGC-753](https://uwdigi.atlassian.net/browse/OGC-753) Panels                                                                                                                                                                                                                                                                                                                                                                    | 3 — [980](https://uwdigi.atlassian.net/browse/OGC-980)–[982](https://uwdigi.atlassian.net/browse/OGC-982)                                                                                                                                                          | §2.6                          | Panels           |
| **M10**     | [OGC-754](https://uwdigi.atlassian.net/browse/OGC-754) Terminology Mappings                                                                                                                                                                                                                                                                                                                                                      | 2 — [957](https://uwdigi.atlassian.net/browse/OGC-957)/[958](https://uwdigi.atlassian.net/browse/OGC-958)                                                                                                                                                          | §2.7                          | Terminology      |
| **M11**     | [OGC-755](https://uwdigi.atlassian.net/browse/OGC-755) Analyzers (read-only)                                                                                                                                                                                                                                                                                                                                                     | 2 — [959](https://uwdigi.atlassian.net/browse/OGC-959)/[960](https://uwdigi.atlassian.net/browse/OGC-960)                                                                                                                                                          | §2.8                          | Analyzers        |
| **M12**     | [OGC-756](https://uwdigi.atlassian.net/browse/OGC-756) Display Order                                                                                                                                                                                                                                                                                                                                                             | 3 — [983](https://uwdigi.atlassian.net/browse/OGC-983)–[985](https://uwdigi.atlassian.net/browse/OGC-985)                                                                                                                                                          | §2.5b                         | Display Order    |
| **M-DC**    | [OGC-747](https://uwdigi.atlassian.net/browse/OGC-747) / [OGC-940](https://uwdigi.atlassian.net/browse/OGC-940) Legacy decommission + release readiness                                                                                                                                                                                                                                                                          | (OGC-940)                                                                                                                                                                                                                                                          | §2.11, §0.5                   | —                |
| **M13–M20** | v2 wave: [760](https://uwdigi.atlassian.net/browse/OGC-760)/[761](https://uwdigi.atlassian.net/browse/OGC-761)/[762](https://uwdigi.atlassian.net/browse/OGC-762)/[763](https://uwdigi.atlassian.net/browse/OGC-763)/[764](https://uwdigi.atlassian.net/browse/OGC-764)/[765](https://uwdigi.atlassian.net/browse/OGC-765)/[766](https://uwdigi.atlassian.net/browse/OGC-766)/[767](https://uwdigi.atlassian.net/browse/OGC-767) | 23                                                                                                                                                                                                                                                                 | §3.2–§3.8                     | (deferred)       |

> Note: M6 (Methods / OGC-750) was implemented on the `demo-silnas` branch (PR
> [#3706](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3706), merged
> 2026-06-10) and is **ported to `develop`**, not reimplemented — see M0 and
> [research.md](./research.md). Legacy decommission (OGC-940, filed under
> OGC-747) is executed as the final v1 gate **M-DC**, not in M1, so the new
> editor is proven before the old pages are deleted.

## Clarifications

_Populated by `/speckit.clarify`. The four locked decisions below were supplied
with the feature description and are recorded here so downstream commands do not
re-ask them._

### Session 2026-06-10 — Locked program decisions

- **Scope** → Whole umbrella. v1 (M0–M12 + M-DC) fully elaborated into
  milestones and tasks; v2 (M13–M20) named but not task-elaborated (carried for
  dependency reasoning, elaborated at v2 kickoff).
- **Integration branch** → `develop` for **all** OGC-949 milestone PRs. Valid
  parts are **manually ported** from `demo-silnas` (first: Methods PR #3706);
  wholesale `demo-silnas` reconciliation is **deferred** until that branch
  merges back to develop. demo-silnas is ~19 commits ahead carrying
  vector/environmental/compliance/NCE work (see [research.md](./research.md)).
- **Jira structure is leveraged, not duplicated** → plan.md/tasks.md mirror the
  existing 20-epic / 72-story Jira structure with inline links, validating and
  paring it to what is dev-critical during generation. Discrepancies (e.g.
  OGC-747 child-count drift between the OGC-949 description and Confluence) are
  recorded in research.md and flagged for Jira-side fixes, never silently
  diverged from.
- **M0 = reconciliation milestone** inside this feature: (a) port Methods PR
  #3706 to develop; (b) the OGC-285 PR
  [#3676](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3676) ↔ v2 Labels
  ([OGC-761](https://uwdigi.atlassian.net/browse/OGC-761)) boundary is
  **resolved** (see session below) — M0 only records it; (c) sequence PR
  [#3546](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3546) (admin SideNav
  consolidation) vs OGC-927; (d) design the `admin.testCatalog.manage` gating
  using the **existing simple role gate** (`hasRole('ADMIN')`; see session
  below). M0 carries no user story (engineering work; lives in plan/tasks only).

### Session 2026-06-10 — `/speckit.clarify`

- Q: How is `admin.testCatalog.manage` (FR-004) realized, given the codebase
  uses coarse `hasRole('ADMIN')` on all admin REST controllers? → A: **Use the
  simple role gate now.** v1 gates both the UI (hide the menu entry) and the REST
  API (`@PreAuthorize("hasRole('ADMIN')")`) on the existing `ADMIN` role —
  matching every existing OE admin page. `admin.testCatalog.manage` is the
  **logical name** for this gate, not a separate grant. A fine-grained
  privilege-based RBAC effort
  ([OGC-384](https://uwdigi.atlassian.net/browse/OGC-384), PR
  [#3443](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3443)) is in flight
  but **not merged**; OGC-949 must **not** depend on or block on it. Migrating the
  gate to a real privilege when OGC-384 lands is a future follow-up (noted in
  research.md), not v1 scope. FR-004 updated.
- Q: Does the v2 Labels section (OGC-761) own a label-preset data model, or
  consume OGC-285's? → A: **Consume OGC-285's.** The `test_label_preset_link`
  table on `feat/ogc-285-integration` (PR #3676) was purpose-built for OGC-761 —
  its changeset comment states it CREATEs the full table because "OGC-761 has NOT
  landed on develop." OGC-285 owns the label-preset data model
  (`label_preset`, `label_preset_field`, `test_label_config`,
  `test_label_preset_link`); OGC-761 builds only the editor Labels tab on top of
  it. No parallel model. FR-D08 updated. M0 records this boundary.
- Q: What qualifies as a "valid part" to port from `demo-silnas` (vs. waiting for
  the whole branch to merge back)? → A: **Only test-catalog-scoped,
  develop-clean commits** — a change that (1) belongs to an OGC-949 epic, (2)
  applies cleanly onto develop without dragging
  vector/environmental/compliance/NCE dependencies, and (3) carries its own
  tests. PR #3706 (Methods) is the first and currently only qualifying port.
  Compliance/S-01 (OGC-528) does **not** qualify for porting here — it reaches
  develop via its own PR #3500, and v2 Compliance (OGC-765) waits on that.

## User Scenarios & Testing _(mandatory)_

User stories are scoped **one per v1 epic** (12 stories). Each describes the
epic's definition-of-done; the detailed, per-field acceptance criteria are
authoritative in the linked Jira child stories. M0 (reconciliation) and M-DC
(legacy decommission) are engineering milestones and carry no user story —
their requirements appear as Functional Requirements and in
[plan.md](./plan.md) / [tasks.md](./tasks.md).

### User Story 1 — Schema Foundation for the Unified Catalog (Priority: P1)

As the system, before any editor UI exists, the database and backend services
must carry every new concept the editor configures — test domain, AMR/WHONET
config, multi-component results, units master list, sample handling, junctions,
display order, and the activation-acknowledgment audit — and every existing test
must migrate cleanly into the new shape.

**Why this priority**: P1 foundation. Every section epic reads from this schema;
nothing downstream can start until it lands. It is also the riskiest single
piece (the `component_id` backfill across three referencing tables).

**Independent Test**: Run the migration against a production-like catalog dump;
verify every existing test, range, interpretation, and select-list option is
present afterward with matching row counts, each repointed to an auto-created
PRIMARY component, and legacy columns still readable. Backend service tests pass.

**Acceptance Scenarios**:

1. **Given** an existing deployment with N tests, **When** the v1 migration
   runs, **Then** all N tests have `domain = CLINICAL` backfilled and a PRIMARY
   `test_result_component` row, with `RESULT_LIMITS` / `TEST_RESULT` rows
   (FRS aliases: `test_range` / `test_select_list_option`) repointed to it and
   counts unchanged.
2. **Given** the migration has run, **When** a downstream consumer reads a
   deprecated per-test column (`result_type`, `unit_of_measure`,
   `significant_digits`, `default_result`), **Then** the value is still present
   (deprecated-not-removed for one release cycle).
3. **Given** the new schema, **When** an admin creates a test via the new
   backend service, **Then** domain is required and unit references resolve to
   the `unit_of_measure` master list (never free text).

**Detailed ACs**: authoritative in Jira [OGC-936](https://uwdigi.atlassian.net/browse/OGC-936) · [OGC-937](https://uwdigi.atlassian.net/browse/OGC-937) · [OGC-938](https://uwdigi.atlassian.net/browse/OGC-938) · [OGC-939](https://uwdigi.atlassian.net/browse/OGC-939) (OGC-940 → M-DC).

---

### User Story 2 — Editor Shell, Permissions & States (Priority: P1)

A lab administrator opens **Administration → Test Catalog Management**, clicks a
test, and lands in a SideNav-routed editor with a breadcrumb and a header
offering Save / Save as new test… / Cancel. Without the
`admin.testCatalog.manage` permission they never see the entry and the API
refuses them.

**Why this priority**: P1 foundation. Every section's UI mounts inside this
shell; permission gating and the standard empty/loading/error/no-permission
states are prerequisites for all section work.

**Independent Test**: A user with the permission can open the editor shell,
navigate the SideNav, and trigger Save-as-new-test (clone); a user without it
gets no menu entry and a 403 from the editor API.

**Acceptance Scenarios**:

1. **Given** a user with `admin.testCatalog.manage`, **When** they open the
   editor, **Then** the SideNav renders the v1 sections (v2 entries hidden) with
   working breadcrumb and header CTAs.
2. **Given** a user without the permission, **When** they request any editor
   route or API, **Then** the UI hides the entry and the API returns 403.
3. **Given** an open test, **When** the admin uses "Save as new test…", **Then**
   a modal collects Name + Code and clones the rest of the configuration into a
   new test.
4. **Given** a section with no data / a load error / no permission, **When** it
   renders, **Then** the standard empty / loading / error / no-permission state
   shows.

**Detailed ACs**: Jira [OGC-941](https://uwdigi.atlassian.net/browse/OGC-941)–[OGC-944](https://uwdigi.atlassian.net/browse/OGC-944).

---

### User Story 3 — Test List View, Filters & Pagination (Priority: P1)

The administrator sees a list of tests with click-to-open rows (no Actions
column), a collapsible filter bar (Section / Sample Type / Result Type / Status
/ Domain / AMR), URL-reflected filter + page state, pagination with page-number
jump, and row decorations including a "Coverage incomplete" tag.

**Why this priority**: P1 foundation. This is the partner-facing entry surface;
it lands alongside the shell.

**Independent Test**: Filter by Section + Domain, confirm the URL reflects the
state and is restorable on reload; paginate and jump to a page; confirm a test
with incomplete range coverage shows the Coverage-incomplete tag.

**Acceptance Scenarios**:

1. **Given** the list, **When** the admin applies filters, **Then** results
   narrow and the URL encodes the filter + page state (restorable on reload).
2. **Given** a long catalog, **When** the admin paginates or jumps to a page
   number, **Then** the correct page renders.
3. **Given** a test with range gaps, **When** it appears in the list, **Then**
   it carries a "Coverage incomplete" tag.
4. **Given** any row, **When** the admin clicks it (or uses keyboard nav),
   **Then** the editor opens for that test.

**Detailed ACs**: Jira [OGC-945](https://uwdigi.atlassian.net/browse/OGC-945)–[OGC-948](https://uwdigi.atlassian.net/browse/OGC-948).

---

### User Story 4 — Basic Info Section (Priority: P2)

The administrator edits a test's identity (Name / Reporting Name / Code /
Description), sets its **Domain** (with a switch-confirmation modal), toggles the
**AMR** flag (revealing conditional WHONET fields), and sets status flags
(Active / Orderable / Internal QA), where activation is gated by range coverage.

**Why this priority**: P2. Domain + AMR are real partner demand (Madagascar);
the activation gate couples to Ranges (US7), so this story stubs the gate and
wires it when US7 lands.

**Independent Test**: Set a test's domain and AMR flag with WHONET fields, save,
reopen — values persist; attempt to activate a test with incomplete coverage and
confirm the gate engages (stub or wired).

**Acceptance Scenarios**:

1. **Given** a test, **When** the admin changes Domain, **Then** a confirmation
   modal explains the impact before applying.
2. **Given** the AMR flag is on, **When** the admin views Basic Info, **Then**
   WHONET fields are revealed; turning it off retains the config for one-click
   re-enable.
3. **Given** an attempt to set Active on a test with incomplete range coverage,
   **When** saved, **Then** activation is gated pending acknowledgment (per US7).

**Detailed ACs**: Jira [OGC-950](https://uwdigi.atlassian.net/browse/OGC-950)–[OGC-953](https://uwdigi.atlassian.net/browse/OGC-953).

---

### User Story 5 — Sample & Results Section (Priority: P2)

The administrator configures a test's sample types and its **result
components** — one or many. Multi-component tests render each component as an
expandable accordion holding its own select-list options and result
interpretations; units are added inline from a master list; an admin can copy
configuration from another test.

**Why this priority**: P2, and the heaviest section. Result Components are the
core multi-component capability and the source of the riskiest migration; this
story **unblocks Ranges (US7)** via `component_id`. Best handled as a pair with
US7 by the same owner.

**Independent Test**: Create a two-component test (e.g. blood pressure), give
each component its own unit, select-list options, and interpretations, save and
reopen — both components and their nested config persist.

**Acceptance Scenarios**:

1. **Given** a single-component test, **When** edited, **Then** the section
   renders flat; **Given** 2+ components, **Then** each renders as an accordion
   with nested options + interpretations.
2. **Given** a component needing a new unit, **When** the admin adds it inline,
   **Then** it is written to the `unit_of_measure` master list and selectable.
3. **Given** a reorder of components / options / interpretations, **When** done
   via drag-drop or keyboard arrows, **Then** order persists.
4. **Given** another test with similar config, **When** the admin uses "Copy
   from Test", **Then** the chosen config is copied into the current component.

**Detailed ACs**: Jira [OGC-961](https://uwdigi.atlassian.net/browse/OGC-961)–[OGC-968](https://uwdigi.atlassian.net/browse/OGC-968).

---

### User Story 6 — Methods Section (Priority: P3)

The administrator links existing methods to a test, creates a new method inline
(written to Master Lists and linked), sets a default method with an effective
date, and can copy methods from another test.

**Why this priority**: P3. Already implemented on `demo-silnas` (PR #3706);
this story is **port + verification** onto develop, not reimplementation.

**Independent Test**: Link a method, create one inline, mark a default with an
effective date, save and reopen — links and default persist; the ported code
passes its tests on develop.

**Acceptance Scenarios**:

1. **Given** a test, **When** the admin links a method, **Then** it appears in
   the linked-methods table with a shortcode.
2. **Given** no suitable method exists, **When** the admin creates one inline,
   **Then** it is created in Master Lists and linked in one action.
3. **Given** multiple linked methods, **When** the admin marks one default with
   an effective date, **Then** the default persists.

**Detailed ACs**: Jira [OGC-954](https://uwdigi.atlassian.net/browse/OGC-954)–[OGC-956](https://uwdigi.atlassian.net/browse/OGC-956).

---

### User Story 7 — Ranges + Coverage Validation (Priority: P2)

The administrator defines reference ranges per range type, grouped by sex, with
**hour-level** age granularity (neonatal). A **Coverage Validation** panel shows
Male/Female coverage with gap/overlap detection and fill/copy actions. Activating
a test with gaps requires an **Activation Acknowledgment** modal, logged to an
audit table.

**Why this priority**: P2 and clinically safety-relevant. Sequenced after US5
(needs `component_id`); pairs with US5.

**Independent Test**: Author neonatal bilirubin ranges across hour windows;
confirm Coverage Validation flips to green only when all windows are covered;
attempt to activate with a gap and confirm the acknowledgment modal blocks until
acknowledged and writes an audit row.

**Acceptance Scenarios**:

1. **Given** ranges with mixed age units, **When** Coverage Validation runs,
   **Then** units normalize to a common basis and gaps/overlaps are detected
   correctly at hour boundaries.
2. **Given** an "All"-sex range, **When** coverage is shown, **Then** both Male
   and Female cards indicate coverage via the "All" range.
3. **Given** incomplete coverage, **When** the admin saves, **Then** it warns
   but never blocks save; **When** the admin Activates, **Then** the Activation
   Acknowledgment modal blocks until explicit acknowledgment, which is logged.
4. **Given** a gap, **When** the admin uses Fill Gap or Copy-to-other-sex,
   **Then** the missing window is filled.

**Detailed ACs**: Jira [OGC-969](https://uwdigi.atlassian.net/browse/OGC-969)–[OGC-976](https://uwdigi.atlassian.net/browse/OGC-976).

---

### User Story 8 — Sample Storage Section (Priority: P3)

The administrator records storage conditions, max duration, stability notes,
special handling, disposal method/timeframe, special instructions, and an
Override-Restricted toggle governing in-progress orders.

**Why this priority**: P3. Independent section; parallelizable after foundation.

**Independent Test**: Set storage conditions and the Override-Restricted toggle,
save and reopen — values persist; confirm an in-progress order keeps its existing
storage settings while new orders use the locked version.

**Acceptance Scenarios**:

1. **Given** a test, **When** the admin sets storage conditions + disposal,
   **Then** they persist on the `test_sample_handling` record.
2. **Given** Override Restricted is toggled, **When** an order is in progress,
   **Then** it keeps existing settings; new orders use the locked version.

**Detailed ACs**: Jira [OGC-977](https://uwdigi.atlassian.net/browse/OGC-977)–[OGC-979](https://uwdigi.atlassian.net/browse/OGC-979).

---

### User Story 9 — Panels Section (Priority: P3)

The administrator adds the test to panels via a typeahead picker, creates a new
panel via a separate button + inline form, and edits the test's position within
each panel via drag-drop, numeric input, or keyboard arrows.

**Why this priority**: P3. Independent section; reuses existing drag-drop infra.

**Independent Test**: Add the test to a panel via typeahead, reposition it, save
and reopen — membership and position persist.

**Acceptance Scenarios**:

1. **Given** the section, **When** the admin picks a panel via typeahead,
   **Then** the test is linked and the panel row is expandable.
2. **Given** a linked panel, **When** the admin reorders position via drag /
   numeric / keyboard, **Then** the position persists.
3. **Given** no suitable panel, **When** the admin creates one inline, **Then**
   a post-creation notification points to Master Lists for further settings.

**Detailed ACs**: Jira [OGC-980](https://uwdigi.atlassian.net/browse/OGC-980)–[OGC-982](https://uwdigi.atlassian.net/browse/OGC-982).

---

### User Story 10 — Terminology Mappings Section (Priority: P3)

The administrator views and adds terminology mappings (Source / Code /
Relationship) for the test.

**Why this priority**: P3. Small, independent section.

**Independent Test**: Add a mapping, save and reopen — it persists in the
mappings table.

**Acceptance Scenarios**:

1. **Given** the section, **When** the admin adds a mapping (Source + Code +
   Relationship), **Then** it appears in the mappings table and persists.

**Detailed ACs**: Jira [OGC-957](https://uwdigi.atlassian.net/browse/OGC-957)/[OGC-958](https://uwdigi.atlassian.net/browse/OGC-958).

---

### User Story 11 — Analyzers Section (read-only) (Priority: P3)

The administrator sees a **read-only** table of which analyzers can run this
test, derived from analyzer test-code mappings, with an info card and empty
state.

**Why this priority**: P3. Read-only; source of truth is the analyzer record,
edited elsewhere.

**Independent Test**: Open a test with analyzer mappings and confirm the
read-only table lists them; open one without and confirm the empty state.

**Acceptance Scenarios**:

1. **Given** a test with analyzer mappings, **When** the section renders,
   **Then** the analyzers appear read-only with no link/unlink controls.
2. **Given** a test with no mappings, **When** the section renders, **Then** the
   empty state + info card show.

**Detailed ACs**: Jira [OGC-959](https://uwdigi.atlassian.net/browse/OGC-959)/[OGC-960](https://uwdigi.atlassian.net/browse/OGC-960).

---

### User Story 12 — Display Order Section (Priority: P3)

The administrator picks a sample type and reorders the tests within it via
drag-drop or keyboard arrows, with auto-save on drop persisting to
`SAMPLETYPE_TEST.display_order` (FRS alias: `test_sample_type.display_order`).

**Why this priority**: P3. Independent section; pulled into v1 because the
drag-drop infrastructure already exists.

**Independent Test**: Reorder tests within a sample type, confirm auto-save on
drop, reload and confirm the order persisted.

**Acceptance Scenarios**:

1. **Given** a sample type, **When** the admin selects it, **Then** its tests
   render in current display order.
2. **Given** a reorder via drag or keyboard, **When** dropped, **Then** the new
   order auto-saves to `SAMPLETYPE_TEST.display_order`.

**Detailed ACs**: Jira [OGC-983](https://uwdigi.atlassian.net/browse/OGC-983)–[OGC-985](https://uwdigi.atlassian.net/browse/OGC-985).

---

### Edge Cases

- **Multi-value free-text legacy tests** (e.g. "120/80" in one field): migration
  maps them to a single PRIMARY component; no automated sweep report ships in v1
  (declined per critique H-05) — manual admin review is recommended
  post-migration.
- **"All"-sex ranges + Coverage Validation**: must indicate coverage in both
  Male and Female cards (fix M-08), not appear as two gaps.
- **AMR flag turned off then on**: WHONET config persists across the toggle;
  existing results keep their result-time export status, new results respect the
  current flag (fix H-04).
- **Domain switch on an existing test**: confirmation modal; in v1 the modal
  omits the "section visibility may change" line because Compliance is hidden for
  all domains until v2 (fix M-04).
- **Activation with no infants seen by the lab** (e.g. senior center): admin may
  acknowledge gaps and proceed consciously — coverage warns, acknowledgment
  unblocks (fix H-03).
- **A user loses `admin.testCatalog.manage` mid-session**: API returns 403 and
  the UI degrades to the no-permission state.

## Requirements _(mandatory)_

### Functional Requirements

Program-level requirements only. Per-section field-level behavior is in the Jira
child stories (linked in the Source Map). The "D-" requirements encode the 11
v2.5 health-check fixes that are cross-cutting deltas not owned by a single epic.

**Replacement & lifecycle**

- **FR-001**: The unified editor MUST fully replace the legacy Test, Test
  Section, Panel, and Method admin pages; at v1 release those pages MUST be
  removed and their routes redirected (Constitution Principle X — legacy
  controllers and React components deleted, not left parallel).
- **FR-002**: v1 MUST ship with **no feature flag**; rollback is a release
  revert. Deprecated per-test columns (`result_type`, `unit_of_measure`,
  `significant_digits`, `default_result`) MUST be retained for one release cycle
  as the data-level safety net and MUST remain readable by existing consumers.
- **FR-003**: Migration of an existing catalog MUST be lossless: every test,
  range, interpretation, and select-list option present before the migration
  MUST be present after, with matching counts and repointed to an auto-created
  PRIMARY result component.

**Access control**

- **FR-004**: The editor surface MUST be gated on the existing `ADMIN` role —
  the UI MUST hide the entry and the API MUST return HTTP 403 for non-admins —
  using `@PreAuthorize("hasRole('ADMIN')")` on the REST layer, matching existing
  OE admin pages. `admin.testCatalog.manage` is the logical name for this gate,
  not a separate grant in v1. (Migration to the fine-grained privilege-based RBAC
  in OGC-384 / PR #3443 is a future follow-up, **not** a v1 dependency — v1 must
  not block on that unmerged work.)

**Editor behavior**

- **FR-005**: The editor MUST be a single SideNav-routed surface with breadcrumb
  and a header offering Save, "Save as new test…" (clone), and Cancel.
- **FR-006**: Every section MUST present standard empty / loading / error /
  no-permission states.
- **FR-007**: Section visibility MUST be domain-conditional: CLINICAL tests hide
  Compliance; ENVIRONMENTAL and VECTOR tests show all sections (Compliance is a
  v2 section, hidden entirely in v1).
- **FR-008**: The Test List View MUST reflect filter + pagination state in the
  URL (restorable on reload) and MUST decorate tests with incomplete range
  coverage.
- **FR-009**: Every reorder surface MUST offer a keyboard-accessible alternative
  (Arrow Up/Down) in addition to drag-drop (WCAG; fix M-02).
- **FR-010**: Result components MUST support multi-component tests, rendering a
  per-component accordion when 2+ components exist and a flat layout for one
  (fix M-03).
- **FR-011**: Units MUST be selected from the `unit_of_measure` master list
  (inline-add writes to the master list); free-text units MUST NOT be accepted.

**Patient safety — ranges & activation**

- **FR-012**: Coverage Validation MUST warn on save but never block save, and
  MUST block _Activate_ until gaps are acknowledged via the Activation
  Acknowledgment modal, with each acknowledgment written to
  `test_activation_acknowledgment` (fix H-03).
- **FR-013**: Coverage Validation MUST normalize all age units (including hours)
  to a common basis and detect gaps/overlaps correctly at hour boundaries
  (neonatal bilirubin).

**Health-check fixes (v2.5 §1, D-01..D-11)** — each MUST be honored:

- **FR-D01**: Remove the five stale `editor.sidenav.*` group-header i18n keys
  (the editor uses a flat SideNav).
- **FR-D02**: FHIR mapping is locked to **one `Observation` per result
  component**; `Observation.component[]` MUST NOT be used.
- **FR-D03**: Alert-rule **authoring** lives in the Test Catalog; **delivery +
  templates** live in the shipped Test Notification system. No template/channel
  template columns on the alert-rule table. (v2 surface; the invariant is
  recorded now.)
- **FR-D04**: A fifth `COMPLIANCE_BREACH` alert trigger exists for
  ENVIRONMENTAL/VECTOR tests only; CLINICAL tests do not see it. (v2 surface.)
- **FR-D05**: Alert rules carry an `acknowledgment_required` flag (no-ops until
  the global Critical Result Acknowledgment workflow ships). (v2 surface.)
- **FR-D06**: The Alerts section cross-references the (separate) Critical Result
  Acknowledgment workflow; the per-rule toggle is the hook. (v2 surface.)
- **FR-D07**: The test↔reagent linkage does not exist today; building it
  (`test_reagent_link` table + API) is part of v2 scope and gates the Reagents
  section.
- **FR-D08**: The v2 Labels section (OGC-761) **consumes the OGC-285 label-preset
  data model** (`label_preset`, `label_preset_field`, `test_label_config`,
  `test_label_preset_link` from PR #3676) — it builds only the editor Labels tab,
  not a parallel model. Full configurable Label Preset Management remains OGC-285's
  feature.
- **FR-D09**: The `test_sample_handling_history` table is created in v1 but
  inert (write triggers light up in v2).
- **FR-D10**: No `useTestCatalogV2` feature flag exists (v1 is a direct
  replacement; see FR-002).
- **FR-D11**: The "Internal QA — No Results Release" flag suppresses results
  from patient reports for internal validation tests and is explicitly **not**
  the EQA participant-workflow surface.

### Constitution Compliance Requirements (OpenELIS Global)

_From `.specify/memory/constitution.md` — principles relevant to this feature:_

- **CR-001**: All editor UI MUST use Carbon Design System (`@carbon/react`) —
  DataTable, ComboBox, FilterableMultiSelect, Accordion, Modal, Pagination,
  SideNav, Tag, InlineNotification. NO custom CSS frameworks (mockup CSS is
  visual approximation only).
- **CR-002**: All UI strings MUST be internationalized via React Intl; new keys
  added to `en.json` only (Transifex owns non-English). The v2.5 i18n staging
  (§0.8) governs which keys land in which wave.
- **CR-003**: Backend MUST follow the 5-layer architecture
  (Valueholder → DAO → Service → Controller → Form); new valueholders MUST use
  JPA/Hibernate annotations; `@Transactional` in services only.
- **CR-004**: All schema changes MUST be Liquibase changesets under
  `src/main/resources/liquibase/3.5.x.x/` (NO direct DDL/DML).
- **CR-005**: FHIR R4 mapping for result components is normative and locked to
  one `Observation` per component (FR-D02).
- **CR-006**: Domain/country variation (Madagascar AMR/WHONET, Indonesia/SILNAS
  environmental & vector) MUST be configuration-driven, not code-branched.
- **CR-007**: RBAC via `admin.testCatalog.manage`; audit trail on
  activation-acknowledgment and (v2) sample-handling history; input validation
  on all forms.
- **CR-008**: TDD — JUnit 4 backend, Jest/RTL frontend, Playwright E2E (Cypress
  deprecated). Tests precede implementation per milestone.

### Key Entities

Full FRS→repository translation in [data-model.md](./data-model.md) (the FRS
uses idealized names; real tables in **bold**, FRS aliases in parentheses —
see research.md §R9). v1 objects:

- **TEST.DOMAIN** — CLINICAL/ENVIRONMENTAL/VECTOR enum on every test (shipped, changeset 040).
- **TEST.ANTIMICROBIAL_RESISTANCE** — the AMR flag (EXISTING column, reused; the WHONET-field reveal toggles this — R11).
- **test_amr_config** — per-test WHONET antibiotic code/class/method/breakpoint (shipped, 040).
- **whonet_antibiotic_codes** — seeded reference list for AMR typeahead (shipped, 040).
- **test_result_component** — NEW; one row per labeled value field in a test
  (multi-component support); the migration auto-creates a PRIMARY per test (shipped, 041).
- **component_id FKs** — added to **RESULT_LIMITS** (FRS: `test_range`) and
  **TEST_RESULT** (FRS: `test_select_list_option`), backfilled to PRIMARY.
- **test_result_interpretation** — NEW (FRS: `test_interpretation`; no legacy
  counterpart — today only `TEST_RESULT.is_normal`).
- **UNIT_OF_MEASURE** — EXISTS; hardened into the master list (add
  `code`/`ucum_code`/`is_active`), not recreated (FR-011).
- **LOCALIZATION / LOCALIZATION_VALUE** — EXISTS; already localizes test names
  via `TEST.name_localization_id` (FRS's `test_localization` is REUSED, not
  created; localized description deferred to v2/M20).
- **test_sample_handling** — NEW; storage conditions/disposal/override per test.
- **test_sample_handling_history** — NEW; created v1, inert until v2 (D-09).
- **PANEL_ITEM** — EXISTS with `SORT_ORDER`; the FRS's `panel_test` junction is
  REUSED, not created (M9).
- **SAMPLETYPE_TEST.display_order** — NEW COLUMN on the existing sample-type↔test
  junction (FRS: `test_sample_type.display_order`) (M12).
- **test_terminology_mapping** — NEW; multi-source (LOINC/SNOMED/CIEL/OCL)
  mappings; `TEST.LOINC` deprecate-in-place with backfill (M10).
- **test_activation_acknowledgment** — NEW; audit log of coverage acknowledgments.
- **Multi-section junction** (FRS: `test_section_assignment`) — DEFERRED
  decision at M2; today TEST is 1:1 section via `TEST.TEST_SECTION_ID`.
- **Deprecated-in-place**: `TEST.UOM_ID` and per-`TEST_RESULT`
  type/significant-digits remain readable one release cycle as the data-level
  rollback (FR-002).

## Deferred Scope — v2 Wave

Carried for dependency reasoning; **named but not task-elaborated** in this
feature (no FR numbers). Elaborated via a `/speckit.plan` revision at v2 kickoff.

- **M13 — [OGC-760](https://uwdigi.atlassian.net/browse/OGC-760)**: Test-Reagent linkage backend (must land first within v2; FR-D07).
- **M14 — [OGC-761](https://uwdigi.atlassian.net/browse/OGC-761)**: Labels section (4 fixed presets; consumes OGC-285 output per M0 boundary; FR-D08).
- **M15 — [OGC-762](https://uwdigi.atlassian.net/browse/OGC-762)**: Reagents section (blocked by M13).
- **M16 — [OGC-763](https://uwdigi.atlassian.net/browse/OGC-763)**: Alerts section (authoring here, delivery via Notification system; FR-D03..D06).
- **M17 — [OGC-764](https://uwdigi.atlassian.net/browse/OGC-764)**: Reflex & Calc section (read-only cross-links).
- **M18 — [OGC-765](https://uwdigi.atlassian.net/browse/OGC-765)**: Compliance section (ENV/VECTOR only; blocked by [OGC-528](https://uwdigi.atlassian.net/browse/OGC-528) reaching develop).
- **M19 — [OGC-766](https://uwdigi.atlassian.net/browse/OGC-766)**: Sample Storage audit history (activates triggers on the v1 table; FR-D09).
- **M20 — [OGC-767](https://uwdigi.atlassian.net/browse/OGC-767)**: Localization Hardening (`get_localized_test_field()` + UI fallback indicators + bulk CSV).

## Assumptions & Constraints

- **Platform**: Java 21 LTS, Jakarta EE 9, Spring Framework 6.2 (Traditional
  MVC, not Spring Boot), React 17, PostgreSQL via Liquibase 4.8.
- **Reusable infrastructure exists** (see [research.md](./research.md)):
  `ResultLimit` already supports age/sex/critical ranges; analyzer test-code
  mappings exist for the read-only Analyzers section; reflex and calculated-value
  subsystems exist for v2 Reflex & Calc; `ViewTestCatalog.jsx` +
  `TestCatalogRestController` are the read-only ancestor; `DisplayListController`
  is the dropdown-list pattern.
- **Test Notification system** must be shipped and live before v2 Alerts
  (OGC-763) — verified pre-v2-kickoff.
- **OGC-528 / S-01 Compliance admin** must reach develop before v2 Compliance
  (OGC-765); it currently exists on `demo-silnas` only.
- **Carbon component availability** (DataTable, ComboBox, FilterableMultiSelect,
  Accordion, Modal, Pagination, SideNav, Tag) in the current `@carbon/react`.

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: A lab administrator can configure a complete, orderable test
  end-to-end — identity, sample types, result components, methods, ranges,
  storage, panels — entirely within the new editor, without opening any legacy
  admin page.
- **SC-002**: At v1 release, the legacy Test, Test Section, Panel, and Method
  admin pages are absent from the application (their menu entries and routes no
  longer resolve to the old surfaces).
- **SC-003**: After the v1 migration on a production-like catalog, 100% of
  pre-existing tests, ranges, interpretations, and select-list options are
  present, with row counts matching pre-migration and each repointed to a PRIMARY
  result component.
- **SC-004**: All 49 v1 Jira child-story acceptance criteria are demonstrably met
  (each story closed against its ACs).
- **SC-005**: A user without `admin.testCatalog.manage` can neither see the
  editor entry nor reach its API (403 on every editor endpoint).
- **SC-006**: Activating a test with incomplete range coverage is impossible
  without an explicit, audited acknowledgment; a neonatal bilirubin test with all
  hour windows covered shows green Coverage Validation.
