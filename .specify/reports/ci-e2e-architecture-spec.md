# E2E CI Architecture — Source of Truth

**Date:** 2026-04-03  
**Owner:** OpenELIS CI maintainers  
**Status:** Active reference (validated against repository workflows and
GitHub/Docker platform docs)

---

## 1. Architectural North Star

- One image build per path (fork and non-fork are mutually exclusive build
  paths).
- Fork and non-fork paths converge into the same downstream E2E execution
  contract.
- GHCR is the canonical downstream image contract.
- A second full fork rebuild is not an acceptable steady-state design.
- If direct fork-path GHCR publish is restricted, prebuilt fork images are
  handed off and published later in privileged context without rebuilding.
- `03 Checkpoint - E2E` remains the single PR-facing E2E status contract.

---

## 2. Current Validated Architecture

### 2.1 Workflow Topology

- Build workflow:
  [.github/workflows/e2e-playwright.yml](.github/workflows/e2e-playwright.yml)
  - Builds artifacts, plugins, and Docker images.
  - Non-fork path publishes to GHCR in the build run.
  - Fork path exports prebuilt image handoff payload and metadata artifacts.
- Orchestrator workflow:
  [.github/workflows/e2e-tests.yml](.github/workflows/e2e-tests.yml)
  - Triggered by `workflow_run` completion of `03 - E2E`.
  - Reads early context and transfer-state artifacts from the build run.
  - For fork handoff mode, publishes handed-off images to GHCR without
    rebuilding.
  - Calls the authoritative executor and updates `03 Checkpoint - E2E`.
- Authoritative reusable:
  [.github/workflows/e2e-authoritative-reusable.yml](.github/workflows/e2e-authoritative-reusable.yml)
  - Runs Playwright core + harness and deprecated Cypress under a unified gate.
- Publish workflow:
  [.github/workflows/publish-images.yml](.github/workflows/publish-images.yml)
  - Post-merge/release retag and publish flow, filtered to `develop` and `v*`.

### 2.2 Contract Flow

1. `03 - E2E` builds once.
2. Non-fork:
   - publishes GHCR image map artifacts directly.
3. Fork:
   - exports image handoff archive (`docker save`) + source image lists.
4. `E2E / Tests` consumes context + transfer-state.
5. Fork handoff mode:
   - loads handed-off images, publishes to GHCR, emits same-run image maps.
6. Authoritative executor consumes image maps and runs all E2E suites.
7. Wrapper posts final `03 Checkpoint - E2E`.

---

## 3. One-Build-Per-Path Requirement

This is non-negotiable for the desired CI state:

- Non-fork PRs: single build in `03 - E2E`.
- Fork PRs: single build in `03 - E2E`, plus publish-from-handoff in
  `E2E / Tests`.
- Fork path must not run a second Maven + Docker image build in privileged
  context.

---

## 4. Trust Boundaries and Token Model

### 4.1 Accepted Trust Position

- Community fork PRs are a primary contribution channel; controlled trust is
  required for functional CI.
- The GitHub “Approve workflows” gate is treated as a meaningful operational
  control.
- Risk is managed, not eliminated.

### 4.2 Boundary Model

- Unprivileged build stage (`pull_request`):
  - Treat as untrusted code execution context.
  - Avoid write paths that are not essential for correctness.
- Privileged follow-up stage (`workflow_run`):
  - Performs required writes (notably GHCR publish and status updates).
  - Must avoid avoidable blast-radius increases (for example, unnecessary shared
    cache writes from untrusted-origin code paths).

---

## 5. Residual Risk and Worst-Case Scenarios

Even with workflow approval and scoped design, privileged follow-up execution
has real risk:

- Image/package tampering if publish controls are weak.
- Cache poisoning if shared cache writes are allowed from untrusted-origin
  flows.
- Misleading status updates that obscure real test outcomes.
- CI resource exhaustion (intentional or accidental heavy workloads).

Mitigations in this design:

- workflow approval gate,
- scoped write-path design (canonical GHCR publication, minimized extra writes),
- explicit artifact contracts,
- deterministic status and gate logic.

---

## 6. Artifact Contract

### 6.1 Build Context and Transfer State

- `e2e-build-context-core`
  - Produced in `03 - E2E` before bake.
  - Contains `event_name`, `is_fork`, `pr_number`, `pr_sha`, `head_sha`.
- `e2e-build-transfer-state`
  - Produced after publication/handoff determination.
  - Contains `image_transfer` mode and event context.

### 6.2 Image and Runtime Artifacts

- Non-fork GHCR maps:
  - `e2e-image-map`
  - `e2e-image-map-base`
- Fork handoff payload:
  - `e2e-image-handoff` (image archive + base/full source lists)
- Plugin runtime payload:
  - `e2e-plugin-jars`

### 6.3 Mode Semantics

