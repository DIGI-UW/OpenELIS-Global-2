# M2 Baseline Flaky Scenarios

This file defines the baseline set for reliability SLO tracking introduced in
M2.

## Reliability SLO

- Target: **>=95% pass rate over 20 CI-equivalent runs**
- Scope: Scenarios listed below with `reliability_id` in `parity-matrix.csv`

## Baseline Set

| Reliability ID | Legacy Spec                                    | Current Risk | Baseline Signal               | Planned Milestone |
| -------------- | ---------------------------------------------- | ------------ | ----------------------------- | ----------------- |
| RLY-001        | `cypress/e2e/storageAssignment.cy.js`          | P0           | Skip-heavy legacy scenario    | M6                |
| RLY-002        | `cypress/e2e/storageBoxCRUD-integration.cy.js` | P0           | Skip-heavy legacy scenario    | M6                |
| RLY-003        | `cypress/e2e/storageBoxCRUD.cy.js`             | P0           | Skip-heavy legacy scenario    | M6                |
| RLY-004        | `cypress/e2e/storageLocationCRUD.cy.js`        | P0           | Skip-heavy legacy scenario    | M6                |
| RLY-005        | `cypress/e2e/storageViewStorage.cy.js`         | P0           | Skip-heavy legacy scenario    | M6                |
| RLY-006        | `cypress/e2e/storageSamplesTable.cy.js`        | P1           | No active legacy tests        | M6                |
| RLY-007        | `cypress/e2e/siteBranding.cy.js`               | P1           | Coverage hole in legacy suite | M9                |

## Measurement Protocol

1. Execute migrated Playwright scenarios in CI-equivalent mode for 20 runs.
2. Record per-run pass/fail outcome by `reliability_id`.
3. Compute pass rate per reliability ID and combined baseline cohort.
4. Mark as stabilized when:
   - Each P0 reliability ID meets >=95%, and
   - Overall baseline cohort meets >=95%.
