# Feature Specification: Vector Surveillance Reporting (OGC-585 / V-04)

**Feature Branch**: `372-vector-surveillance-reporting`
**Created**: 2026-06-15
**Status**: Draft
**Target branch**: `demo-silnas` (SILNAS Indonesia demo)
**Roadmap**: [`specs/roadmaps/vector-surveillance-reporting-roadmap.md`](../roadmaps/vector-surveillance-reporting-roadmap.md) — authoritative scoping context; supersedes the drifted external FRS (repo v1.4 vs Jira v1.5).
**Input**: User description: "Vector Surveillance Reporting (OGC-585 / V-04): internal dashboard of vector surveillance indices (collection density, species distribution, MIR, pathogen positivity, QC pass-rate) with date-range and sampling-site filters, PDF export, and a SILANTOR Manual Entry Helper, computed from OpenELIS's own data. FHIR/Superset pipeline parked; reporting read-model deferred to planning."

> **Scope note (why this is "internal"):** This feature turns recorded vector lab data into surveillance indices and presents them **inside OpenELIS, computed from OpenELIS's own data**. The FHIR push / Apache Superset / OHS-ETL pipeline described in earlier design revisions is **parked** — it has no current external consumer and nothing of it is built. See *Assumptions & Constraints* and the roadmap.

## Clarifications

### Session 2026-06-15

Resolved **from the V-04 v1.5 FRS** (the Jira OGC-585 comment trail that defines the Manual Entry Helper) — the source of truth. No open user decisions remained after grounding in it.

- Q: Manual Entry submission grain — aggregate lab-level vs per-site? → A: **Lab-level, weekly**, organized by **Aedes/Anopheles species tabs** (v1.5). "Sites with positives" is itself a lab-level aggregate metric. (Audit `siteId` is retained nullable to allow a future per-site option without schema change.)
- Q: Sporozoite-rate — gating only, or the actual value? → A (confirmed by product owner): the **actual sporozoite rate IS computed** — an Anopheles MIR-style figure from the CSP-ELISA assay (identified by LOINC `71712-2`). A low deconvolution resolution (`positive_resolution_pct < 95%`) is surfaced as a **confidence caveat beside the value**, not a hard withhold.
- Q: Reporting period granularity? → A: **Weekly** ("Mark week submitted" — v1.5).
- Q: Submission audit shape? → A: One **immutable** row per "mark week submitted", capturing a **`snapshot_json`** of the visible figures (v1.5 `vector_manual_entry_audit`).
- Deferred (not blocking): the **default seed metric list** is held at FR-shape pending the program expert (Ida). It is configuration (FR-009), adjustable without code.

## User Scenarios & Testing _(mandatory)_

### User Story 1 - View the vector surveillance dashboard (Priority: P1)

A surveillance coordinator opens **Reports → Vector Surveillance** and sees current surveillance indices — collection density over time, species distribution, Minimum Infection Rate (MIR), pathogen positivity, and QC pass-rate — computed from the vector collections, species identifications, and pathogen results already recorded in OpenELIS. No manual spreadsheet work.

**Why this priority**: This is the core value — it replaces manual Excel aggregation and is a viable MVP on its own. Everything else refines or routes this data.

**Independent Test**: Record a set of vector collections, pool results, and species IDs, open the dashboard, and confirm the indices reflect that data correctly (including MIR and resolution indicators).

**Acceptance Scenarios**:

1. **Given** recorded vector collections and pathogen results for a period, **When** the coordinator opens the Vector Surveillance dashboard, **Then** collection density, species distribution, MIR, pathogen positivity, and QC pass-rate are displayed for that data.
2. **Given** positive pools that are only partially deconvoluted to individuals, **When** MIR is displayed, **Then** both the classical MIR and the deconvolution-aware infection rate are shown alongside a positive-resolution percentage indicating how much of the figure is ground-truth versus fallback.
3. **Given** a period containing QC samples (controls/blanks/duplicates), **When** any surveillance index is computed, **Then** the QC samples are excluded from the numerators and denominators.
4. **Given** the dashboard is displayed, **When** the coordinator reads it, **Then** an indication of how current the figures are (data freshness) is visible.

---

### User Story 2 - Filter by date range and sampling site (Priority: P2)

The coordinator narrows the dashboard to a specific time window and/or sampling site so they can focus on a particular area or period.

