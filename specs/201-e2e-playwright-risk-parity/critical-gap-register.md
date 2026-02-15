# M2 Critical Gap Register

This register tracks high-risk gaps identified during parity baseline work.

## Gap Entries (Mixed Status)

Current status summary:

- `Open`: `GAP-007` (target milestone `M9`)
- `In Progress`: `GAP-001` through `GAP-006` (target milestone `M6`)

| Gap ID  | Legacy Spec                                    | Risk | Gap Type         | Evidence                                             | Status      | Target Milestone | Owner  | Notes                                                                |
| ------- | ---------------------------------------------- | ---- | ---------------- | ---------------------------------------------------- | ----------- | ---------------- | ------ | -------------------------------------------------------------------- |
| GAP-001 | `cypress/e2e/storageAssignment.cy.js`          | P0   | skip-heavy       | 4 skipped cases in inventory baseline                | In Progress | M6               | qa-e2e | Linked reliability ID `RLY-001`; Playwright target added (`PARTIAL`) |
| GAP-002 | `cypress/e2e/storageBoxCRUD-integration.cy.js` | P0   | skip-heavy       | 2 skipped cases in inventory baseline                | In Progress | M6               | qa-e2e | Linked reliability ID `RLY-002`; Playwright target added (`PARTIAL`) |
| GAP-003 | `cypress/e2e/storageBoxCRUD.cy.js`             | P0   | skip-heavy       | 1 skipped case in inventory baseline                 | In Progress | M6               | qa-e2e | Linked reliability ID `RLY-003`; Playwright target added (`PARTIAL`) |
| GAP-004 | `cypress/e2e/storageLocationCRUD.cy.js`        | P0   | skip-heavy       | 1 skipped case in inventory baseline                 | In Progress | M6               | qa-e2e | Linked reliability ID `RLY-004`; Playwright target added (`PARTIAL`) |
| GAP-005 | `cypress/e2e/storageSamplesTable.cy.js`        | P1   | no-active-legacy | 0 active tests and 2 skipped tests                   | In Progress | M6               | qa-e2e | Linked reliability ID `RLY-006`; Playwright target added (`PARTIAL`) |
| GAP-006 | `cypress/e2e/storageViewStorage.cy.js`         | P0   | skip-heavy       | 1 skipped case in inventory baseline                 | In Progress | M6               | qa-e2e | Linked reliability ID `RLY-005`; Playwright target added (`PARTIAL`) |
| GAP-007 | `cypress/e2e/siteBranding.cy.js`               | P1   | coverage-hole    | Low active coverage and known commented legacy scope | Open        | M9               | qa-e2e | Linked reliability ID `RLY-007`                                      |

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
  (target `M6`, currently `In Progress`)
- `GAP-007`: core `coverage-hole` entry (target `M9`, currently `Open`)

This preserves visibility of skipped legacy behavior while M8a parity gate
focuses on current non-skipped Cypress cutoff scope.
