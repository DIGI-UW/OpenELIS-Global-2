# Biorepository QC evidence assets

Screenshots captured from the AHRI/dev Docker stack (`https://localhost`) on **2026-06-01** using `frontend/cypress/e2e/biorepositoryQcEvidence.cy.js` (notebook template **27**).

| File | What it shows |
|------|----------------|
| `01-storage-management-active-samples.png` | Storage Management active sample-items |
| `03-qc-overview-nonzero-counts.png` | QC Inspection KPIs (Total Stored > 0) + pool aligned with storage |
| `04-qc-device-selected.png` | Freezer/device filter applied before round generation |
| `05-qc-round-generated.png` | Random QC round generated (1 box × 2 samples in dev) |
| `06-qc-pass-saved.png` | PASS via Check All → Record Verification |
| `07-qc-fail-validation.png` | FAIL guardrail: Record Discrepancy blocked without discrepancy type |
| `08-qc-fail-saved-with-correction.png` | FAIL saved with correction workflow (supervisor review) |
| `09-qc-escalation-alert.png` | Batch escalation banner after batch fail-rate threshold |
| `10-qc-history-audit-trail.png` | Per-sample inspection history modal |

`02-qc-before-fix.png` is intentionally omitted (no pre-fix branch in this environment).

Guides: [DEMO_GUIDE.md](./DEMO_GUIDE.md) · [DATA_SOURCE_TRACE.md](./DATA_SOURCE_TRACE.md)

Re-capture:

```bash
cd frontend
npx cypress run --browser electron --config defaultCommandTimeout=120000,video=false \
  --spec cypress/e2e/biorepositoryQcEvidence.cy.js
cp cypress/screenshots/biorepositoryQcEvidence.cy.js/biorepository-qc-evidence/*.png \
  ../docs/biorepository-qc-evidence/
```
