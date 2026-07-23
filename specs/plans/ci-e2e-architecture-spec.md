# E2E CI Architecture — Source of Truth

**Date:** 2026-04-04  
**Owner:** OpenELIS CI maintainers  
**Status:** Active reference

This document is the concise repository-level reference for how OpenELIS runs
PR-facing E2E validation across trusted and fork-originated contributions.

## 1. North Star

- Build once per path.
- Keep fork and non-fork paths operationally equivalent after image publication.
- Use GHCR as the canonical downstream image contract.
- Do not perform a second privileged fork rebuild.
- Keep `03 Checkpoint - E2E` as the single required PR-facing E2E status.

## 2. Workflow Topology

- Build workflow:
  [`../../.github/workflows/e2e-playwright.yml`](../../.github/workflows/e2e-playwright.yml)
  - Builds artifacts, plugins, and Docker images.
  - Publishes GHCR images directly for non-fork runs.
  - Exports a prebuilt image handoff payload for fork runs.
- Wrapper workflow:
  [`../../.github/workflows/e2e-tests.yml`](../../.github/workflows/e2e-tests.yml)
  - Triggered by `workflow_run` from `03 - E2E`.
  - Reads build context and transfer-state artifacts.
  - Publishes handed-off fork images to GHCR without rebuilding.
  - Calls the authoritative executor and owns checkpoint status updates.
- Authoritative executor:
  [`../../.github/workflows/e2e-authoritative-reusable.yml`](../../.github/workflows/e2e-authoritative-reusable.yml)
  - Runs the full E2E gate: Playwright core, Playwright harness, and deprecated
    Cypress.
- Post-merge publish flow:
  [`../../.github/workflows/publish-images.yml`](../../.github/workflows/publish-images.yml)
  - Retags and publishes release/develop images outside PR validation.

See the operator runbook for troubleshooting and expectations:
[`./e2e-ci-operator-model.md`](./e2e-ci-operator-model.md).

## 3. Execution Contract

### 3.1 Artifact contract

- `e2e-build-context-core`
  - Early metadata from `03 - E2E`.
  - Includes `event_name`, `is_fork`, `pr_number`, `pr_sha`, `head_sha`.
- `e2e-build-transfer-state`
  - Late-stage metadata describing image transfer mode.
  - Includes `image_transfer` and related event context.
- `e2e-image-map`, `e2e-image-map-base`
  - GHCR image references for downstream execution.
- `e2e-image-handoff`
  - Fork-only prebuilt image archive and source image lists.
- `e2e-plugin-jars`
  - Runtime plugin payload consumed by downstream tests.

### 3.2 Transfer modes

- `ghcr`
  - Build run already published images to GHCR.
- `fork-handoff`
  - Build run exported prebuilt images for privileged follow-up publication.
- `none`
  - Invalid or failed transfer state; downstream execution must not pretend
    success.

### 3.3 Path behavior

- Non-fork PRs
  - `03 - E2E` builds and publishes to GHCR.
  - `E2E / Tests` consumes GHCR image maps and runs the executor.
- Fork PRs
  - `03 - E2E` builds once and exports handoff payloads.
  - `E2E / Tests` publishes those exact images to GHCR and then runs the
    executor.

## 4. Trust Boundary Model

- `pull_request` build stage is untrusted execution.
  - Prefer read-only behavior.
  - Avoid non-essential writes and shared-cache blast radius.
- `workflow_run` follow-up stage is privileged execution.
  - Performs GHCR publication and status updates.
  - Must consume explicit artifacts rather than infer state from display
    metadata.

This design accepts controlled risk because fork PRs are a core contribution
path. Risk is reduced, not eliminated.

## 5. Status and Identity Rules

- `03 Checkpoint - E2E` is the only required PR-facing E2E status.
- The wrapper workflow owns pending and terminal status reporting.
- In `workflow_run`, two identities exist:
  - run identity: what GitHub shows in Actions UI for the triggered run
  - validation identity: the code ref and commit actually validated downstream
- For PR gating, the authoritative commit is the one identified by `status_sha`,
  not the run label shown in the Actions list.

Operational rule: when debugging parity or "wrong SHA" reports, trust the
commit/PR status surface plus artifact-derived `checkout_ref` and `status_sha`,
not the Actions list label by itself.

## 6. What This Architecture Fixes

- Removes the old duplicate privileged fork rebuild as a steady-state design.
- Converges fork and non-fork paths on the same downstream executor contract.
- Makes fork publication an image handoff problem, not a second build problem.

## 7. What Still Breaks Parity

Parity failures can still happen even with the correct CI topology:

- Non-deterministic E2E tests.
- Runtime timing sensitivity across containers, DB readiness, plugins, and
  browser startup.
- Comparing the wrong audit surface when diagnosing runs.

The main remaining parity risk is test/runtime instability, not `workflow_run`
itself.

## 8. Determinism Changes Landed With This Remediation

- `frontend/playwright/helpers/accept-results.ts`
  - Added full-load settling and a stable post-navigation check before polling.
- `frontend/playwright/helpers/create-analyzer-from-profile.ts`
  - Added bounded analyzer API readiness polling before save and after mock
    network creation.
  - Added safer mock cleanup to reduce teardown races.
- `frontend/playwright/tests/foundational/core/sidenav.spec.ts`
  - Changed refresh navigation waiting from `domcontentloaded` to `load`.

Policy decision: CI `retries` remains `0`; stability must come from
deterministic tests and helper behavior, not retry masking.

## 9. Decision Log

### Accepted

- Mutually exclusive fork and non-fork image publication paths.
- GHCR as the canonical downstream image contract.
- Fork image handoff plus privileged publish instead of second rebuild.
- Separate early build context from late transfer-state artifacts.

### Rejected

- Full privileged fork rebuild as normal behavior.
- Inferring fork/non-fork mode primarily from `workflow_run` display metadata.
- Fragile control flow that depends on artifact overwrite timing.

### Deferred

- Larger trigger-model redesign beyond this remediation.

## 10. References

### Repository references

- [`../../.github/workflows/e2e-playwright.yml`](../../.github/workflows/e2e-playwright.yml)
- [`../../.github/workflows/e2e-tests.yml`](../../.github/workflows/e2e-tests.yml)
- [`../../.github/workflows/e2e-authoritative-reusable.yml`](../../.github/workflows/e2e-authoritative-reusable.yml)
- [`../../.github/workflows/publish-images.yml`](../../.github/workflows/publish-images.yml)
- [`./e2e-ci-operator-model.md`](./e2e-ci-operator-model.md)

### Relevant PR history

- [#3298](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3298)
- [#3299](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3299)
- [#3301](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3301)
- [#3305](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3305)

### Platform references

- [GitHub workflow permissions model](https://docs.github.com/en/actions/writing-workflows/workflow-syntax-for-github-actions#how-permissions-are-calculated-for-a-workflow-job)
- [GitHub `workflow_run` event semantics](https://docs.github.com/en/actions/reference/events-that-trigger-workflows#workflow_run)
- [Docker BuildKit `type=gha` cache backend](https://docs.docker.com/build/cache/backends/gha/)
