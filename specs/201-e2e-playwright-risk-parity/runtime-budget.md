# Runtime Budget Contract (M7)

**Feature**: `201-e2e-playwright-risk-parity`  
**Milestone**: `M7`  
**Status**: Drafted for CI enforcement

## Purpose

Define explicit runtime thresholds used by parity reporting so dual-run
execution remains within acceptable CI bounds.

## Budget Thresholds (Wall-Clock)

| Framework  | Budget (minutes) | Budget (ms) | Notes                               |
| ---------- | ---------------: | ----------: | ----------------------------------- |
| Playwright |              35m |   2,100,000 | Sharded run with merged reports     |
| Cypress    |              55m |   3,300,000 | Includes static checks + E2E shards |

## Compliance Rule

- A run is **in-budget** when measured duration is less than or equal to the
  framework budget.
- Stabilization acceptance target (M9): at least **90%** of sampled dual-run CI
  executions are in-budget.

## Data Source

- Runtime metrics are exported by `scripts/e2e/export-runtime-metrics.js` from
  normalized framework artifacts.
- Budget status is surfaced in:
  - `specs/201-e2e-playwright-risk-parity/artifacts/runtime-metrics.json`
  - `specs/201-e2e-playwright-risk-parity/artifacts/parity-report.md`
