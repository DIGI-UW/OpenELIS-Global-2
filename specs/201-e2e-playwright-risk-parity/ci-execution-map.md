# M1 CI Execution Map

This document maps E2E specs to their current CI execution points.

## Source Workflows

- `.github/workflows/frontend-qa.yml` (Cypress)
- `.github/workflows/playwright-e2e.yml` (Playwright)
- `.github/workflows/e2e-parity-report.yml` (cross-workflow parity report)

## Cypress CI Mapping (`frontend-qa.yml`)

### Job Structure

| Job                      | Purpose                                | Notes                                             |
| ------------------------ | -------------------------------------- | ------------------------------------------------- |
| `static-checks`          | frontend formatting + unit test checks | Runs before E2E Cypress                           |
| `e2e-cypress`            | matrix Cypress execution               | Shards: `core`, `storage`, `admin`, `independent` |
| `build-and-run-qa-tests` | fan-in status gate                     | Consolidated branch-protection check name         |

Each `e2e-cypress` shard now emits:

- Raw JSON reporter artifact: `cypress-raw-json-<shard>`
- Normalized results artifact: `cypress-normalized-<shard>`

### Matrix Shard Summary

| Shard       | Mapping Type      | Explicit Specs in Workflow | Current Resolved Specs |
| ----------- | ----------------- | -------------------------: | ---------------------: |
| core        | static list       |                         15 |                     15 |
| storage     | static list       |                         14 |                     14 |
| admin       | static list       |                          8 |                      8 |
| independent | dynamic catch-all |                          0 |                     11 |

### Shard -> Spec Mapping

#### core (15)

- `cypress/e2e/login.cy.js`
- `cypress/e2e/home.cy.js`
- `cypress/e2e/AdminE2E/organizationManagement.cy.js`
- `cypress/e2e/AdminE2E/providerManagement.cy.js`
- `cypress/e2e/patientEntry.cy.js`
- `cypress/e2e/orderEntity.cy.js`
- `cypress/e2e/modifyOrder.cy.js`
- `cypress/e2e/batchOrderEntry.cy.js`
- `cypress/e2e/workplan.cy.js`
- `cypress/e2e/result.cy.js`
- `cypress/e2e/validation.cy.js`
- `cypress/e2e/nonConform.cy.js`
- `cypress/e2e/dashboard.cy.js`
- `cypress/e2e/report.cy.js`
- `cypress/e2e/AdminE2E/barcode.cy.js`

#### storage (14)

- `cypress/e2e/storageAssignment.cy.js`
- `cypress/e2e/storageBoxCRUD.cy.js`
- `cypress/e2e/storageBoxCRUD-integration.cy.js`
- `cypress/e2e/storageDashboard.cy.js`
- `cypress/e2e/storageDashboardMetrics.cy.js`
- `cypress/e2e/storageDisposal.cy.js`
- `cypress/e2e/storageFilters.cy.js`
- `cypress/e2e/storageLocationCRUD.cy.js`
- `cypress/e2e/storageLocationCRUD-integration.cy.js`
- `cypress/e2e/storageLocationCRUD-smoke.cy.js`
- `cypress/e2e/storageLocationExpandableRows.cy.js`
- `cypress/e2e/storageSamplesTable.cy.js`
- `cypress/e2e/storageSearch.cy.js`
- `cypress/e2e/storageViewStorage.cy.js`

#### admin (8)

- `cypress/e2e/AdminE2E/userManagement.cy.js`
- `cypress/e2e/siteBranding.cy.js`
- `cypress/e2e/patientMerge.cy.js`
- `cypress/e2e/help.cy.js`
- `cypress/e2e/labNumberManagement.cy.js`
- `cypress/e2e/AdminE2E/dictionaryMenu.cy.js`
- `cypress/e2e/AdminE2E/calculatedValueTestsManagement.cy.js`
- `cypress/e2e/AdminE2E/batchTestReassignmentandCancelation.cy.js`

#### independent (dynamic catch-all, currently 11)

Computed at runtime as:

`all cypress/e2e/**/*.cy.js - (core U storage U admin)`

Current resolved set:

