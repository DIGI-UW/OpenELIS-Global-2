# Analyzer Bidirectional E2E – QA & Constitution Gate

Run this checklist before considering the analyzer bidirectional E2E slice done.

## 1. Fixture and harness alignment

- [ ] **Stable lane**: Fixture-backed tests use analyzer ID from
      `GENEXPERT_FIXTURE_ID` (2013) in
      `frontend/playwright/fixtures/analyzer-constants.ts`.
- [ ] **Source of truth**: Minimal fixture is
      `src/test/resources/analyzer-minimal.sql`; do not rely on other fixture
      sources for harness-backed specs.
- [ ] **Promotion gate**: New-analyzer spec uses `HARNESS_MOCK_HOST` /
      `HARNESS_MOCK_PORT` (172.20.1.100:9600) so the created analyzer points at
      the same mock as the fixture.

## 2. Harness preflight

- [ ] **Preflight helper**: `frontend/playwright/utils/harness-preflight.ts`
      provides `isMockSimulateAvailable()` and `triggerMockResultsPush()`.
- [ ] **Usage**: Fixture-backed and promotion-gate specs call preflight in
      `beforeEach` and skip with a clear message when the mock is unreachable
      (e.g. `MOCK_SIMULATE_URL` default `http://localhost:8085`).
- [ ] **No brittle waits**: Specs avoid arbitrary `waitForTimeout()`; they use
      web-first assertions and preflight instead.

## 3. Targeted Playwright runs

- [ ] **Dashboard actions** (no harness required):  
       `npx playwright test analyzer-bidi-dashboard-actions`
- [ ] **Fixture-backed bidi** (harness required):  
       `npx playwright test analyzer-bidi-fixture`
- [ ] **Promotion gate** (harness required):  
       `npx playwright test analyzer-bidi-promotion-gate`
- [ ] **ASTM matrix** (harness + multi-port mock, load
      `--analyzers=astm-full`):  
       `npx playwright test analyzer-bidi-matrix`
- [ ] **Full analyzer set** (optional, after focused slices pass):  
       `npx playwright test tests/analyzer-`

## 3a. Multi-analyzer (ASTM full) validation

- [ ] **Harness**: Single mock with `config/port_templates.json`; ports
      9600–9604 map to genexpert_astm, mindray_ba88a, stago_start4,
      horiba_micros60, horiba_pentra60.
- [ ] **Fixtures**: Load with
      `./src/test/resources/load-test-fixtures.sh --analyzers=astm-full`
      (minimal + analyzer-astm-full.sql; IDs 2013–2017).
- [ ] **Matrix spec**: `analyzer-bidi-matrix.spec.ts` runs Test Connection for
      each `ASTM_FIXTURE_IDS`; skip when harness or mock is unavailable.

## 4. Trace and debug expectations

- [ ] On failure, run with traces:  
       `npx playwright test analyzer-bidi-fixture --trace=on`
- [ ] Review browser console logs and screenshots for failed runs.
- [ ] Confirm real-effect assertions: at least one spec verifies visible outcome
      in the Analyzer Results UI after a mock push or after Send Order / Query
      Results.

## 5. Constitution and testing roadmap

- [ ] **Carbon only**: New UI uses Carbon components (e.g. ComposedModal,
      TextInput, Tag); no Bootstrap/Tailwind.
- [ ] **React Intl**: All new user-facing strings use message IDs in `en.json` /
      `fr.json` (e.g. `analyzer.action.sendOrder`,
      `analyzer.action.queryResults`, modal keys).
- [ ] **TDD**: Implementation was done in small chunks with tests first where
      applicable.
- [ ] **E2E**: At least one test asserts a real effect in the UI (e.g. result
      row in Analyzer Results) after a mutation or import.
- [ ] **Naming**: Dashboard action "Query Results" is distinct from the
      field-discovery "Query Analyzer" on the mappings page; no user-facing
      duplication or confusion.

## 6. CI behavior

- [ ] Specs that require the analyzer list with fixture data or the harness skip
      when `process.env.CI === "true"` so default CI does not fail for missing
      harness/fixtures.
- [ ] Document in PR or README that full analyzer E2E (including bidi) requires
      starting the analyzer harness and loading the minimal analyzer fixture for
      local or dedicated CI runs.
- [ ] Any Playwright run (including bidi) requires `TEST_USER` and `TEST_PASS`
      for auth setup; bidi specs then skip inside the run when `CI === "true"`.