**Why this priority**: High value but the dashboard delivers value at the default (all-data) scope without it; filters are a refinement.

**Independent Test**: Apply a date range and a site filter and confirm every index recomputes for exactly that scope.

**Acceptance Scenarios**:

1. **Given** the dashboard, **When** the coordinator selects a date range and applies it, **Then** all indices recompute for that window.
2. **Given** the dashboard, **When** the coordinator selects a sampling site and applies it, **Then** all indices recompute for that site only.
3. **Given** a scope with no recorded data, **When** filters are applied, **Then** a clear empty state is shown (not an error).

---

### User Story 3 - Export the dashboard to PDF (Priority: P2)

The coordinator exports the current dashboard view as a PDF to share with stakeholders who have no system access.

**Why this priority**: Important for stakeholder reporting, but secondary to seeing and filtering the data.

**Independent Test**: Open the dashboard for a scope, export, and confirm a PDF is produced that reflects the on-screen figures.

**Acceptance Scenarios**:

1. **Given** a dashboard view (any filter scope), **When** the coordinator exports to PDF, **Then** a downloadable PDF reflecting the current figures and filters is produced.
2. **Given** the coordinator lacks export permission, **When** they attempt to export, **Then** the action is unavailable/blocked.

---

### User Story 4 - Submit weekly numbers via the Manual Entry Helper (Priority: P2)

Because the national vector portal (SILANTOR) is filled in **manually** by lab/program staff, the coordinator opens a Manual Entry Helper (organized by **Aedes/Anopheles species tabs**, per v1.5) that shows the **week's lab-level** numbers in the order the portal asks for them, copies each value into the portal browser tab, and marks the week submitted.

**Why this priority**: Distinct, independently valuable regulatory workflow; depends on the indices existing (US1) but is otherwise standalone.

**Independent Test**: Open the helper for a period, copy values, mark the week submitted, and confirm a submission audit record is created capturing the values, period, user, and time.

**Acceptance Scenarios**:

1. **Given** a reporting period with computed indices, **When** the coordinator opens the Manual Entry Helper, **Then** the period's metrics are displayed in a defined order, each with an individual copy action.
2. **Given** the helper is open, **When** the coordinator marks the week submitted, **Then** an audit record is created capturing a snapshot of the submitted values, the period, the user, and the timestamp.
3. **Given** the period's deconvolution resolution is below 95% (`positive_resolution_pct < 95%`), **When** the sporozoite-rate figure is shown, **Then** the value is displayed with a low-resolution confidence caveat (consistent with the dashboard), not withheld.
4. **Given** a week already marked submitted, **When** the coordinator marks it again, **Then** the system records the re-submission distinctly (no silent overwrite of history).

---

### User Story 5 - Configure the Manual Entry field map (Priority: P3)

A lab administrator reorders, hides, relabels, or portal-tags the metrics shown in the Manual Entry Helper so each deployment matches its portal's field list — without code changes.

**Why this priority**: Deployment-configuration convenience; the helper works with a sensible default field map without it.

**Independent Test**: Change the field map (reorder + hide one metric + relabel another) and confirm the Manual Entry Helper reflects the change with no redeploy.

**Acceptance Scenarios**:

1. **Given** the admin field-map screen, **When** the admin reorders/hides/relabels metrics and saves, **Then** the Manual Entry Helper reflects the new configuration.
2. **Given** the admin lacks the field-map management permission, **When** they attempt to open the screen, **Then** access is blocked.

---

### Edge Cases

