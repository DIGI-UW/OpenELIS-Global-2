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
- Status: `PENDING` (to be updated after M3 gate execution)
