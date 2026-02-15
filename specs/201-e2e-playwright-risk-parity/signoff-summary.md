# M8a/M9 Signoff Summary

**Feature**: `201-e2e-playwright-risk-parity`

## M8a Full Parity Side-by-Side Gate

### Cutoff Freeze (T129)

- Cutoff artifact: `artifacts/cutoff-scope.json`
- Gate check artifact: `artifacts/m8a-gate-check.md`
- Scope policy: current non-skipped Cypress coverage from active Cypress specs
  in inventory snapshot.

### Side-by-Side Run Evidence (T130/T134)

- Cypress workflow run ID: `22042226792`
  - https://github.com/DIGI-UW/OpenELIS-Global-2/actions/runs/22042226792
- Playwright workflow run ID: `22042226797`
  - https://github.com/DIGI-UW/OpenELIS-Global-2/actions/runs/22042226797
- Parity report artifact: `artifacts/parity-report.md`
- Runtime metrics artifact: `artifacts/runtime-metrics.json`
- M8a gate artifact: `artifacts/m8a-gate-check.md`

### Gate Status (T133)

- Blocking statuses in cutoff scope: `LEGACY_ONLY`, `GAP`, `PARTIAL`
- Current gate result: `FAIL`
  - Scoped scenarios: `47`
  - Blocking rows: `47`
  - Blocking breakdown: `LEGACY_ONLY=24`, `PARTIAL=22`, `GAP=1`
  - Evidence source: `artifacts/cutoff-scope.json`,
    `artifacts/m8a-gate-check.md`

### Notes

- Cutoff scope was frozen for commit `01f70c7758cc7ecf1bc15348990a8845f2504c9b`.
- Full side-by-side parity closure remains blocked until all cutoff-scope rows
  are `PASS` (no `LEGACY_ONLY`, `GAP`, `PARTIAL`).

## M9 Migration Closure and Sunset Recommendation

_To be completed in M9._
