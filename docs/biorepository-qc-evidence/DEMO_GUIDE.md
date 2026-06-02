# Biorepository Periodic QC — Demo Guide

## Prerequisites

- OpenELIS-Global running locally (AHRI/dev stack) with biorepository storage data
- Admin user: `admin` / `adminADMIN!`
- Biorepository **notebook template** id **27** (not notebook entry id 16)
- At least one **active** sample-item with a storage assignment under department **Biorepository Laboratory** (test section **196** in dev)
- For local WAR with QC pool fix: `docker compose -f docker-compose.yml -f docker-compose.qc-deploy.yml up -d oe.openelis.org` after `mvn package -DskipTests`

## URLs

| Step | URL |
|------|-----|
| Storage Management | `https://localhost/Storage/samples` |
| Notebook QC (stage 6) | `https://localhost/NoteBookInstanceEditForm/27?mode=edit` → Workflow → **QC Inspection** |
| QC APIs (base) | `https://localhost/OpenELIS-Global/rest/biorepository/qc-inspection/...` |

## Verified counts (dev Docker, 2026-06-01)

| Source | Metric | Value |
|--------|--------|-------|
| Storage | `GET /rest/storage/sample-items?countOnly=true` → active | **20** |
| QC overview | `GET .../storage-overview?notebookId=27&includeInspected=true` → totalStored | **19** |
| QC diagnostics | qcPoolTotal / storageManagementActiveInScope | **19** / **19** |
| QC diagnostics | excludedNotInScope | **1** |
| QC diagnostics | bioSamplesLazyLinked | **0** |
| QC samples | `GET .../samples?notebookId=27` | **19** rows |

Production AHRI may show ~4055 active items; the alignment principle is the same: QC uses `getAllSamplesWithAssignments()` + biorepository scope, not `BioSample` STORED-only.

### Production server (`192.168.25.25`)

| Step | URL |
|------|-----|
| Storage Management | `https://192.168.25.25/Storage/samples` |
| Notebook workflow | `https://192.168.25.25/NoteBookInstanceEditForm/117?mode=edit` |
| QC Inspection | Workflow → **6. QC Inspection** |
| Ongoing Monitoring | Workflow → **3. Ongoing Storage and Monitoring** |
| Sample attach | Workflow → **5. Sample Request** → Work Order Details → attach |

**Post-deploy checks:**

1. QC KPI **Total Stored** > 0 (UI must not say “No samples with STORED status…”).
2. Monitoring **STORAGE DEVICES** > 0; **Log Room Environment** room dropdown populated.
3. Attach sample on work order returns 200 (no `NotebookEntry` proxy / no Session error).
4. QC FAIL correction box dropdown lists biorepository boxes only.

## Step-by-step proof

### 1. Storage Management baseline

1. Open `/Storage/samples`.
2. Note active sample-items (dev: **20**).

**Screenshot:** `01-storage-management-active-samples.png`

### 2. QC overview KPIs (same population)

1. Open `/NoteBookInstanceEditForm/27?mode=edit` → Workflow → **QC Inspection**.
2. Enable **Include samples already inspected this quarter** for the full pool.
3. Confirm **Total Stored** > 0 and matches biorepository scope (dev: **19**).

**Screenshot:** `03-qc-overview-nonzero-counts.png`

### 3. Device filter

1. Select a freezer/device when multiple exist.
2. Dev: use **boxesPerRound = 1** (only one eligible box).

**Screenshot:** `04-qc-device-selected.png`

### 4. Generate random QC round

1. Set boxes per round / samples per box (evidence spec: 1 × 2).
2. Click **Generate Random QC Round**.

**Screenshot:** `05-qc-round-generated.png`

### 5. FAIL + batch escalation

1. Record discrepancies for samples in the active `qcBatchId` (two FAILs in a 2-sample round triggers >5% batch fail rate).
2. Use correction workflow on the second FAIL (e.g. Request supervisor review).

**Screenshots:** `08-qc-fail-saved-with-correction.png`, `09-qc-escalation-alert.png`

### 6. PASS inspection

1. **Inspect** → **Check All (Verify)** → **Record Verification**.

**Screenshot:** `06-qc-pass-saved.png`

### 7. FAIL validation (no save)

1. **Inspect** → **Clear All** → **Record Discrepancy** without choosing discrepancy type.
2. Expect inline error: “Please select a discrepancy type.”

**Screenshot:** `07-qc-fail-validation.png`

### 8. Inspection history

1. **Inspect** → **Inspection history** on a sample with prior QC.

**Screenshot:** `10-qc-history-audit-trail.png`

## Automated evidence capture

```bash
cd frontend
npx cypress run --browser electron --config defaultCommandTimeout=120000,video=false \
  --spec cypress/e2e/biorepositoryQcEvidence.cy.js
cp cypress/screenshots/biorepositoryQcEvidence.cy.js/biorepository-qc-evidence/*.png \
  ../docs/biorepository-qc-evidence/
```

## Troubleshooting

| Symptom | Check |
|---------|--------|
| No “QC Inspection” workflow | URL must use **notebook template id** (27), not entry id 16 |
| QC Total Stored = 0, Storage > 0 | Deploy WAR with `BiorepositoryQcSamplePoolServiceImpl`; pass `notebookId=27`; read `diagnostics.excludedNotInScope` |
| Wrong department (e.g. dept 187) | Notebook 16 is Traditional & Modern Medicine; use template 27 / dept 196 |
| `generate-round` 400 “only N eligible boxes” | Lower `boxesPerRound` to 1 in dev |
| `bulk-apply` 409 in same batch | Cannot re-inspect same `bioSampleId` + `qcBatchId`; use another sample in the round |
| `bioSamplesLazyLinked` > 0 on first run | Expected when pool creates BioSample links on first overview load |

## Automated tests (no UI)

```bash
# Backend
mvn test -Dtest=BiorepositoryQcSamplePoolServiceTest,BiorepositoryQcSamplePoolVsStorageDashboardTest,BiorepositoryQcRoundGenerationServiceTest

# Frontend helpers (optional)
cd frontend && npm test -- --testPathPattern=biorepositoryQcScopeHelpers
```

Integration tests (Testcontainers) require Docker and are not run in the evidence pipeline by default.
