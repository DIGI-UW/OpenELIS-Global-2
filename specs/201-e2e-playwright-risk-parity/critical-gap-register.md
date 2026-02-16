# M2 Critical Gap Register

This register tracks high-risk gaps identified during parity baseline work.

## Gap Entries (Mixed Status)

Current status summary:

- `Open`: none
- `In Progress`: none
- `Closed`: `GAP-001` through `GAP-007`

| Gap ID  | Legacy Spec                                    | Risk | Gap Type         | Evidence                                             | Status | Target Milestone | Owner  | Notes                                                            |
| ------- | ---------------------------------------------- | ---- | ---------------- | ---------------------------------------------------- | ------ | ---------------- | ------ | ---------------------------------------------------------------- |
| GAP-001 | `cypress/e2e/storageAssignment.cy.js`          | P0   | skip-heavy       | 4 skipped cases in inventory baseline                | Closed | M6               | qa-e2e | Linked reliability ID `RLY-001`; parity matrix status now `PASS` |
| GAP-002 | `cypress/e2e/storageBoxCRUD-integration.cy.js` | P0   | skip-heavy       | 2 skipped cases in inventory baseline                | Closed | M6               | qa-e2e | Linked reliability ID `RLY-002`; parity matrix status now `PASS` |
| GAP-003 | `cypress/e2e/storageBoxCRUD.cy.js`             | P0   | skip-heavy       | 1 skipped case in inventory baseline                 | Closed | M6               | qa-e2e | Linked reliability ID `RLY-003`; parity matrix status now `PASS` |
| GAP-004 | `cypress/e2e/storageLocationCRUD.cy.js`        | P0   | skip-heavy       | 1 skipped case in inventory baseline                 | Closed | M6               | qa-e2e | Linked reliability ID `RLY-004`; parity matrix status now `PASS` |
| GAP-005 | `cypress/e2e/storageSamplesTable.cy.js`        | P1   | no-active-legacy | 0 active tests and 2 skipped tests                   | Closed | M6               | qa-e2e | Linked reliability ID `RLY-006`; parity matrix status now `PASS` |
| GAP-006 | `cypress/e2e/storageViewStorage.cy.js`         | P0   | skip-heavy       | 1 skipped case in inventory baseline                 | Closed | M6               | qa-e2e | Linked reliability ID `RLY-005`; parity matrix status now `PASS` |
| GAP-007 | `cypress/e2e/siteBranding.cy.js`               | P1   | coverage-hole    | Low active coverage and known commented legacy scope | Closed | M9               | qa-e2e | Linked reliability ID `RLY-007`; parity matrix status now `PASS` |

## Update Rules

When updating a gap:

1. Keep `parity-matrix.csv` status aligned for the same legacy spec.
2. Add owner and milestone target before changing status to `In Progress`.
3. Only mark `Closed` when Playwright parity status is `PASS` or approved
   exception is recorded.

## M8a Skipped Legacy Tracking Confirmation (T135)

Skipped/weak legacy coverage remains explicitly tracked for post-migration
review in this register:

- `GAP-001` through `GAP-006`: storage `skip-heavy` / `no-active-legacy` entries
  (target `M6`, currently `Closed`)
- `GAP-007`: core `coverage-hole` entry (target `M9`, currently `Closed`)

This preserves visibility of skipped legacy behavior while M8a parity gate
focuses on current non-skipped Cypress cutoff scope.