- `ghcr`: build run already published images to GHCR.
- `fork-handoff`: build run exported prebuilt images for privileged same-run
  publication.
- `none`: failure/unknown transfer state.

---

## 7. Fork vs Non-Fork Execution Model

### 7.1 Non-Fork

- `03 - E2E` builds and publishes to GHCR.
- `E2E / Tests` runs in cross-run artifact mode.
- Executor pulls GHCR images and executes suites.

### 7.2 Fork

- `03 - E2E` builds once and exports handoff payload (no rebuild).
- `E2E / Tests` publishes handed-off images to GHCR in same run.
- Executor consumes same-run image maps and executes suites.

---

## 8. Status and Checkpoint Contract

- Single required context: `03 Checkpoint - E2E`.
- Wrapper workflow owns pending and terminal status reporting.
- Terminal status must reflect real gate outcome and avoid misclassifying fork
  failures as non-fork transfer failures.

### 8.1 Two Identities Exist in `workflow_run`

This architecture intentionally separates:

- **Run/orchestration identity**
  - The GitHub Actions run record shown in the Actions UI/API.
  - For `workflow_run`, this is labeled from the triggering workflow metadata
    (`head_branch`, `head_sha`).
  - This label is operational metadata, not the authoritative answer to "which
    code did we validate for PR status purposes?"
- **Validation identity**
  - The code ref actually executed by the downstream wrapper and reusable
    executor.
  - In this design, that is determined by artifact-derived context plus
    `checkout_ref`.
  - The commit that receives the PR-facing checkpoint is determined by
    `status_sha`, not by the run record label shown in the Actions list.

### 8.2 Audit-Surface Rule

- The Actions run list is useful for orchestration troubleshooting.
- The commit/PR status surface is the authoritative place to answer whether a
  given PR/head commit is currently green.
- A single run can appear in the Actions UI as a `develop`/`workflow_run` object
  while still validating PR-derived code and posting `03 Checkpoint - E2E` to
  the PR head commit.
- Conversely, multiple `workflow_run` executions can share similar displayed run
  metadata while only one status attachment is visible on the commit/PR surface.

### 8.3 Consequence

When investigating parity, do **not** assume:

- displayed run `head_sha` in Actions history == authoritative tested commit,
- or "the commit page only shows one attached run" == "only one downstream run
  existed."

The authoritative tested commit for E2E checkpoint purposes is the one named by
`status_sha`.

---

## 9. Why Parity Still Breaks

The current fork/non-fork topology revamp removed one major source of
non-parity: duplicate privileged fork rebuilds as the steady-state model.
However, parity can still fail for reasons outside the image-transfer design.

### 9.1 What Is **Not** Preventing Parity

- The current intended architecture is no longer "old fork path vs new develop
  path" for this remediation target.
- For the unified model, both PR and post-merge validation are supposed to feed
  the same downstream executor contract.
- The presence of a `workflow_run` label on `develop` is not itself proof that
  different code was executed.

### 9.2 What **Actually** Prevents Parity

- **Non-deterministic E2E suites**
  - Playwright jobs still show timing-sensitive failures:
    - transient fetch/save failures in analyzer flows,
    - stale page/context polling after navigation,
    - browser/response binding races on reload/navigation-heavy tests.
  - This means "same effective code under test" can still produce different red
    or green outcomes across runs.
- **Runtime-environment sensitivity**
  - E2E depends on Docker container startup timing, database readiness, plugin
    seeding, browser startup, and bridge/harness availability.
  - Small timing shifts between runs can change the observed result without any
    code difference.
- **Audit-surface mismatch**
  - Operators can compare the wrong things:
    - the Actions run label,
    - the attached checkpoint status,
    - the checked-out ref inside the executor.
  - This creates an apparent parity failure even when the real problem is
    unstable test/runtime behavior.

### 9.3 Accurate Summary

The main remaining parity blocker is **not** the existence of `workflow_run`
itself. It is that the downstream E2E executor is still not deterministic enough
for "green before merge" to reliably predict "green after merge" when the same
logical code path is exercised.

### 9.4 Remediation Landed For Determinism

The following targeted test/runtime hardening changes were implemented to remove
the repeated flake signatures seen in harness shard `1/2` and core shard `2/2`:

- **Post-save reload settling in analyzer results flow**

  - File: `frontend/playwright/helpers/accept-results.ts`
  - Added `page.waitForLoadState("load")` plus a stable post-navigation element
    assertion before row-count polling.
  - Intent: avoid touching response objects while Playwright is still rebinding
    connection state after full-page reload.

- **Deterministic analyzer API readiness before Save**

  - File: `frontend/playwright/helpers/create-analyzer-from-profile.ts`
  - Added bounded readiness polling of
    `/api/OpenELIS-Global/rest/analyzer/analyzers` after mock network creation
    and immediately before form save.
  - Intent: prevent transient `Failed to fetch` notification failures caused by
    temporary connectivity instability during mock network setup.

