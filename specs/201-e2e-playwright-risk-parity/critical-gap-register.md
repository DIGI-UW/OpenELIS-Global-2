# M2 Critical Gap Register

This register tracks high-risk gaps identified during parity baseline work.

## Open Gaps

| Gap ID | Legacy Spec | Risk | Gap Type | Evidence | Status | Target Milestone | Owner | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| GAP-001 | `cypress/e2e/storageAssignment.cy.js` | P0 | skip-heavy | 4 skipped cases in inventory baseline | Open | M6 | qa-e2e | Linked reliability ID `RLY-001` |
| GAP-002 | `cypress/e2e/storageBoxCRUD-integration.cy.js` | P0 | skip-heavy | 2 skipped cases in inventory baseline | Open | M6 | qa-e2e | Linked reliability ID `RLY-002` |
| GAP-003 | `cypress/e2e/storageBoxCRUD.cy.js` | P0 | skip-heavy | 1 skipped case in inventory baseline | Open | M6 | qa-e2e | Linked reliability ID `RLY-003` |
| GAP-004 | `cypress/e2e/storageLocationCRUD.cy.js` | P0 | skip-heavy | 1 skipped case in inventory baseline | Open | M6 | qa-e2e | Linked reliability ID `RLY-004` |
| GAP-005 | `cypress/e2e/storageSamplesTable.cy.js` | P1 | no-active-legacy | 0 active tests and 2 skipped tests | Open | M6 | qa-e2e | Linked reliability ID `RLY-006` |
| GAP-006 | `cypress/e2e/storageViewStorage.cy.js` | P0 | skip-heavy | 1 skipped case in inventory baseline | Open | M6 | qa-e2e | Linked reliability ID `RLY-005` |
| GAP-007 | `cypress/e2e/siteBranding.cy.js` | P1 | coverage-hole | Low active coverage and known commented legacy scope | Open | M9 | qa-e2e | Linked reliability ID `RLY-007` |

## Update Rules

When updating a gap:

1. Keep `parity-matrix.csv` status aligned for the same legacy spec.
2. Add owner and milestone target before changing status to `In Progress`.
3. Only mark `Closed` when Playwright parity status is `PASS` or approved
   exception is recorded.
