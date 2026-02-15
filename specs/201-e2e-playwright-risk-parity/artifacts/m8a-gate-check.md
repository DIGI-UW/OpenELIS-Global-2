# M8a Parity Gate Check

Generated at: 2026-02-15T20:07:59.169Z

## Scope

- Commit SHA: 01f70c7758cc7ecf1bc15348990a8845f2504c9b
- Scoped non-skipped Cypress scenarios: **47**

## Gate Result

- Gate pass: **NO**
- Blocking rows: **47**

## Blocking Rows (first 50)

| Scenario ID | Risk | Domain   | Status      | Legacy Spec                                                    | Playwright Spec                                        |
| ----------- | ---- | -------- | ----------- | -------------------------------------------------------------- | ------------------------------------------------------ |
| LEG-CYP-001 | P0   | admin    | PARTIAL     | cypress/e2e/AdminE2E/barcode.cy.js                             | playwright/tests/admin-barcode-core.spec.ts            |
| LEG-CYP-002 | P1   | admin    | LEGACY_ONLY | cypress/e2e/AdminE2E/batchTestReassignmentandCancelation.cy.js | playwright/tests/admin-batch-reassignment.spec.ts      |
| LEG-CYP-003 | P1   | admin    | LEGACY_ONLY | cypress/e2e/AdminE2E/calculatedValueTestsManagement.cy.js      | playwright/tests/admin-calculated-values.spec.ts       |
| LEG-CYP-004 | P1   | admin    | LEGACY_ONLY | cypress/e2e/AdminE2E/dictionaryMenu.cy.js                      | playwright/tests/admin-dictionary-menu.spec.ts         |
| LEG-CYP-005 | P1   | admin    | LEGACY_ONLY | cypress/e2e/AdminE2E/generalConfigurations.cy.js               | playwright/tests/admin-general-config.spec.ts          |
| LEG-CYP-006 | P2   | admin    | LEGACY_ONLY | cypress/e2e/AdminE2E/MenuConfig/billingMenuConfig.cy.js        | playwright/tests/admin-menu-billing.spec.ts            |
| LEG-CYP-007 | P2   | admin    | LEGACY_ONLY | cypress/e2e/AdminE2E/MenuConfig/globalMenuConfig.cy.js         | playwright/tests/admin-menu-global.spec.ts             |
| LEG-CYP-008 | P2   | admin    | LEGACY_ONLY | cypress/e2e/AdminE2E/MenuConfig/nonConformMenuConfig.cy.js     | playwright/tests/admin-menu-nonconform.spec.ts         |
| LEG-CYP-009 | P2   | admin    | LEGACY_ONLY | cypress/e2e/AdminE2E/MenuConfig/patientMenuConfig.cy.js        | playwright/tests/admin-menu-patient.spec.ts            |
| LEG-CYP-010 | P2   | admin    | LEGACY_ONLY | cypress/e2e/AdminE2E/MenuConfig/studyMenuConfig.cy.js          | playwright/tests/admin-menu-study.spec.ts              |
| LEG-CYP-011 | P2   | admin    | LEGACY_ONLY | cypress/e2e/AdminE2E/notifyUser.cy.js                          | playwright/tests/admin-notify-user.spec.ts             |
| LEG-CYP-012 | P0   | admin    | PARTIAL     | cypress/e2e/AdminE2E/organizationManagement.cy.js              | playwright/tests/admin-organization-provider.spec.ts   |
| LEG-CYP-013 | P1   | admin    | LEGACY_ONLY | cypress/e2e/AdminE2E/programEntry.cy.js                        | playwright/tests/admin-program-entry.spec.ts           |
| LEG-CYP-014 | P0   | admin    | PARTIAL     | cypress/e2e/AdminE2E/providerManagement.cy.js                  | playwright/tests/admin-organization-provider.spec.ts   |
| LEG-CYP-015 | P2   | admin    | LEGACY_ONLY | cypress/e2e/AdminE2E/reflexTestsManagement.cy.js               | playwright/tests/admin-reflex-tests.spec.ts            |
| LEG-CYP-016 | P2   | admin    | LEGACY_ONLY | cypress/e2e/AdminE2E/resultReportingConfig.cy.js               | playwright/tests/admin-result-reporting-config.spec.ts |
| LEG-CYP-017 | P2   | admin    | LEGACY_ONLY | cypress/e2e/AdminE2E/testManagement.cy.js                      | playwright/tests/admin-test-management.spec.ts         |
| LEG-CYP-018 | P0   | admin    | PARTIAL     | cypress/e2e/AdminE2E/userManagement.cy.js                      | playwright/tests/admin-user-management.spec.ts         |
| LEG-CYP-019 | P0   | clinical | PARTIAL     | cypress/e2e/batchOrderEntry.cy.js                              | playwright/tests/clinical-patient-order.spec.ts        |
| LEG-CYP-020 | P1   | core     | PARTIAL     | cypress/e2e/dashboard.cy.js                                    | playwright/tests/dashboard-smoke.spec.ts               |
| LEG-CYP-021 | P1   | core     | LEGACY_ONLY | cypress/e2e/help.cy.js                                         | playwright/tests/help-navigation.spec.ts               |
| LEG-CYP-022 | P0   | core     | PARTIAL     | cypress/e2e/home.cy.js                                         | playwright/tests/home-navigation.spec.ts               |
| LEG-CYP-023 | P1   | core     | LEGACY_ONLY | cypress/e2e/labNumberManagement.cy.js                          | playwright/tests/lab-number-management.spec.ts         |
| LEG-CYP-024 | P0   | core     | PARTIAL     | cypress/e2e/login.cy.js                                        | playwright/tests/auth-login.spec.ts                    |
| LEG-CYP-025 | P0   | clinical | PARTIAL     | cypress/e2e/modifyOrder.cy.js                                  | playwright/tests/clinical-patient-order.spec.ts        |
| LEG-CYP-026 | P0   | clinical | PARTIAL     | cypress/e2e/nonConform.cy.js                                   | playwright/tests/clinical-nonconform.spec.ts           |
| LEG-CYP-027 | P0   | clinical | PARTIAL     | cypress/e2e/orderEntity.cy.js                                  | playwright/tests/clinical-patient-order.spec.ts        |
| LEG-CYP-028 | P0   | clinical | PARTIAL     | cypress/e2e/patientEntry.cy.js                                 | playwright/tests/clinical-patient-order.spec.ts        |
| LEG-CYP-029 | P1   | clinical | LEGACY_ONLY | cypress/e2e/patientMerge.cy.js                                 | playwright/tests/patient-merge.spec.ts                 |
| LEG-CYP-030 | P0   | clinical | PARTIAL     | cypress/e2e/report.cy.js                                       | playwright/tests/clinical-report-workplan.spec.ts      |
| LEG-CYP-031 | P0   | clinical | PARTIAL     | cypress/e2e/result.cy.js                                       | playwright/tests/clinical-result-validation.spec.ts    |
| LEG-CYP-032 | P1   | core     | GAP         | cypress/e2e/siteBranding.cy.js                                 | playwright/tests/site-branding.spec.ts                 |
| LEG-CYP-033 | P0   | storage  | PARTIAL     | cypress/e2e/storageAssignment.cy.js                            | playwright/tests/storage-assignment-critical.spec.ts   |
| LEG-CYP-034 | P0   | storage  | PARTIAL     | cypress/e2e/storageBoxCRUD-integration.cy.js                   | playwright/tests/storage-box-crud-critical.spec.ts     |
| LEG-CYP-035 | P0   | storage  | PARTIAL     | cypress/e2e/storageBoxCRUD.cy.js                               | playwright/tests/storage-box-crud-critical.spec.ts     |
| LEG-CYP-036 | P1   | storage  | LEGACY_ONLY | cypress/e2e/storageDashboard.cy.js                             | playwright/tests/storage-dashboard.spec.ts             |
| LEG-CYP-037 | P1   | storage  | LEGACY_ONLY | cypress/e2e/storageDashboardMetrics.cy.js                      | playwright/tests/storage-dashboard-metrics.spec.ts     |
| LEG-CYP-038 | P1   | storage  | LEGACY_ONLY | cypress/e2e/storageDisposal.cy.js                              | playwright/tests/storage-disposal.spec.ts              |
| LEG-CYP-039 | P1   | storage  | LEGACY_ONLY | cypress/e2e/storageFilters.cy.js                               | playwright/tests/storage-filters.spec.ts               |
| LEG-CYP-040 | P0   | storage  | PARTIAL     | cypress/e2e/storageLocationCRUD-integration.cy.js              | playwright/tests/storage-view-edit-critical.spec.ts    |
| LEG-CYP-041 | P1   | storage  | LEGACY_ONLY | cypress/e2e/storageLocationCRUD-smoke.cy.js                    | playwright/tests/storage-location-smoke.spec.ts        |
| LEG-CYP-042 | P0   | storage  | PARTIAL     | cypress/e2e/storageLocationCRUD.cy.js                          | playwright/tests/storage-view-edit-critical.spec.ts    |
| LEG-CYP-043 | P1   | storage  | LEGACY_ONLY | cypress/e2e/storageLocationExpandableRows.cy.js                | playwright/tests/storage-location-expandable.spec.ts   |
| LEG-CYP-045 | P0   | storage  | LEGACY_ONLY | cypress/e2e/storageSearch.cy.js                                | playwright/tests/storage-search.spec.ts                |
| LEG-CYP-046 | P0   | storage  | PARTIAL     | cypress/e2e/storageViewStorage.cy.js                           | playwright/tests/storage-view-edit-critical.spec.ts    |
| LEG-CYP-047 | P0   | clinical | PARTIAL     | cypress/e2e/validation.cy.js                                   | playwright/tests/clinical-result-validation.spec.ts    |
| LEG-CYP-048 | P0   | clinical | PARTIAL     | cypress/e2e/workplan.cy.js                                     | playwright/tests/clinical-report-workplan.spec.ts      |
