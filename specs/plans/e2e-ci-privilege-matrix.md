# E2E CI Privilege Matrix (Phase 1)

This matrix inventories the current trust boundaries for E2E CI before hardening
changes.

Scope:

- `.github/workflows/e2e-playwright.yml`
- `.github/workflows/e2e-tests.yml`
- `.github/workflows/e2e-playwright-analyzer-harness-reusable.yml`
- `.github/workflows/e2e-playwright-analyzer-harness-manual.yml`
- `.github/workflows/e2e-cypress-deprecated.yml`

## 1) Workflow-Level Privilege Baseline

| Workflow                                         | Trigger                                                | Workflow-level permissions                                              | Notable trust implications                                                            |
| ------------------------------------------------ | ------------------------------------------------------ | ----------------------------------------------------------------------- | ------------------------------------------------------------------------------------- |
| `03 - E2E` (`e2e-playwright.yml`)                | `pull_request`, `push`, `release`, `workflow_dispatch` | `contents: read`, `packages: write`                                     | PR lane can attempt GHCR push (forks fail naturally due to token scope).              |
| `E2E / Tests and Publish` (`e2e-tests.yml`)      | `workflow_run` on `03 - E2E`                           | `contents: read`, `packages: write`, `statuses: write`, `actions: read` | Broad write scope shared by setup, tests, publish, and reporter jobs.                 |
| `E2E / Playwright / Analyzer Harness (Reusable)` | `workflow_call`                                        | `contents: read`, `packages: read`, `actions: read`                     | Good read-only baseline, but still consumes secret-backed `TEST_PASS`.                |
| `E2E / Playwright / Analyzer Harness (Manual)`   | `workflow_dispatch`                                    | inherited from called workflow                                          | Uses `secrets: inherit` at wrapper entrypoint.                                        |
| `E2E / Cypress (Deprecated)`                     | `workflow_call`, `workflow_dispatch`                   | `contents: read`, `packages: read`, `actions: read`                     | Read-only package usage, but still references secret-backed fallback for `TEST_PASS`. |

## 2) Job-Level Privilege Matrix

Legend:

- PR code checkout: checks out branch/merge ref controlled by PR author.
- PR code execution: runs build/test tooling against that checked-out PR code.
- GHCR write: pushes package versions to `ghcr.io/.../e2e-cache/...`.
- Status write: posts `03 Checkpoint - E2E` commit status.

