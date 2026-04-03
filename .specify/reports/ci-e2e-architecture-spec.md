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

---

## 9. Known GitHub Actions Semantics That Shape Design

Validated platform constraints:

- Fork-originated `pull_request` runs generally have write scopes downgraded to
  read-only unless special settings apply.
- `workflow_run` provides privileged follow-up execution with access to write
  tokens/secrets.
- GitHub explicitly warns `workflow_run` on untrusted code paths can introduce
  cache-poisoning and write-privilege risks.
- `workflow_run` branch filters apply to the triggering workflow run branch.

Docker/BuildKit constraints:

- `type=gha` cache obeys GitHub cache restrictions.
- export/import can hit timeout/throttling constraints.

---

## 10. Decision Log (Accepted / Rejected / Deferred)

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

## 11. Pitfalls From Parallel CI Remediation and Feature Delivery

- CI architecture can drift faster than docs when fixes land in rapid small PRs.
- Parallel feature work causes real pressure: CI behavior changes while active
  PRs are trying to merge.
- GitHub Actions skip semantics (`needs` + skipped job behavior) can invalidate
  seemingly correct `if` logic.
- Missing artifacts can silently collapse execution into the wrong branch of
  orchestration.
- UI/check surfaces may suggest one root cause while workflow code points to
  another.
- Branch protection and operator-model docs can become stale immediately after
  checkpoint contract changes.

---

## 12. Reality-Check Gate for No-Rebuild Handoff

Before broadening implementation, validate:

- handoff artifact size/transfer practicality for real image payloads,
- runtime overhead versus previous duplicate-build cost,
- ability to reconstruct image-map + plugin-jars contract without rebuilding,
- compatibility with current downstream reusable executor.

If these checks fail, stop and review before expanding scope.

---

## 13. GitHub Evidence Appendix

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

### Platform Docs

- [GitHub workflow permissions model](https://docs.github.com/en/actions/writing-workflows/workflow-syntax-for-github-actions#how-permissions-are-calculated-for-a-workflow-job)
- [GitHub `workflow_run` event semantics](https://docs.github.com/en/actions/reference/events-that-trigger-workflows#workflow_run)
- [Docker BuildKit `type=gha` cache backend](https://docs.docker.com/build/cache/backends/gha/)
