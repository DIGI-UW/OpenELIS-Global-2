# Data Model: Vector Surveillance Reporting (OGC-585 / V-04)

**Date**: 2026-06-15 | **Feature**: 372-vector-surveillance-reporting

This feature is **read-mostly**. It introduces **two** new persisted entities (for the Manual Entry Helper) plus **one additive catalog column** — `test_result.significance` (the positivity classification, Liquibase 046); everything else is **derived** (computed DTOs) or **consumed read-only** from the existing vector model.

---

## A. New persisted entities (2)

### A1. `ManualEntryFieldMap` (admin-configurable)

Table `clinlims.manual_entry_field_map` — drives which surveillance metrics the Manual Entry Helper shows, in what order, with what label/portal tag. (FR-009)

| Field | Type | Notes |
|---|---|---|
| `id` | Integer (seq) | PK, `manual_entry_field_map_seq` |
| `metricKey` | String(100) | stable key of the surveillance metric (e.g. `MIR_CLASSIC`, `POOLS_POSITIVE`) |
| `fieldOrder` | Integer | display order |
| `label` | String(255) | deployment-facing label (i18n key or literal) |
| `portalTag` | String(50), null | tag/code the national portal uses |
| `visible` | Boolean | hide without delete |
| `lastupdated` | Timestamp | from `BaseObject` (optimistic lock) |

- Extends `BaseObject<Integer>`; `@Entity @Table(schema="clinlims") @DynamicUpdate`.
- Audited (`AuditableBaseObjectServiceImpl` + `reference_tables.keep_history='Y'`).
- **Seed** (Liquibase 043): the 8 default metrics (pools tested, pools positive, confirmed-positive organisms, top species, MIR classical, MIR observed, sporozoite rate, sites with positives).
- **Validation**: `metricKey` non-null + one of the known metric keys; `fieldOrder` non-null.

### A2. `ManualEntrySubmissionAudit` (immutable)

Table `clinlims.manual_entry_submission_audit` — one row per "mark submitted" action. (FR-008)

| Field | Type | Notes |
|---|---|---|
| `id` | Integer (seq) | PK |
| `periodStart` | LocalDate | reporting week start |
| `periodEnd` | LocalDate | reporting week end |
| `siteId` | Integer, null | site scope if submitted per-site |
| `valueSnapshot` | Text (JSON) | snapshot of the submitted figures at submit time |
| `submittedByUserId` | String(50) | acting user (`sysUserId`) |
| `submittedAt` | Timestamp | submission time |
| `lastupdated` | Timestamp | from `BaseObject` |

- Extends `BaseObject<Integer>`; **insert-only** (no update/delete in the service). Re-submission of a week = a new distinct row (FR-008 / US4-4).
- **Validation**: `periodStart ≤ periodEnd`; `valueSnapshot` non-empty; `submittedByUserId` non-null.

---

## B. Derived DTOs (computed, not persisted)

Returned by the analytics service (D1/D2). All compiled within the service transaction (Constitution IV).

- **`SurveillanceIndicesDTO`** — root response for a scope (dateFrom, dateTo, siteId):
  - `freshness` (timestamp "as of")
  - `collectionDensity`: list of `{ periodLabel, siteId, siteName, poolCount, specimenCount }`
  - `speciesDistribution`: list of `{ speciesId, genus, species, specimenCount, pct }`
  - `mirBySpecies`: list of `{ speciesId, speciesLabel, pathogen, mirClassic, infectionRateObserved, positiveResolutionPct, positivePools, totalSpecimens }` — **per species × pathogen** (the pathogen is the detection Test).
  - `pathogenPositivity`: list of `{ pathogen, poolsPositive, poolsTested, positivityPct }` (per pathogen).
  - `qcPassRate`: `{ analysesPassed, analysesTotal, passRatePct }`
  - `sporozoiteRatePct` (top-level, nullable) — the **computed** Anopheles sporozoite rate (CSP-ELISA, LOINC 71712-2); null only when not computable.
  - `positivityConfigured` (boolean) — false when in-scope results carry no significance classification → frontend shows "not configured" (FR-015).
- **`ManualEntryViewDTO`** — `{ periodStart, periodEnd, siteId?, rows: [{ metricKey, label, portalTag, value, gated(bool) }] }` (rows ordered + filtered by the field map; `gated=true` flags the sporozoite rate with a low-resolution caveat when resolution < threshold — US4-3; the value is still shown).

---

## C. Consumed read-only (existing — do NOT modify, except the additive `test_result.significance` catalog column)

Verified vector model (mini-ER):

```
Sample ──1:N──> VectorPool (sample_id, parent_pool_id, deconvolution_status,
   │                         deconvolution_outcome_pct, identification_status)
   │               ├──M:N──> VectorPoolMember (vector_pool_id, sample_item_id)
   │               │            └─> SampleItem (quantity, collection_location_id)
   │               │                  └─1:1─> VectorSpecimenIdentification
   │               │                          (vector_species_id, confidence, lifecycle_stage)
   │               │                            └─> VectorSpecies (genus, species)
   │               └──1:N──> Analysis (vector_pool_id, status, completedDate)
   │                            ├─> Result ─> TestResult (significance: POSITIVE/NEGATIVE/INDETERMINATE)
   │                            └─0:1─> analysis_qaevent ─> qa_event (QC categories)
   └─ observation_history (type='vecCollectionSiteId') ─> VectorSamplingSite
                              (site resolved via COALESCE(sample_item.collection_location_id, …))
```

**Key computations** (per research D3/D4):
- **Density** = COUNT(DISTINCT pool) per site/period (non-QC).
- **Species mix** = specimen counts grouped by `vector_species_id` (CONFIRMED).
- **MIR classic** = positive pools / Σ(sample_item.quantity) × 1000, **per species × pathogen**. A pool is positive for a pathogen when its result resolves to `test_result.significance = POSITIVE` (not `Result.value` or deconvolution status).
- **Infection rate (observed)** = deconvolution-aware (exact individual `sampitem`-anchored POSITIVE counts where `deconvolution_status=COMPLETE`; classical 1-per-pool fallback otherwise).
- **Positivity** = pools with a POSITIVE-significance result / pools tested, **per pathogen**.
- **Sporozoite rate** = Anopheles CSP-ELISA (LOINC 71712-2) POSITIVE pools / Anopheles specimens tested.
- **QC pass-rate** = analyses with no failing `analysis_qaevent` / total surveillance analyses.

---

## D. Liquibase

- Dir `src/main/resources/liquibase/3.5.x.x/`, registered in `base.xml` after `041-laporan-hasil-menu.xml` **in this order** (M1's changeset lands first):
  - `042-vector-surveillance-dashboard-permissions.xml` — **M1**: `VectorSurveillanceDashboard` module + url + grants.
  - `043-manual-entry-field-map.xml` — **M3**: table + seed 8 default metrics.
  - `044-manual-entry-submission-audit.xml` — **M3**: immutable audit table.
  - `045-manual-entry-permissions.xml` — **M3**: `VectorManualEntryHelper` + `VectorManualEntryFieldMap` modules + url + grants.
  - `046-add-test-result-significance.xml` — positivity classification: additive nullable `test_result.significance` column (POSITIVE/NEGATIVE/INDETERMINATE).
- Each changeset includes a `<rollback>` (Constitution VI).
