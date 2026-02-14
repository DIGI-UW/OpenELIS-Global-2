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
