# M8 Cutover Checklist (Playwright Primary, Cypress Retained)

**Feature**: `201-e2e-playwright-risk-parity`  
**Milestone**: `M8`  
**Status**: Draft (execution gated by M8a parity pass)

## Preconditions (Must Be True Before Cutover)

- [ ] M8a gate is `PASS` (no `LEGACY_ONLY`, `GAP`, `PARTIAL` in cutoff scope)
- [ ] `artifacts/parity-report.md` and `artifacts/runtime-metrics.json` are
      published for the cutover candidate SHA
- [ ] Stakeholder acknowledgment recorded in signoff summary

## Primary vs Comparison Checks

- **Primary E2E check**: `Run Playwright E2E Tests`
  - Source workflow: `.github/workflows/playwright-e2e.yml`
  - Fan-in job name: `Run Playwright E2E Tests`
- **Comparison E2E check**: `build-and-run-qa-tests`
  - Source workflow: `.github/workflows/frontend-qa.yml`
  - Fan-in job name: `build-and-run-qa-tests`

## Cutover Execution Steps

1. Confirm preconditions for target SHA.
2. Verify Playwright fan-in check is configured as required primary gate.
3. Verify Cypress fan-in check remains active as comparison signal.
4. Confirm merged Playwright report artifact upload is present.
5. Confirm Cypress screenshot artifacts are still uploaded for diagnostics.

## Rollback Criteria

Rollback from primary cutover decision if any of the following occurs:

- Playwright primary check fails on baseline branch after cutover enablement.
- Cross-framework parity divergence spikes beyond accepted stabilization budget.
- Runtime budget violations exceed threshold for consecutive runs.

## Rollback Actions

1. Revert/check update that made Playwright primary.
2. Restore previous required-check policy.
3. Re-run both workflows and re-evaluate from latest parity artifact set.
