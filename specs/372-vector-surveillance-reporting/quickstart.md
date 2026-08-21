# Quickstart: Vector Surveillance Reporting (OGC-585 / V-04)

**Branch**: `372-vector-surveillance-reporting` (off `demo-silnas`)

## Prerequisites

- Java 21 LTS (`java -version` → 21). Jakarta EE 9, Spring Framework 6.2 (Traditional MVC).
- The vector module (V-01/02/03) is present on `demo-silnas` (taxonomy, collection/pools, identification, deconvolution) — this feature reads it.
- Docker (for integration tests via Testcontainers / `BaseWebContextSensitiveTest`).

## Build (skip tests for a fast loop)

```bash
mvn clean install -DskipTests -Dmaven.test.skip=true
```

## Run the app + frontend

```bash
# backend (assembled WAR) — see restart-dev-env skill for the full compose
# frontend
cd frontend && npm install && npm start
```

## Verify the feature (maps to user stories)

1. **Dashboard (US1)** — record a few vector collections, pool results, and species IDs, then open **Reports → Vector Surveillance**. Confirm density, species distribution, MIR (classic + observed + resolution %), pathogen positivity, and QC pass-rate render. Add a QC sample → confirm MIR/positivity/density do **not** change (SC-005).
2. **Filters (US2)** — apply a date range + a sampling site; confirm every chart recomputes; pick an empty scope → empty state, not an error (FR-012).
3. **PDF (US3)** — export; confirm a downloadable PDF reflects the on-screen figures (client-side jsPDF).
4. **Manual Entry Helper (US4)** — open the helper for a week; copy values; "mark submitted"; confirm an audit row exists (`GET /rest/reports/vector-surveillance/manual-entry/audit`). Re-submit → a second distinct audit row.
5. **Field map (US5)** — as admin, reorder/hide/relabel a metric; confirm the helper reflects it with no redeploy.

## Permissions (FR-010)

A user lacking each module is blocked:
- `VectorSurveillanceDashboard` → dashboard + export
- `VectorManualEntryHelper` → submit
- `VectorManualEntryFieldMap` → field-map admin

## Key endpoints (see `contracts/vector-surveillance-api.yaml`)

- `GET /rest/reports/vector-surveillance/indices?dateFrom&dateTo&siteId`
- `GET /rest/reports/vector-surveillance/manual-entry?periodStart&periodEnd`
- `POST /rest/reports/vector-surveillance/manual-entry/submit`
- `GET/POST/PUT /rest/admin/vector/manual-entry-fields`

## Tests

```bash
# backend unit/integration for the analytics service + entities
mvn -Dtest=VectorSurveillance*Test test
# frontend unit
cd frontend && npm test -- VectorSurveillance
# E2E (Playwright, recommended)
cd frontend && npm run pw:test -- vector-surveillance.spec.ts
```

The MIR/QC-exclusion **inversion test** is the critical one: replace the aggregation with a constant and the test MUST fail (Constitution V.6).