| Workflow                                       | Job                                | Effective permission need (current behavior)        | PR code checkout                                      | PR code execution                        | GHCR write              | Status write | Secrets / vars flow                                                                                                         | `secrets: inherit` | `environment: e2e`                                                    | `persist-credentials: false` |
| ---------------------------------------------- | ---------------------------------- | --------------------------------------------------- | ----------------------------------------------------- | ---------------------------------------- | ----------------------- | ------------ | --------------------------------------------------------------------------------------------------------------------------- | ------------------ | --------------------------------------------------------------------- | ---------------------------- | --- | --- |
| `e2e-playwright.yml`                           | `shared-build`                     | `contents: read`, `packages: write`                 | Yes (`actions/checkout`)                              | Yes (Maven, npm, Docker build/push)      | Yes                     | No           | `secrets.GITHUB_TOKEN` for GHCR login                                                                                       | No                 | Yes                                                                   | No                           |
| `e2e-tests.yml`                                | `setup`                            | `actions: read`, `contents: read`                   | No                                                    | No                                       | No                      | No           | `secrets.GITHUB_TOKEN` for cross-run artifact read                                                                          | No                 | No                                                                    | N/A                          |
| `e2e-tests.yml`                                | `set-pending-status`               | `statuses: write`                                   | No                                                    | No                                       | No                      | Yes          | GITHUB token via `github-script`                                                                                            | No                 | No                                                                    | N/A                          |
| `e2e-tests.yml`                                | `fork-rebuild`                     | `packages: write`, `contents: read`                 | Yes (`refs/pull/<n>/merge`)                           | Yes (Maven, npm, Docker build/push)      | Yes                     | No           | `secrets.GITHUB_TOKEN` for GHCR login                                                                                       | No                 | Yes                                                                   | No                           |
| `e2e-tests.yml`                                | `playwright-core`                  | `packages: read`, `actions: read`, `contents: read` | Yes (merge ref for forks, workflow_run SHA otherwise) | Yes (Playwright + compose runtime)       | No                      | No           | `TEST_USER` from vars fallback; `TEST_PASS` from `secrets.TEST_PASS`; `secrets.GITHUB_TOKEN` for artifact read + GHCR login | No                 | Yes                                                                   | No                           |
| `e2e-tests.yml`                                | `analyzer-harness` (reusable call) | Should be read-only test execution                  | Indirect (inside called workflow)                     | Indirect                                 | No (in called workflow) | No           | Inputs passed + full secret inheritance                                                                                     | Yes                | Indirect                                                              | Indirect/no                  |
| `e2e-tests.yml`                                | `cypress` (reusable call)          | Should be read-only test execution                  | Indirect (inside called workflow)                     | Indirect                                 | No (in called workflow) | No           | Inputs passed + full secret inheritance                                                                                     | Yes                | Indirect                                                              | Indirect/no                  |
| `e2e-tests.yml`                                | `e2e-gate`                         | none beyond default token                           | No                                                    | No (only evaluates upstream results)     | No                      | No           | none                                                                                                                        | No                 | No                                                                    | N/A                          |
| `e2e-tests.yml`                                | `publish-images`                   | `packages: read`, DockerHub creds, `actions: read`  | No                                                    | No (retag/push only)                     | No write (reads GHCR)   | No           | `vars.DOCKERHUB_USERNAME`, `secrets.DOCKERHUB_TOKEN`, `secrets.GITHUB_TOKEN`                                                | No                 | Yes                                                                   | N/A                          |
| `e2e-tests.yml`                                | `report-status`                    | `statuses: write`                                   | No                                                    | No                                       | No                      | Yes          | GITHUB token via `github-script`                                                                                            | No                 | No                                                                    | N/A                          |
| `e2e-playwright-analyzer-harness-reusable.yml` | `demo-shards`                      | `packages: read`, `actions: read`, `contents: read` | Yes (`inputs.head_sha` / `github.sha`)                | Yes (Playwright harness stack + seeding) | No                      | No           | `TEST_USER` vars fallback; `TEST_PASS` from `secrets.TEST_PASS`; `secrets.GITHUB_TOKEN` for artifact read + GHCR login      | No                 | Yes                                                                   | No                           |
| `e2e-playwright-analyzer-harness-reusable.yml` | `merge-reports`                    | `actions: read`, `contents: read`                   | Yes (`actions/checkout`)                              | Yes (report merge tooling)               | No                      | No           | none explicit                                                                                                               | No                 | No                                                                    | No                           |
| `e2e-playwright-analyzer-harness-reusable.yml` | `analyzer-e2e-gate`                | none beyond default token                           | No                                                    | No (result checks only)                  | No                      | No           | none                                                                                                                        | No                 | No                                                                    | N/A                          |
| `e2e-playwright-analyzer-harness-manual.yml`   | `run`                              | delegated to called workflow                        | Delegated                                             | Delegated                                | Delegated               | No           | full inherited secret scope to called workflow                                                                              | Yes                | Delegated                                                             | Delegated/no                 |
| `e2e-cypress-deprecated.yml`                   | `e2e-cypress`                      | `packages: read`, `actions: read`, `contents: read` | Yes (`inputs.head_sha` / `github.sha`)                | Yes (Cypress + compose runtime)          | No                      | No           | `TEST_USER` vars fallback; `TEST_PASS` from `secrets.TEST_PASS                                                              |                    | 'adminADMIN!'`; `secrets.GITHUB_TOKEN` for artifact read + GHCR login | No                           | No  | No  |

## 3) Explicit PR-Controlled Code Execution Points

The following jobs currently execute PR-controlled code with nontrivial token
capability:

1. `e2e-playwright.yml` / `shared-build`
   - Runs in `pull_request` context with `packages: write`.
   - For fork PRs, GHCR push fails by token scope, but build/test commands still
     run.
