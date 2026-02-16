# Quickstart: Playwright Risk-Parity Migration

This guide captures local/CI commands and milestone gate evidence for
Feature 201.

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
  - First run failed due missing browser binary
    (`Executable doesn't exist ...`).
  - Installed browser with
    `npm --prefix frontend exec playwright install chromium`.
  - Second run reached test execution, then failed in setup with
    `net::ERR_CONNECTION_REFUSED at https://localhost/` because OpenELIS app was
    not reachable in this cloud environment.
- Artifact path from failed run:
  - `frontend/test-results/playwright/auth.setup.ts-authenticate-setup/test-failed-1.png`

## M3 Fixture Strategy Gate Result (T052)

- Selected strategy: `hybrid`
  - Read/non-mutating: `verify-reuse`
  - Mutating: `reset-load-verify`
- Implementation touchpoints:
  - `frontend/cypress.config.js`
  - `frontend/cypress/support/storage-setup.js`
  - `frontend/playwright/fixtures/storage-fixtures.ts`
  - `frontend/playwright/fixtures/e2e-base.ts`
- Representative commands attempted:
  - Playwright mutating-flow storage fixture command:
    `cd frontend && TEST_USER=admin TEST_PASS=adminADMIN! PW_LOAD_STORAGE_FIXTURES=true PW_FIXTURE_STRATEGY_MODE=hybrid PW_FIXTURE_MUTATING_MODE=reset-load-verify npm run pw:test -- --grep "Storage box CRUD critical parity migration"`
  - Cypress storage fixture strategy command:
    `cd frontend && TEST_USER=admin TEST_PASS=adminADMIN! CYPRESS_FIXTURE_STRATEGY=hybrid CYPRESS_FIXTURE_MUTATING_MODE=reset-load-verify xvfb-run -a npm run cy:failfast:spec "cypress/e2e/storageBoxCRUD.cy.js"`
- Execution date: `2026-02-15`
- Status: `BLOCKED_IN_ENV`
- Observed outcome:
  - Playwright setup project failed first with
    `net::ERR_CONNECTION_REFUSED at https://localhost/`.
  - Cypress launched under xvfb but failed baseUrl verification because
    `https://localhost` was not reachable.

## M3 Auth Strategy Gate Result (T055)

- Auth strategy contract:
  - Read/non-mutating default: `shared-session`
  - Mutating default: `worker-isolated` (worker credential override supported)
- Representative commands attempted:
  - Read-flow command:
    `cd frontend && TEST_USER=admin TEST_PASS=adminADMIN! PW_AUTH_STRATEGY_MODE=hybrid PW_AUTH_READ_MODE=shared-session npm run pw:test -- --grep "Home and navbar navigation parity migration"`
  - Mutating-flow command:
    `cd frontend && TEST_USER=admin TEST_PASS=adminADMIN! PW_AUTH_STRATEGY_MODE=hybrid PW_AUTH_MUTATING_MODE=worker-isolated npm run pw:test -- --grep "Clinical patient and order parity migration"`
- Execution date: `2026-02-15`
- Status: `BLOCKED_IN_ENV`
- Observed outcome:
  - Both commands failed in setup with
    `net::ERR_CONNECTION_REFUSED at https://localhost/` before app-level auth
    behavior could be exercised.
  - Fixture/auth contract implementation is complete in code, but runtime
    validation requires reachable OpenELIS environment.

## M4a Auth/Nav Spot-Check Gate Result (T066)

- Playwright command:
  - `cd frontend && TEST_USER=admin TEST_PASS=adminADMIN! npm run pw:test -- --grep "Auth login parity migration|Home and navbar navigation parity migration|Dashboard critical navigation smoke"`
- Cypress counterpart command:
  - `cd frontend && TEST_USER=admin TEST_PASS=adminADMIN! xvfb-run -a npm run cy:failfast:spec "cypress/e2e/login.cy.js,cypress/e2e/home.cy.js,cypress/e2e/dashboard.cy.js"`