- **No data in scope** → clear empty state, not an error (FR-012).
- **Partially deconvoluted pools** → both MIR metrics shown with resolution % < 100 (US1-2).
- **Low deconvolution resolution** (`positive_resolution_pct < 95%`) → sporozoite rate shown with a confidence caveat (US4-3).
- **Period with only QC samples** → excluded from indices → zero/empty with clear messaging (FR-002).
- **Site with collections but no positive results** (significance configured) → density shown, positivity = 0, MIR = 0.
- **Catalog positivity classification absent** (no `test_result.significance` on any in-scope result) → positivity-dependent panels (MIR, positivity, sporozoite) show a "not configured" state, never fabricated zeros (FR-015).
- **Re-submission of an already-submitted week** → recorded as a distinct audit event (US4-4).
- **Very large date range** → figures still render within the freshness/perf targets (SC-002, SC-008).

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: System MUST compute vector surveillance indices from recorded vector collection, species-identification, and pathogen-result data: collection density, species distribution, Minimum Infection Rate (MIR), pathogen positivity rate, and QC pass-rate.
- **FR-002**: System MUST exclude QC samples (controls, blanks, duplicates) from surveillance index numerators and denominators as a structural rule, with no user-facing toggle to include them.
- **FR-003**: System MUST compute MIR **per pathogen × species** — classical MIR as (positive pools ÷ total specimens) × 1000, AND a deconvolution-aware infection rate that uses exact descendant positive counts where pools are fully deconvoluted and falls back to the classical count otherwise, AND a positive-resolution percentage indicating how much of the figure is ground-truth versus fallback. A pool counts positive for a pathogen when its recorded result is **classified POSITIVE via the test catalog** (`test_result.significance` = POSITIVE); species identifications are restricted to `confidence = CONFIRMED`.
- **FR-004**: System MUST present the indices as a dashboard within the Reports area, visible only to users holding the view permission.
- **FR-005**: Users MUST be able to filter the dashboard by date range and by sampling site, with every index recomputed for the selected scope.
- **FR-006**: Users MUST be able to export the current dashboard view (respecting active filters) as a PDF document.
- **FR-007**: System MUST provide a Manual Entry Helper that displays the current reporting period's surveillance numbers in a defined order, provides an individual copy action per value, and lets the user mark the period as submitted.
- **FR-008**: System MUST record an audit entry on each "mark submitted" action capturing a snapshot of the submitted values, the reporting period, the acting user, and the timestamp; re-submissions MUST be recorded as distinct events.
- **FR-009**: System MUST allow an administrator to configure the Manual Entry Helper field map (order, visibility, label, portal tag) without code changes, with a sensible default field map shipped.
- **FR-010**: System MUST gate dashboard view, PDF export, Manual Entry submission, and field-map administration behind distinct permissions, enforced at the API layer.
- **FR-011**: Surveillance figures MUST be computed from OpenELIS's own recorded data; rendering the dashboard MUST NOT depend on any external system or service.
- **FR-012**: When no data exists for the selected scope, the system MUST present a clear empty state rather than an error.
- **FR-013**: System MUST indicate how current the displayed figures are (data freshness) so users know the figures' recency.
- **FR-014**: All user-facing figures and labels MUST be presented through the internationalization layer (no hardcoded display strings).
- **FR-015**: Positivity MUST be determined from the test catalog's result-significance classification (`test_result.significance`), not from a hardcoded result value or the deconvolution workflow status. When no in-scope result carries a significance classification, the positivity-dependent panels (MIR, pathogen positivity, sporozoite) MUST present a clear "not configured" state rather than fabricated zeros — so the feature degrades safely on deployments without the classification configured.

### Constitution Compliance Requirements (OpenELIS Global)

_Derived from `.specify/memory/constitution.md`:_

- **CR-001**: UI MUST use Carbon Design System (`@carbon/react`) — no custom CSS frameworks.
- **CR-002**: All UI strings MUST be internationalized via React Intl (new keys in `en.json` only; translations via Transifex). No hardcoded text.
- **CR-003**: Backend MUST follow the 5-layer architecture (Valueholder → DAO → Service → Controller → Form); `@Transactional` in services only; services compile all data within the transaction. New valueholders use JPA/Hibernate annotations (no XML mapping).
- **CR-004**: New persistent structures (Manual Entry field map, submission audit) MUST be created via Liquibase changesets — no direct DDL/DML.
- **CR-005**: FHIR R4 / IHE interoperability — **Not applicable to this feature.** It is internal reporting over OpenELIS data; the external FHIR publishing path is parked (separate stories OGC-586/592). No new external-facing entities are introduced here.
- **CR-006**: Country/portal-specific variation (the SILANTOR field map) MUST be configuration-driven (FR-009), not code branching.
- **CR-007**: Security — RBAC permissions (FR-010), audit trail (`sys_user_id` + `lastupdated`) on configurable/auditable entities, input validation.
- **CR-008**: Tests MUST be included (unit + integration; E2E for the primary dashboard + Manual Entry flows). JUnit 4.

### Key Entities

