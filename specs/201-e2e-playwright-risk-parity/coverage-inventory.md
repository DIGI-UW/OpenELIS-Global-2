# M1 Coverage Inventory Baseline

Generated for milestone **M1 (Inventory Baseline)** using:

```bash
node scripts/e2e/export-e2e-inventory.js
node scripts/e2e/validate-e2e-inventory.js
```

- Inventory artifact: `specs/201-e2e-playwright-risk-parity/artifacts/inventory.json`
- Generated timestamp is recorded in `inventory.json` as `generatedAt`.

## Framework Summary

| Framework | Specs | Active Specs | Tests Total | Tests Active | Tests Skipped | Tests Fixme |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Cypress | 48 | 47 | 440 | 429 | 11 | 0 |
| Playwright | 13 | 12 | 42 | 41 | 1 | 0 |
| **Total** | **61** | **59** | **482** | **470** | **12** | **0** |

## Domain Summary

| Domain | Specs | Tests Total | Tests Active | Tests Skipped |
| --- | ---: | ---: | ---: | ---: |
| admin | 21 | 133 | 133 | 0 |
| analyzer | 2 | 8 | 8 | 0 |
| auth-nav | 5 | 22 | 21 | 1 |
| clinical | 10 | 169 | 169 | 0 |
| core | 6 | 46 | 46 | 0 |
| dashboard | 2 | 6 | 6 | 0 |
| other | 1 | 2 | 2 | 0 |
| storage | 14 | 97 | 86 | 11 |

## Spec-Level Listing

### Cypress Spec Listing

| Spec | Domain | Tests (active/total) | Skipped | Status |
| --- | --- | ---: | ---: | --- |
| `cypress/e2e/AdminE2E/barcode.cy.js` | admin | 11/11 | 0 | active |
| `cypress/e2e/AdminE2E/batchTestReassignmentandCancelation.cy.js` | admin | 5/5 | 0 | active |
| `cypress/e2e/AdminE2E/calculatedValueTestsManagement.cy.js` | admin | 7/7 | 0 | active |
| `cypress/e2e/AdminE2E/dictionaryMenu.cy.js` | admin | 10/10 | 0 | active |
| `cypress/e2e/AdminE2E/generalConfigurations.cy.js` | admin | 5/5 | 0 | active |
| `cypress/e2e/AdminE2E/MenuConfig/billingMenuConfig.cy.js` | admin | 6/6 | 0 | active |
| `cypress/e2e/AdminE2E/MenuConfig/globalMenuConfig.cy.js` | admin | 5/5 | 0 | active |
| `cypress/e2e/AdminE2E/MenuConfig/nonConformMenuConfig.cy.js` | admin | 7/7 | 0 | active |
| `cypress/e2e/AdminE2E/MenuConfig/patientMenuConfig.cy.js` | admin | 7/7 | 0 | active |
| `cypress/e2e/AdminE2E/MenuConfig/studyMenuConfig.cy.js` | admin | 7/7 | 0 | active |
| `cypress/e2e/AdminE2E/notifyUser.cy.js` | admin | 5/5 | 0 | active |
| `cypress/e2e/AdminE2E/organizationManagement.cy.js` | admin | 6/6 | 0 | active |
| `cypress/e2e/AdminE2E/programEntry.cy.js` | admin | 1/1 | 0 | active |
| `cypress/e2e/AdminE2E/providerManagement.cy.js` | admin | 9/9 | 0 | active |
| `cypress/e2e/AdminE2E/reflexTestsManagement.cy.js` | admin | 3/3 | 0 | active |
| `cypress/e2e/AdminE2E/resultReportingConfig.cy.js` | admin | 3/3 | 0 | active |
| `cypress/e2e/AdminE2E/testManagement.cy.js` | admin | 4/4 | 0 | active |
| `cypress/e2e/AdminE2E/userManagement.cy.js` | admin | 27/27 | 0 | active |
| `cypress/e2e/batchOrderEntry.cy.js` | clinical | 14/14 | 0 | active |
| `cypress/e2e/dashboard.cy.js` | core | 14/14 | 0 | active |
| `cypress/e2e/help.cy.js` | core | 4/4 | 0 | active |
| `cypress/e2e/home.cy.js` | core | 14/14 | 0 | active |
| `cypress/e2e/labNumberManagement.cy.js` | core | 5/5 | 0 | active |
| `cypress/e2e/login.cy.js` | core | 8/8 | 0 | active |
| `cypress/e2e/modifyOrder.cy.js` | clinical | 19/19 | 0 | active |
| `cypress/e2e/nonConform.cy.js` | clinical | 19/19 | 0 | active |
| `cypress/e2e/orderEntity.cy.js` | clinical | 8/8 | 0 | active |
| `cypress/e2e/patientEntry.cy.js` | clinical | 15/15 | 0 | active |
| `cypress/e2e/patientMerge.cy.js` | clinical | 10/10 | 0 | active |
| `cypress/e2e/report.cy.js` | clinical | 38/38 | 0 | active |
| `cypress/e2e/result.cy.js` | clinical | 30/30 | 0 | active |
| `cypress/e2e/siteBranding.cy.js` | core | 1/1 | 0 | active |
| `cypress/e2e/storageAssignment.cy.js` | storage | 1/5 | 4 | active |
| `cypress/e2e/storageBoxCRUD-integration.cy.js` | storage | 2/4 | 2 | active |
| `cypress/e2e/storageBoxCRUD.cy.js` | storage | 3/4 | 1 | active |
| `cypress/e2e/storageDashboard.cy.js` | storage | 5/5 | 0 | active |
| `cypress/e2e/storageDashboardMetrics.cy.js` | storage | 5/5 | 0 | active |
| `cypress/e2e/storageDisposal.cy.js` | storage | 5/5 | 0 | active |
| `cypress/e2e/storageFilters.cy.js` | storage | 11/11 | 0 | active |
| `cypress/e2e/storageLocationCRUD-integration.cy.js` | storage | 4/4 | 0 | active |
| `cypress/e2e/storageLocationCRUD-smoke.cy.js` | storage | 2/2 | 0 | active |
| `cypress/e2e/storageLocationCRUD.cy.js` | storage | 13/14 | 1 | active |
| `cypress/e2e/storageLocationExpandableRows.cy.js` | storage | 16/16 | 0 | active |
| `cypress/e2e/storageSamplesTable.cy.js` | storage | 0/2 | 2 | inactive |
| `cypress/e2e/storageSearch.cy.js` | storage | 16/16 | 0 | active |
| `cypress/e2e/storageViewStorage.cy.js` | storage | 3/4 | 1 | active |
| `cypress/e2e/validation.cy.js` | clinical | 7/7 | 0 | active |
| `cypress/e2e/workplan.cy.js` | clinical | 9/9 | 0 | active |