- Execution date: `2026-02-14`
- Status: `BLOCKED_IN_ENV`
- Observed outcome:
  - Playwright setup project failed first with
    `net::ERR_CONNECTION_REFUSED at https://localhost/`.
  - Cypress launched successfully under xvfb, then failed baseUrl verification
    because `https://localhost` was not running.

## M4b Admin-Core Spot-Check Gate Result (T075)

- Playwright command:
  - `cd frontend && TEST_USER=admin TEST_PASS=adminADMIN! npm run pw:test -- --grep "Admin user management parity migration|Admin organization and provider parity migration|Admin barcode configuration parity migration"`
- Cypress counterpart command:
  - `cd frontend && TEST_USER=admin TEST_PASS=adminADMIN! xvfb-run -a npm run cy:failfast:spec "cypress/e2e/AdminE2E/userManagement.cy.js,cypress/e2e/AdminE2E/organizationManagement.cy.js,cypress/e2e/AdminE2E/providerManagement.cy.js,cypress/e2e/AdminE2E/barcode.cy.js"`
- Execution date: `2026-02-14`
- Status: `BLOCKED_IN_ENV`
- Observed outcome:
  - Playwright setup project failed first with
    `net::ERR_CONNECTION_REFUSED at https://localhost/`.
  - Cypress launched successfully under xvfb, then failed baseUrl verification
    because `https://localhost` was not running.

## M5 Clinical Spot-Check Gate Result (T086)

- Playwright command:
  - `cd frontend && TEST_USER=admin TEST_PASS=adminADMIN! npm run pw:test -- --grep "Clinical patient and order parity migration|Clinical result and validation parity migration|Clinical report and workplan parity migration|Clinical non-conformity parity migration"`
- Cypress counterpart command:
  - `cd frontend && TEST_USER=admin TEST_PASS=adminADMIN! xvfb-run -a npm run cy:failfast:spec "cypress/e2e/batchOrderEntry.cy.js,cypress/e2e/modifyOrder.cy.js,cypress/e2e/nonConform.cy.js,cypress/e2e/orderEntity.cy.js,cypress/e2e/patientEntry.cy.js,cypress/e2e/report.cy.js,cypress/e2e/result.cy.js,cypress/e2e/validation.cy.js,cypress/e2e/workplan.cy.js"`
- Execution date: `2026-02-14`
- Status: `BLOCKED_IN_ENV`
- Observed outcome:
  - Playwright setup project failed first with
    `net::ERR_CONNECTION_REFUSED at https://localhost/`.
  - Cypress launched successfully under xvfb, then failed baseUrl verification
    because `https://localhost` was not running.

## M6 Storage Critical Gap Spot-Check Gate Result (T097)

- Playwright command:
  - `cd frontend && TEST_USER=admin TEST_PASS=adminADMIN! npm run pw:test -- --grep "Storage assignment critical parity migration|Storage box CRUD critical parity migration|Storage samples visibility parity migration|Storage view/edit critical parity migration"`
- Cypress counterpart command:
  - `cd frontend && TEST_USER=admin TEST_PASS=adminADMIN! xvfb-run -a npm run cy:failfast:spec "cypress/e2e/storageAssignment.cy.js,cypress/e2e/storageBoxCRUD-integration.cy.js,cypress/e2e/storageBoxCRUD.cy.js,cypress/e2e/storageLocationCRUD.cy.js,cypress/e2e/storageViewStorage.cy.js,cypress/e2e/storageSamplesTable.cy.js"`
- Execution date: `2026-02-14`
- Status: `BLOCKED_IN_ENV`
- Observed outcome:
  - Playwright setup project failed first with
    `net::ERR_CONNECTION_REFUSED at https://localhost/`.
  - Cypress launched successfully under xvfb, then failed baseUrl verification
    because `https://localhost` was not running.
- Gap register check:
  - `critical-gap-register.md` has **zero** `Open` entries targeting milestone
    `M6` after this update.