- `cypress/e2e/AdminE2E/MenuConfig/billingMenuConfig.cy.js`
- `cypress/e2e/AdminE2E/MenuConfig/globalMenuConfig.cy.js`
- `cypress/e2e/AdminE2E/MenuConfig/nonConformMenuConfig.cy.js`
- `cypress/e2e/AdminE2E/MenuConfig/patientMenuConfig.cy.js`
- `cypress/e2e/AdminE2E/MenuConfig/studyMenuConfig.cy.js`
- `cypress/e2e/AdminE2E/generalConfigurations.cy.js`
- `cypress/e2e/AdminE2E/notifyUser.cy.js`
- `cypress/e2e/AdminE2E/programEntry.cy.js`
- `cypress/e2e/AdminE2E/reflexTestsManagement.cy.js`
- `cypress/e2e/AdminE2E/resultReportingConfig.cy.js`
- `cypress/e2e/AdminE2E/testManagement.cy.js`

## Playwright CI Mapping (`playwright-e2e.yml`)

### Job Structure

| Job                | Command                          | Scope                                                                               |
| ------------------ | -------------------------------- | ----------------------------------------------------------------------------------- |
| `playwright-tests` | `npm run pw:test -- --shard=x/3` | Executes Playwright projects from `frontend/playwright.config.ts` via matrix shards |
| `playwright-e2e`   | fan-in + blob merge              | Consolidates shard outcome and merges blob reports into a single HTML artifact      |

Each `playwright-tests` shard now emits:

- Normalized results artifact: `playwright-normalized-<shard>`
- JSON artifact: `playwright-json-<shard>`
- Blob report artifact: `playwright-blob-report-<shard>`

Fan-in job emits:

- Merged Playwright report artifact: `playwright-merged-report`

## Parity Report Workflow (`e2e-parity-report.yml`)

### Trigger + Output

- Triggered by completion of either E2E workflow (`workflow_run`) and by manual
  dispatch.
- Resolves latest completed Cypress + Playwright runs for the same head SHA.
- Downloads normalized artifacts and generates:
  - `artifacts/runtime-metrics.json`
  - `artifacts/parity-report.json`
  - `artifacts/parity-report.md`
- Publishes artifact bundle: `e2e-parity-report-<sha>`

### Project -> Spec Mapping

| Project    | Matching Rule                                                          | Current Specs                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| ---------- | ---------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `setup`    | `testMatch: /.*\\.setup\\.ts/`                                         | `playwright/tests/auth.setup.ts`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| `chromium` | default Playwright spec match (`*.spec.ts`) with dependency on `setup` | `playwright/tests/admin-barcode-core.spec.ts`, `playwright/tests/admin-organization-provider.spec.ts`, `playwright/tests/admin-user-management.spec.ts`, `playwright/tests/analyzer-list.spec.ts`, `playwright/tests/analyzer-navigation.spec.ts`, `playwright/tests/auth-login.spec.ts`, `playwright/tests/clinical-nonconform.spec.ts`, `playwright/tests/clinical-patient-order.spec.ts`, `playwright/tests/clinical-report-workplan.spec.ts`, `playwright/tests/clinical-result-validation.spec.ts`, `playwright/tests/dashboard-smoke.spec.ts`, `playwright/tests/error-dashboard.spec.ts`, `playwright/tests/harness-smoke.spec.ts`, `playwright/tests/home-navigation.spec.ts`, `playwright/tests/navbar.spec.ts`, `playwright/tests/sidenav.spec.ts`, `playwright/tests/storage-assignment-critical.spec.ts`, `playwright/tests/storage-box-crud-critical.spec.ts`, `playwright/tests/storage-samples-visibility.spec.ts`, `playwright/tests/storage-view-edit-critical.spec.ts` |

## Notes for Migration Planning

- Cypress CI currently runs via matrix sharding plus a dynamic catch-all shard.
- Playwright CI runs as matrix shards (`1/3`, `2/3`, `3/3`) plus fan-in merge.
- Parity reporting is now artifact-driven and cross-workflow.
- For parity work, this mapping can be used to identify where migrated specs
  should be added and where legacy comparisons are still executed.