- **Navigation settle hardening for sidenav refresh persistence**

  - File: `frontend/playwright/tests/foundational/core/sidenav.spec.ts`
  - Changed refresh navigation wait mode from `domcontentloaded` to `load`.
  - Intent: ensure response binding and resource load are complete before
    asserting persisted collapsed state.

- **Mock analyzer cleanup race reduction**
  - File: `frontend/playwright/helpers/create-analyzer-from-profile.ts`
  - Added existence check (`GET /analyzers`) before `DELETE` in mock-network
    cleanup path.
  - Intent: reduce noisy 404 teardown races and lower setup/teardown timing
    variance.

Operational decision: CI `retries` remains `0`; stability is enforced through
test/helper determinism rather than retry masking.

---

## 10. Known GitHub Actions Semantics That Shape Design

Validated platform constraints:

- Fork-originated `pull_request` runs generally have write scopes downgraded to
  read-only unless special settings apply.
- `workflow_run` provides privileged follow-up execution with access to write
  tokens/secrets.
- `workflow_run` run records are labeled from the triggering workflow metadata;
  those labels are not sufficient by themselves to identify the authoritative
  commit receiving PR-facing status.
- GitHub explicitly warns `workflow_run` on untrusted code paths can introduce
  cache-poisoning and write-privilege risks.
- `workflow_run` branch filters apply to the triggering workflow run branch.

Docker/BuildKit constraints:

- `type=gha` cache obeys GitHub cache restrictions.
- export/import can hit timeout/throttling constraints.

---

## 11. Decision Log (Accepted / Rejected / Deferred)

### Accepted

- Use mutually exclusive fork/non-fork build publication paths.
- Keep GHCR as canonical downstream image contract.
- Use fork image handoff + privileged publish to avoid second fork rebuild.
- Split early context and late transfer-state artifacts.

### Rejected

- Inline ternary expression injection in bake `set:` block as the primary
  control mechanism.
- Overwrite-dependent artifact control flow.
- Treating `workflow_run` payload/API as primary fork detector.
- Accepting full fork rebuild as steady-state behavior.

### Deferred

- Larger CI topology changes (for example, full trigger model redesign) beyond
  this remediation.

---

## 12. Pitfalls From Parallel CI Remediation and Feature Delivery

- CI architecture can drift faster than docs when fixes land in rapid small PRs.
- Parallel feature work causes real pressure: CI behavior changes while active
  PRs are trying to merge.
- GitHub Actions skip semantics (`needs` + skipped job behavior) can invalidate
  seemingly correct `if` logic.
- Missing artifacts can silently collapse execution into the wrong branch of
  orchestration.
- UI/check surfaces may suggest one root cause while workflow code points to
  another.
- GitHub can present a run under one displayed branch/SHA identity while the E2E
  checkpoint is intentionally attached to another commit identity.
- Branch protection and operator-model docs can become stale immediately after
  checkpoint contract changes.

---

## 13. Reality-Check Gate for No-Rebuild Handoff

Before broadening implementation, validate:

- handoff artifact size/transfer practicality for real image payloads,
- runtime overhead versus previous duplicate-build cost,
- ability to reconstruct image-map + plugin-jars contract without rebuilding,
- compatibility with current downstream reusable executor.

If these checks fail, stop and review before expanding scope.

---

## 14. GitHub Evidence Appendix

### Repository Workflows

- [.github/workflows/e2e-playwright.yml](.github/workflows/e2e-playwright.yml)
- [.github/workflows/e2e-tests.yml](.github/workflows/e2e-tests.yml)
- [.github/workflows/e2e-authoritative-reusable.yml](.github/workflows/e2e-authoritative-reusable.yml)
- [.github/workflows/publish-images.yml](.github/workflows/publish-images.yml)
- [.github/e2e-ci-operator-model.md](.github/e2e-ci-operator-model.md)

### Relevant PR History

- [#3298](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3298)
- [#3299](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3299)
- [#3301](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3301)
- [#3146](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3146)
- [#3257](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3257)

### Evidence Patterns To Validate During Triage

- Compare:
  - triggering build-run metadata,
  - downstream `checkout_ref`,
  - downstream `status_sha`,
  - and the commit surface that shows `03 Checkpoint - E2E`.
- Do not treat the Actions list label alone as the source of truth for which
  commit was validated for PR gating.

### Platform Docs

- [GitHub workflow permissions model](https://docs.github.com/en/actions/writing-workflows/workflow-syntax-for-github-actions#how-permissions-are-calculated-for-a-workflow-job)
- [GitHub `workflow_run` event semantics](https://docs.github.com/en/actions/reference/events-that-trigger-workflows#workflow_run)
- [Docker BuildKit `type=gha` cache backend](https://docs.docker.com/build/cache/backends/gha/)