## M7 Dual-Run Parity CI Tooling (T101-T113)

Implemented artifacts and workflows:

- Scripts:
  - `scripts/e2e/export-playwright-results.js`
  - `scripts/e2e/export-cypress-results.js`
  - `scripts/e2e/compare-e2e-results.js`
  - `scripts/e2e/export-runtime-metrics.js`
- CI workflows:
  - `.github/workflows/playwright-e2e.yml` (normalized Playwright artifacts +
    blob merge)
  - `.github/workflows/frontend-qa.yml` (normalized Cypress artifacts)
  - `.github/workflows/e2e-parity-report.yml` (cross-workflow parity report
    generation)
- Contracts/docs:
  - `contracts/parity-report.schema.json`
  - `runtime-budget.md`
  - `artifacts/parity-report.md`

Local validation commands run:

```bash
node --check scripts/e2e/export-playwright-results.js
node --check scripts/e2e/export-cypress-results.js
node --check scripts/e2e/compare-e2e-results.js
node --check scripts/e2e/export-runtime-metrics.js
cd frontend && npm run pw:test -- --list
```

Status:

- Script syntax validation: `PASS`
- Playwright config/reporter validation (`--list`): `PASS`
- CI milestone gate T109: `IN_PROGRESS` (normalized exporter hardening applied;
  pending next CI run confirmation)

## M8a Cutoff Freeze + Gate Commands

```bash
# 1) Freeze cutoff scope from inventory + parity matrix + run IDs
node scripts/e2e/freeze-m8a-cutoff.js \
  --inventory specs/201-e2e-playwright-risk-parity/artifacts/inventory.json \
  --parity-matrix specs/201-e2e-playwright-risk-parity/parity-matrix.csv \
  --head-sha <sha> \
  --cypress-run-id <cypress_run_id> \
  --playwright-run-id <playwright_run_id> \
  --output specs/201-e2e-playwright-risk-parity/artifacts/cutoff-scope.json

# 2) Evaluate M8a gate (exit non-zero when blocking rows remain)
node scripts/e2e/check-m8a-parity-gate.js \
  --cutoff specs/201-e2e-playwright-risk-parity/artifacts/cutoff-scope.json \
  --output specs/201-e2e-playwright-risk-parity/artifacts/m8a-gate-check.md
```

### Latest M8a Freeze Attempt

- Execution date: `2026-02-15`
- Commit SHA: `4be0be9a91f847c6a0e0a19bbd4c8b8e66e5f756`
- Referenced run IDs:
  - Cypress: `22044932029`
  - Playwright: `22044932041`
- Output summary:
  - Scoped scenarios: `47`
  - Gate pass: `true`
  - Blocking rows: `0`
  - Blocking status counts: `PASS=47`

### One-command M8a Evidence Generation (CI artifacts)

```bash
bash scripts/e2e/generate-m8a-evidence.sh \
  --cypress-run-id <cypress_run_id> \
  --playwright-run-id <playwright_run_id> \
  --head-sha <sha>
```

Current result against run IDs `22044932029` + `22044932041`:

- Status: `PASS` for artifact download + cutoff gate evaluation
- Message: normalized artifacts download by name works; cutoff gate reports zero
  blocking rows.
- Interpretation: M8a cutoff-scope parity gate is currently passing. Remaining
  parity-report completeness depends on the next Playwright CI run using updated
  normalized-export fallback logic.

## M8 Cutover Orientation (Prework)

Primary vs comparison check alignment:

- Playwright primary check: `Run Playwright E2E Tests`
  (`.github/workflows/playwright-e2e.yml` fan-in job)
- Cypress comparison check: `build-and-run-qa-tests`
  (`.github/workflows/frontend-qa.yml` fan-in job)

Cutover checklist artifact:

- `specs/201-e2e-playwright-risk-parity/cutover-checklist.md`

Important: M8a parity pass is currently satisfied for cutoff scope.
