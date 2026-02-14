# Quickstart: Playwright Risk-Parity Migration

This guide captures local/CI commands and milestone gate evidence for Feature
201.

## M3 Foundation Commands

Run from repository root:

```bash
cd frontend
npm ci
npx playwright install chromium --with-deps
npm run pw:test
```

If Docker-backed local environment is needed:

```bash
cp .env.example .env
docker compose -f build.docker-compose.yml up -d --wait --wait-timeout 600
```

## Shared Foundation Conventions (M3)

- Shared fixture helper: `frontend/playwright/fixtures/e2e-base.ts`
- Parity metadata helper: `frontend/playwright/fixtures/parity-metadata.ts`
- Harness smoke spec: `frontend/playwright/tests/harness-smoke.spec.ts`
- Reporter outputs:
  - JSON: `frontend/playwright-report/results.json`
  - HTML: `frontend/playwright-report/`
  - Raw test outputs: `frontend/test-results/playwright/`

## M3 Baseline Test Gate Result (T046)

- Command: `cd frontend && npm run pw:test`
- Execution date: `2026-02-14`
- Status: `BLOCKED_IN_ENV`
- Observed output summary:
  - First run failed due missing browser binary (`Executable doesn't exist ...`).
  - Installed browser with `npm --prefix frontend exec playwright install chromium`.
  - Second run reached test execution, then failed in setup with
    `net::ERR_CONNECTION_REFUSED at https://localhost/` because OpenELIS app was
    not reachable in this cloud environment.
- Artifact path from failed run:
  - `frontend/test-results/playwright/auth.setup.ts-authenticate-setup/test-failed-1.png`

## M4a Auth/Nav Spot-Check Gate Result (T066)

- Playwright command:
  - `cd frontend && TEST_USER=admin TEST_PASS=adminADMIN! npm run pw:test -- --grep "Auth login parity migration|Home and navbar navigation parity migration|Dashboard critical navigation smoke"`
- Cypress counterpart command:
  - `cd frontend && TEST_USER=admin TEST_PASS=adminADMIN! xvfb-run -a npm run cy:failfast:spec "cypress/e2e/login.cy.js,cypress/e2e/home.cy.js,cypress/e2e/dashboard.cy.js"`
- Execution date: `2026-02-14`
- Status: `BLOCKED_IN_ENV`
- Observed outcome:
  - Playwright setup project failed first with `net::ERR_CONNECTION_REFUSED at https://localhost/`.
  - Cypress launched successfully under xvfb, then failed baseUrl verification because
    `https://localhost` was not running.

## M4b Admin-Core Spot-Check Gate Result (T075)

- Playwright command:
  - `cd frontend && TEST_USER=admin TEST_PASS=adminADMIN! npm run pw:test -- --grep "Admin user management parity migration|Admin organization and provider parity migration|Admin barcode configuration parity migration"`
- Cypress counterpart command:
  - `cd frontend && TEST_USER=admin TEST_PASS=adminADMIN! xvfb-run -a npm run cy:failfast:spec "cypress/e2e/AdminE2E/userManagement.cy.js,cypress/e2e/AdminE2E/organizationManagement.cy.js,cypress/e2e/AdminE2E/providerManagement.cy.js,cypress/e2e/AdminE2E/barcode.cy.js"`
- Execution date: `2026-02-14`
- Status: `BLOCKED_IN_ENV`
- Observed outcome:
  - Playwright setup project failed first with `net::ERR_CONNECTION_REFUSED at https://localhost/`.
  - Cypress launched successfully under xvfb, then failed baseUrl verification because
    `https://localhost` was not running.

## M5 Clinical Spot-Check Gate Result (T086)

- Playwright command:
  - `cd frontend && TEST_USER=admin TEST_PASS=adminADMIN! npm run pw:test -- --grep "Clinical patient and order parity migration|Clinical result and validation parity migration|Clinical report and workplan parity migration|Clinical non-conformity parity migration"`
- Cypress counterpart command:
  - `cd frontend && TEST_USER=admin TEST_PASS=adminADMIN! xvfb-run -a npm run cy:failfast:spec "cypress/e2e/batchOrderEntry.cy.js,cypress/e2e/modifyOrder.cy.js,cypress/e2e/nonConform.cy.js,cypress/e2e/orderEntity.cy.js,cypress/e2e/patientEntry.cy.js,cypress/e2e/report.cy.js,cypress/e2e/result.cy.js,cypress/e2e/validation.cy.js,cypress/e2e/workplan.cy.js"`
- Execution date: `2026-02-14`
- Status: `BLOCKED_IN_ENV`
- Observed outcome:
  - Playwright setup project failed first with `net::ERR_CONNECTION_REFUSED at https://localhost/`.
  - Cypress launched successfully under xvfb, then failed baseUrl verification because
    `https://localhost` was not running.
