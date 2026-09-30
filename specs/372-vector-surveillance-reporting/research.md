# Research: Vector Surveillance Reporting (OGC-585 / V-04)

**Date**: 2026-06-15 | **Feature**: 372-vector-surveillance-reporting

All decisions are grounded in the existing demo-silnas codebase (4 research passes over reporting, RBAC, admin-entity, and the vector data model). The central decision is the **read model**.

## D1 — Read model: OLTP-direct, service-layer aggregation, on demand

- **Decision**: Compute surveillance indices **on demand from OpenELIS's own OLTP tables** in a `@Transactional(readOnly=true)` service that returns fully-compiled DTOs as JSON. No ETL, no materialized views, no separate read store.
- **Rationale**: This is exactly the established modern reporting pattern in the repo — `TATReportServiceImpl` (`org.openelisglobal.reports.tat.service`) serves `/rest/reports/tat/{summary,detail,trend}` as computed JSON from the OLTP DB to a Carbon dashboard. It needs **zero new infrastructure**, data is **live** (freshness target SC-002 trivially met), and it honors the spec's read-model-agnostic stance with the lightest rung. Demo data volumes are small.
- **Alternatives considered**:
  - *Materialized views / scheduled refresh* — premature; adds Liquibase views + refresh scheduling + a staleness window for no current benefit. **Documented evolution path** if a deployment's data volume makes on-demand aggregation slow.
  - *Dedicated CQRS read store* — over-engineering for a demo; revisit only if reporting load demands it.
  - *FHIR/HAPI as read model* — **rejected** (roadmap §4–5): no vector→FHIR push exists, it would un-park the whole pipeline, and FHIR is a poor analytics substrate.

## D2 — Query mechanism: HQL aggregation in the DAO/service layer

- **Decision**: Express the aggregations (density, species distribution, MIR, positivity, QC pass-rate) as **HQL** queries in the DAO layer; the service compiles them into DTOs within the transaction (Constitution IV data-compilation rule).
- **Rationale**: Constitution IV mandates HQL, not native SQL. The required aggregations (COUNT DISTINCT pools, SUM(sample_item.quantity), GROUP BY species/site/period, anti-join QC exclusion) are expressible in HQL/JPQL.
- **Exception clause**: If a specific index (e.g., a window-function trend) proves impractical in HQL, that one query MAY use native SQL **with a documented justification in the PR** (Constitution IV anti-pattern carve-out). Default is HQL.
- **Alternatives**: Criteria API (more verbose, no benefit here); native SQL throughout (violates IV).

## D3 — MIR / infection-rate computation (BR from spec FR-003)

- **Decision**: Compute in the service from the vector model:
  - **Classical MIR**, **per pathogen × species** = `COUNT(DISTINCT pools POSITIVE for the pathogen) / SUM(sample_item.quantity for the species) × 1000`, where "POSITIVE" means the pool's result resolves to `test_result.significance = POSITIVE` (catalog classification, not a hardcoded `Result.value`).
  - **Deconvolution-aware infection rate** = exact descendant positive counts where `vector_pool.deconvolution_status = COMPLETE`; classical fallback otherwise.
  - **Positive-resolution %** = share of positive pools fully deconvoluted (derivable from `vector_pool.deconvolution_outcome_pct` / sub-pool counts).
- **Rationale**: Grounded in the verified model — `VectorPool` (`deconvolution_status`, `deconvolution_outcome_pct`, `parent_pool_id`), `VectorPoolMember` (pool×sample_item), `Analysis.vector_pool_id → Result → TestResult.significance`, `VectorSpecimenIdentification.confidence='CONFIRMED'`. Positivity is read from the catalog classification, NOT the value-agnostic `deconvolution_status` (which marks both a split-positive and a confirmed-negative pool COMPLETE). When no in-scope result carries a significance tag, the positivity-dependent indices degrade to a "not configured" state rather than fabricating zeros.
- **Alternatives**: MLE estimator — explicitly **deferred** (V-04c) per spec; not in scope.

## D4 — QC exclusion (FR-002)

