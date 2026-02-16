# M8a/M9 Signoff Summary

**Feature**: `201-e2e-playwright-risk-parity`

## M8a Full Parity Side-by-Side Gate

### Cutoff Freeze (T129)

- Cutoff artifact: `artifacts/cutoff-scope.json`
- Gate check artifact: `artifacts/m8a-gate-check.md`
- Scope policy: current non-skipped Cypress coverage from active Cypress specs
  in inventory snapshot.

### Side-by-Side Run Evidence (T130/T134)

- Cypress workflow run ID: `22044932029`
  - https://github.com/DIGI-UW/OpenELIS-Global-2/actions/runs/22044932029
- Playwright workflow run ID: `22044932041`
  - https://github.com/DIGI-UW/OpenELIS-Global-2/actions/runs/22044932041
- Parity report artifact: `artifacts/parity-report.md`
- Runtime metrics artifact: `artifacts/runtime-metrics.json`
- M8a gate artifact: `artifacts/m8a-gate-check.md`
- Artifact pull status: `PASS`
  - `scripts/e2e/generate-m8a-evidence.sh` now downloads normalized artifacts
    directly by artifact name.

### Gate Status (T133)

- Blocking statuses in cutoff scope: `LEGACY_ONLY`, `GAP`, `PARTIAL`
- Current gate result: `PASS`
  - Scoped scenarios: `47`
  - Blocking rows: `0`
  - Scoped status counts: `PASS=47`
  - Evidence source: `artifacts/cutoff-scope.json`,
    `artifacts/m8a-gate-check.md`

### Notes

- Cutoff scope is currently frozen for commit
  `4be0be9a91f847c6a0e0a19bbd4c8b8e66e5f756`.
- M8a full-parity gate criteria are satisfied for cutoff non-skipped Cypress
  scope.

## M9 Migration Closure and Sunset Recommendation

_To be completed in M9._

## Branch Protection Evidence (T126)

- Attempted command:
  - `gh api repos/DIGI-UW/OpenELIS-Global-2/branches/develop/protection`
- Current result: `HTTP 403 Resource not accessible by integration`
- Impact: Required-check policy verification is currently blocked in this cloud
  environment and must be verified via a token/context with branch protection
  read access.
