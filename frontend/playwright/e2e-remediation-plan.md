# E2E Remediation and Migration Plan (Cypress -> Playwright)

## Goals

1. Reduce flaky CI failures caused by data-coupled Cypress flows.
2. Preserve coverage through explicit parity tracking during migration.
3. Make quarantine decisions auditable and reversible.

## Strategy

### 1) Policy-driven quarantine (implemented)

- Source of truth: `frontend/cypress/remediation-policy.json`
- Branch-specific exclusions are declared as structured entries:
  - `migrated_to_playwright`
  - `quarantined_pending_migration`
- Each migrated spec declares:
  - replacement Playwright spec
  - minimum required Playwright test count
  - parity level (`smoke` vs future `full`)

### 2) Automated parity analysis (implemented)

- Tool: `frontend/scripts/e2e-remediation-policy.mjs`
- Commands:
  - `filter` -> removes quarantined Cypress specs from a shard list
  - `parity` -> validates migrated entries have replacement coverage
- CI now runs parity in static checks before E2E execution.

### 3) Migration execution (implemented in this branch)

New Playwright smoke coverage:

- `playwright/tests/workplan-smoke.spec.ts`
- `playwright/tests/result-smoke.spec.ts`
- `playwright/tests/nonconform-smoke.spec.ts`

Helper added:

- `playwright/fixtures/menu-navigation.ts`

## Current Parity Scope

- Cypress `workplan.cy.js` -> Playwright `workplan-smoke.spec.ts` (smoke parity)
- Cypress `result.cy.js` -> Playwright `result-smoke.spec.ts` (smoke parity)
- Cypress `nonConform.cy.js` -> Playwright `nonconform-smoke.spec.ts` (smoke parity)

## Remaining Quarantine Backlog

The following specs remain quarantined and require dedicated migration work:

- `AdminE2E/userManagement.cy.js`
- `AdminE2E/organizationManagement.cy.js`
- `siteBranding.cy.js`
- `AdminE2E/generalConfigurations.cy.js`
- `orderEntity.cy.js`
- `patientEntry.cy.js`
- `storageSearch.cy.js`

## Next Iteration (recommended)

1. Migrate the seven remaining quarantined specs into Playwright smoke suites.
2. Upgrade smoke parity to behavior parity for high-risk domains:
   - admin configuration writes
   - patient/order flows
3. Remove branch-specific quarantine entries as replacements become stable.