- **Decision**: Exclude QC samples via **anti-join on `analysis_qaevent` / `sample_qaevent` → `qa_event`** (QC categories: controls, blanks, duplicates). Structural rule, no toggle.
- **Rationale**: Reuses OE's existing QC catalog pattern (verified). Analysis-level exclusion is the precise grain for pool analyses.
- **Inversion-test hook**: a QC sample added to a period MUST NOT change MIR/positivity/density (SC-005) — this is the QC-exclusion unit test.

## D5 — Charting: `@carbon/charts-react` (already a dependency)

- **Decision**: Render the dashboard charts with `@carbon/charts-react` v1.27.3 (`@carbon/charts` v1.27.2) — already in `frontend/package.json`.
- **Rationale**: Constitution II (Carbon-first); no new dependency. (Note: the existing TAT trend tab hand-rolls an SVG bar chart; we standardize on `@carbon/charts` instead, which is cleaner and already present.)
- **Alternatives**: hand-rolled SVG (TAT precedent) — rejected, not maintainable for 5 chart types; recharts/d3 — d3 is present but `@carbon/charts` is the Carbon-native choice.

## D6 — PDF export: client-side `jsPDF` (already a dependency)

- **Decision**: Produce the dashboard PDF **client-side via `jsPDF` + `jspdf-autotable`** (v4.2.1 / v5.0.7, already present), capturing the rendered figures/charts. No server-side Jasper template.
- **Rationale**: The dashboard is a Carbon/React chart view; a JasperReports `.jrxml` would duplicate the whole layout with no charts. The repo already uses client-side jsPDF for ad-hoc exports (`shipment/utils/pdfGenerator.js`). Leaner and chart-capable.
- **Alternatives**: Server-side Jasper — rejected (template duplication, no charts). Revisit only if a pixel-perfect official document format is later mandated.
- **Permission note**: PDF gating is UI-level (hide/disable export for users without the permission); the underlying data endpoint is independently permission-gated.

## D7 — Permissions (FR-010): `system_module` + `system_module_url` + `system_role_module`

- **Decision**: Seed **distinct modules** for view, submit, and field-map admin via Liquibase (the `shipment-007-add-menu-and-role.xml` pattern), each mapped to its REST URL(s) in `system_module_url`; enforced by `ModuleAuthenticationInterceptor`. Frontend gates routes via `SecureRoute` + `UserSessionDetailsContext`.
- **Rationale**: This is the verified OE RBAC mechanism. PDF export shares the view module (UI-level gate).
- **Modules**: `VectorSurveillanceDashboard` (view+export), `VectorManualEntryHelper` (submit), `VectorManualEntryFieldMap` (admin).

## D8 — New entities follow the `VectorTrapType` exemplar

- **Decision**: `ManualEntryFieldMap` and `ManualEntrySubmissionAudit` follow the verified modern pattern: JPA valueholder extends `BaseObject<Integer>`, DAO extends `BaseDAOImpl`, service extends `AuditableBaseObjectServiceImpl`, REST under `/rest/admin/...`, admin React page. Liquibase `042`/`043` in `3.5.x.x/`, registered in `base.xml`; audited via `AuditTrailService` + `reference_tables.keep_history='Y'`.
- **Rationale**: Directly mirrors `org.openelisglobal.vector.valueholder.VectorTrapType` end-to-end (verified). Submission audit is insert-only (immutable).
- **Open assumption**: `ManualEntrySubmissionAudit.value_snapshot` stored as JSON text (snapshot of submitted figures) — matches the FRS intent; no relational explosion.

## Resolved unknowns (no NEEDS CLARIFICATION remain)

- **Freshness** (spec assumption): on-demand = live; SC-002's 15-min window is met with margin.
- **Default metric list** (spec assumption): seed the 8-metric default into `ManualEntryFieldMap`; deployments adjust via the admin screen (FR-009). Confirmable with the program expert without blocking.
- **Sampling-site source**: resolve effective site via `COALESCE(sample_item.collection_location_id, observation_history[vecCollectionSiteId])` (verified).