2. `e2e-tests.yml` / `fork-rebuild`
   - Runs in privileged `workflow_run` context, checks out
     `refs/pull/<n>/merge`.
   - Rebuilds and pushes GHCR cache images using write-capable token.
3. Test executor lanes (Playwright core, analyzer harness reusable, Cypress)
   - Check out PR-controlled refs and run test commands.
   - Should be read-only consumers but currently mixed with secret usage and
     broad inheritance at callers.

## 4) Status Publication Boundary

Current status writers in `e2e-tests.yml`:

- `set-pending-status` posts pending `03 Checkpoint - E2E`.
- `report-status` posts final `03 Checkpoint - E2E`.

Both are appropriately tiny in behavior (no checkout/build/tests), but workflow
top-level permissions currently grant `statuses: write` to all jobs in the file.

## 5) GHCR Package Transfer Boundary

Current GHCR namespace usage:

- Shared build and fork-rebuild publish images under:
  `ghcr.io/<owner>/openelis-global-2/e2e-cache/<normalized-image>:sha-<...>`
- Test executors pull and retag from `e2e-image-map` artifacts.
- `publish-images` consumes tested images and republishes to DockerHub in the
  same workflow file that also handles PR test orchestration.

Risk currently is not namespace naming, but mixed responsibilities and shared
workflow-level write permissions in `e2e-tests.yml`.

## 6) Secret and Variable Flow Inventory

Current references:

- `secrets.TEST_PASS` used by:
  - `e2e-tests.yml` `playwright-core`
  - `e2e-playwright-analyzer-harness-reusable.yml` (`demo-shards` envs)
  - `e2e-cypress-deprecated.yml`
    (`TEST_PASS: ${{ secrets.TEST_PASS || 'adminADMIN!' }}`)
- `vars.TEST_USER || 'admin'` used across Playwright/Cypress lanes.
- `secrets: inherit` used by:
  - `e2e-tests.yml` reusable calls to analyzer harness and Cypress
  - `e2e-playwright-analyzer-harness-manual.yml` wrapper
- DockerHub publish currently uses:
  - `vars.DOCKERHUB_USERNAME`
  - `secrets.DOCKERHUB_TOKEN`
  - job is bound to `environment: e2e` (to be replaced by `publish` env).

## 7) Fork vs Non-Fork Execution Graphs (Current)

### Same-repo PR

1. `03 - E2E` / `shared-build` checks out PR code, builds, pushes GHCR cache.
2. `E2E / Tests and Publish` triggered by `workflow_run`.
3. `setup` parses build context.
4. `set-pending-status` posts pending checkpoint.
5. `fork-rebuild` skipped.
6. `playwright-core`, `analyzer-harness`, `cypress` run from GHCR cache.
7. `e2e-gate` evaluates results.
8. `report-status` posts final checkpoint.
9. `publish-images` skipped (event is PR, not push/release).

### Fork PR

1. `03 - E2E` / `shared-build` checks out PR code, GHCR push denied, emits
   `image_transfer=fork-fallback`.
2. `E2E / Tests and Publish` triggered by `workflow_run`.
3. `setup` parses build context.
4. `set-pending-status` posts pending checkpoint.
5. `fork-rebuild` checks out `refs/pull/<n>/merge`, rebuilds, pushes GHCR cache.
6. `playwright-core`, `analyzer-harness`, `cypress` consume fork-rebuild
   artifacts.
7. `e2e-gate` evaluates results.
8. `report-status` posts final checkpoint.
9. `publish-images` skipped (event is PR, not push/release).

## 8) Findings Summary Before Hardening

1. `e2e-tests.yml` is overprivileged at workflow scope (`packages: write` +
   `statuses: write`) relative to per-job intent.
2. `secrets: inherit` remains at reusable workflow call boundaries.
3. `secrets.TEST_PASS` remains in multiple executor workflows.
4. `environment: e2e` is still present in build, test, fork-rebuild, and
   publish.
5. `persist-credentials: false` is not set on current checkout steps.
6. The primary trust-boundary exception is `fork-rebuild` executing
   PR-controlled code in privileged `workflow_run` context.

This file serves as the baseline evidence for Phases 2-6 hardening work.