### Playwright Spec Listing

| Spec | Domain | Tests (active/total) | Skipped | Status |
| --- | --- | ---: | ---: | --- |
| `playwright/tests/admin-barcode-core.spec.ts` | admin | 1/1 | 0 | active |
| `playwright/tests/admin-organization-provider.spec.ts` | admin | 2/2 | 0 | active |
| `playwright/tests/admin-user-management.spec.ts` | admin | 2/2 | 0 | active |
| `playwright/tests/analyzer-list.spec.ts` | analyzer | 5/5 | 0 | active |
| `playwright/tests/analyzer-navigation.spec.ts` | analyzer | 3/3 | 0 | active |
| `playwright/tests/auth-login.spec.ts` | auth-nav | 3/3 | 0 | active |
| `playwright/tests/auth.setup.ts` | auth-nav | 0/0 | 0 | inactive |
| `playwright/tests/dashboard-smoke.spec.ts` | dashboard | 1/1 | 0 | active |
| `playwright/tests/error-dashboard.spec.ts` | dashboard | 5/5 | 0 | active |
| `playwright/tests/harness-smoke.spec.ts` | other | 2/2 | 0 | active |
| `playwright/tests/home-navigation.spec.ts` | auth-nav | 2/2 | 0 | active |
| `playwright/tests/navbar.spec.ts` | auth-nav | 5/5 | 0 | active |
| `playwright/tests/sidenav.spec.ts` | auth-nav | 10/11 | 1 | active |

## Validation Gate Output (T007)

```text
$ node scripts/e2e/validate-e2e-inventory.js
PASS: inventory covers all active specs (59 active specs).
```

## M1 Milestone Gate Status

- [x] Active Cypress specs represented in inventory
- [x] Active Playwright specs represented in inventory
- [x] Validation command is repeatable and passing
- [x] Spec-level listing includes domain, counts, and skip status

## M4a Migration Evidence Notes (Auth/Nav)

- Added Playwright specs:
  - `frontend/playwright/tests/auth-login.spec.ts`
  - `frontend/playwright/tests/home-navigation.spec.ts`
  - `frontend/playwright/tests/dashboard-smoke.spec.ts`
- Parity matrix updates (implementation-complete, gate pending):
  - `LEG-CYP-024` (`login.cy.js`) -> status `PARTIAL`
  - `LEG-CYP-022` (`home.cy.js`) -> status `PARTIAL`
  - `LEG-CYP-020` (`dashboard.cy.js`) -> status `PARTIAL`
- These rows will move to `PASS` after M4a gate validation (T066/T067).

## M4b Migration Evidence Notes (Admin-Core)

- Added Playwright specs:
  - `frontend/playwright/tests/admin-user-management.spec.ts`
  - `frontend/playwright/tests/admin-organization-provider.spec.ts`
  - `frontend/playwright/tests/admin-barcode-core.spec.ts`
- Parity matrix updates (implementation-complete, gate pending):
  - `LEG-CYP-018` (`userManagement.cy.js`) -> status `PARTIAL`
  - `LEG-CYP-012` (`organizationManagement.cy.js`) -> status `PARTIAL`
  - `LEG-CYP-014` (`providerManagement.cy.js`) -> status `PARTIAL`
  - `LEG-CYP-001` (`barcode.cy.js`) -> status `PARTIAL`
- These rows will move to `PASS` after M4b gate validation (T075/T076).

## M5 Migration Evidence Notes (Clinical P0)

- Added Playwright specs:
  - `frontend/playwright/tests/clinical-patient-order.spec.ts`
  - `frontend/playwright/tests/clinical-result-validation.spec.ts`
  - `frontend/playwright/tests/clinical-report-workplan.spec.ts`
  - `frontend/playwright/tests/clinical-nonconform.spec.ts`
- Parity matrix updates (implementation-complete, gate pending):
  - `LEG-CYP-019`, `LEG-CYP-025`, `LEG-CYP-027`, `LEG-CYP-028` -> status `PARTIAL`
  - `LEG-CYP-031`, `LEG-CYP-047` -> status `PARTIAL`
  - `LEG-CYP-030`, `LEG-CYP-048` -> status `PARTIAL`
  - `LEG-CYP-026` -> status `PARTIAL`
- These rows will move to `PASS` after M5 gate validation (T086/T087).