- **Surveillance Index (derived)**: A computed figure for a scope (date range + site) — density, species mix, MIR (classical + deconvolution-aware + resolution %), positivity, QC pass-rate. Derived from existing data; persistence (if any) is an implementation/read-model decision (see Assumptions).
- **Manual Entry Field Map**: Admin-configured, ordered set of reporting metrics with label, visibility, and portal tag. Drives what the Manual Entry Helper shows.
- **Manual Entry Submission Audit**: Immutable record of a "mark submitted" action — value snapshot, reporting period, user, timestamp.
- **Existing data consumed (read-only)**: vector pools and pool members, species identifications, samples/sample items, pathogen results, and the QC catalog (`qa_event` / `analysis_qaevent`) used to identify QC samples.

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: A coordinator can see current surveillance indices for a chosen site and period entirely within OpenELIS, with zero manual spreadsheet steps.
- **SC-002**: Displayed figures reflect newly recorded results within an acceptable freshness window (target: within 15 minutes; near-real-time acceptable), and the displayed freshness indicator matches reality.
- **SC-003**: A coordinator can produce a shareable PDF of the current dashboard view in 3 clicks or fewer.
- **SC-004**: A coordinator can transcribe a reporting period's numbers into the national portal using the helper, and 100% of "mark submitted" actions produce a retrievable audit record with a correct value snapshot.
- **SC-005**: QC samples never affect a surveillance figure — adding a QC sample to a period leaves MIR, positivity, and density numerators/denominators unchanged (100% exclusion).
- **SC-006**: For every species/period with pathogen results, MIR and a positive-resolution percentage are shown, and the resolution percentage correctly reflects the proportion of positive pools fully deconvoluted.
- **SC-007**: Each of dashboard view, export, submission, and field-map administration blocks an unauthorized user (verified by an access-denied result for a user lacking the respective permission).
- **SC-008**: The dashboard renders a typical site/period scope without perceptible delay under expected demo data volumes.

## Assumptions & Constraints

**Scope decisions (from the roadmap; treat as settled):**

- **Internal computation.** Figures come from OpenELIS's own recorded data (FR-011). The FHIR push, Apache Superset embedding, and OHS SQL-on-FHIR ETL from earlier design revisions are **parked** — no current external consumer, and nothing of it is built. They are out of scope here and tracked as separate stories.
- **Read-model is deferred to planning.** Whether figures are produced by direct query, scheduled materialized views, or a dedicated read store is an implementation decision for `/speckit.plan`. This spec is read-model-agnostic; it requires only that figures reflect recorded data within the freshness target (SC-002).
- **National submission = Manual Entry Helper (v1.5).** The v1.4 eWARS/SILANTOR CSV exporters are explicitly **not** built (the domain expert confirmed SILANTOR is manual entry and CSV ingestion is unused).
- **Default Manual Entry metric list** (confirmable with the program expert): pools tested, pools positive, confirmed-positive organisms, top species, MIR classical, MIR observed, sporozoite rate, sites with positives. The field map (FR-009) lets deployments adjust this without code.

**Deferred / out of scope (separate stories):**

- FHIR push of vector data + extensions; consolidated-FHIR / Superset / OHS-ETL pipeline (OGC-586, OGC-592).
- Row-level security / per-site access restriction (RLS).
- In-app threshold alerts (V-04b).
- Maximum-likelihood-estimate (MLE) infection rate (V-04c).
- Automated national-portal API submission (V-04d).

**Dependencies:**

- The vector data model (V-01 OGC-555, V-02 OGC-581, V-03 OGC-582 — taxonomy, collection/CollectionLot, identification, pool deconvolution) is present on `demo-silnas`. This feature reads the vector model (it does not modify it) but adds **one additive catalog column** — `test_result.significance` (POSITIVE/NEGATIVE/INDETERMINATE) classifying a result's positivity. It is nullable, so a deployment without it degrades to the "not configured" state (FR-015).
- **SILNAS distro** (`DIGI-UW/openelis-indonesia-distro`): supplies the `test_result.significance` values + per-pathogen vector demo data via a paired PR; the dashboard reads them as loaded catalog config.

**Platform constraints:** Java 21 LTS, Jakarta EE 9 (`jakarta.*`), Spring Framework 6.2 (Traditional MVC, not Spring Boot), Liquibase for schema, Carbon + React Intl on the frontend, milestone-based delivery (one PR per phase: analytics layer → dashboard UI → Manual Entry Helper).
