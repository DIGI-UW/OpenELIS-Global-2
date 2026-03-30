---
name: e2e ci trust boundary hardening
overview:
  Preserve the current two-layer E2E architecture where it materially helps
  fork/non-fork parity and GHCR-backed cache transfer, but reduce the privileged
  surface so that PR CI only validates, never publishes real deliverables, and
  uses privilege solely where the workflow model truly requires it. The target
  state is a clean separation between transient E2E cache publication, read-only
  test execution, and the final 03 Checkpoint reporter.
todos:
  - id: save-plan-record
    content:
      Record the agreed workplan in .specify/plans/ on a dedicated
      branch/worktree before implementation work begins.
    status: completed
  - id: map-current-trust-boundaries
    content:
      Inventory every current privilege boundary in 03 - E2E and E2E / Tests and
      Publish, including reusable/manual wrapper entrypoints, packages write,
      statuses write, actions read, inherited secrets, GHCR image transfer, and
      PR-code checkout points.
    status: pending
  - id: classify-required-vs-accidental-privilege
    content:
      Separate truly required capabilities from convenience-driven ones so the
      workflow design reflects the rule that PR CI validates only and merged
      branches alone publish reusable outputs.
    status: pending
  - id: isolate-ghcr-e2e-cache-lane
    content:
      Redesign the GHCR usage model as an explicit transient e2e-cache lane with
      distinct write and read permission scopes, separate from any future real
      image publication concerns.
    status: pending
  - id: remove-nonessential-secret-dependence
    content:
      Define one explicit deterministic CI auth contract, then replace TEST_PASS
      and any broad secret inheritance in PR validation paths with that contract
      or other non-sensitive inputs.
    status: pending
  - id: shrink-job-permissions
    content:
      Move from workflow-wide permissions to job-scoped least privilege so cache
      publishers, test executors, and the 03 Checkpoint reporter each get only
      the minimum permissions they require.
    status: pending
  - id: resolve-fork-pr-trust-model
    content:
      Decide and document whether privileged fork rebuilds remain a short-term
      exception or must be eliminated by moving fork execution fully into the
      unprivileged pull_request lane.
    status: pending
  - id: verify-develop-merge-behavior
    content:
      Prove that the resulting design remains green after merge to develop and
      preserves expected behavior for same-repo PRs, fork PRs, and push/release
      publish flows.
    status: pending
  - id: document-operator-model
    content:
      Write clear repo guidance describing which workflow changes require
      default branch merge, which code/config changes are picked up pre-merge,
      and how to reason about the split safely.
    status: pending
isProject: false
---

# E2E CI Trust Boundary Hardening Plan

## Why This Plan Exists

The recent CI work surfaced a real architectural tension rather than a single
bug:

- The project needs a meaningful analyzer-harness and core E2E gate on PRs.
- Fork and non-fork PRs should remain as aligned as practical.
- `GHCR` is being used as a transient cache and transport layer between the
  build and test phases because it is materially simpler than artifact tarballs
  or double rebuilds.
- The project does **not** want pre-merge CI to behave like publishing.
- The only clearly unavoidable privileged PR-side action is reporting the final
  `03 Checkpoint - E2E` result back to the PR.

GitHub Actions supports this pattern, but its security model is explicit:

- `pull_request` is the unprivileged lane for untrusted PR code.
- `workflow_run` can access secrets and write-capable tokens.
- Running PR-controlled code in a privileged `workflow_run` lane expands risk
  and must be treated as an intentional exception, not a default.

Relevant references:

- GitHub docs on `workflow_run` privileges and warnings:
  <https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows>
- GitHub Security Lab on "pwn requests":
  <https://securitylab.github.com/resources/github-actions-preventing-pwn-requests/>
- GitHub reusable workflow same-commit behavior for local references:
  <https://github.blog/changelog/2022-01-25-github-actions-reusable-workflows-can-be-referenced-locally/>

This plan defines the work needed to get from the current workable-but-blurry
setup to a setup whose trust boundaries are explicit, minimal, and durable.

## Current State Summary

The current E2E pipeline is split across:

- `.github/workflows/e2e-playwright.yml`
- `.github/workflows/e2e-tests.yml`
- `.github/workflows/e2e-playwright-analyzer-harness-reusable.yml`
- `.github/workflows/e2e-playwright-analyzer-harness-manual.yml`

Current behavior:

1. `03 - E2E` builds images and attempts to push them to `GHCR`.
2. `E2E / Tests and Publish` is triggered via `workflow_run`.
3. Non-fork PRs consume the cached images from `GHCR`.
4. Fork PRs fall back to a privileged rebuild path in the downstream workflow.
5. The downstream workflow also posts the custom `03 Checkpoint - E2E` status.
6. Some test lanes still depend on `TEST_PASS` and broad secret inheritance.
7. The manual analyzer-harness wrapper still broadens the surface via
   `secrets: inherit`, even though it is not the main PR execution path.

Current pain points:

- Workflow-level permissions are broader than the actual responsibilities of
  each job.
- The GHCR cache lane is operationally useful, but its privilege model is not
  isolated clearly enough from other concerns.
- `TEST_PASS` is not a meaningful secret boundary, but it still forces secret
  handling into PR validation paths.
- The current plan needs one explicit replacement contract for CI auth, or the
  secret-removal phase will drift during implementation.
- The fork rebuild path is the main trust-boundary exception because it runs
  PR-controlled code in a more privileged workflow context.
- Publication logic still lives in the same `workflow_run` file as the
  privileged PR-side test orchestration, which keeps the trust boundary harder
  to reason about even when the jobs are conditionally isolated.
- The repo lacks concise operator guidance on what changes are picked up pre-
  merge versus what requires default-branch workflow updates.

## Design Principles

The target setup must follow these principles:

- PR CI validates only.
- Real publication happens only on `push`/`release` after merge.
- `GHCR` cache writes for PRs are treated as transient E2E transport, not as
  product publishing.
- Cache publication and cache consumption are separate permission classes.
- CI auth uses one documented deterministic contract rather than ad hoc
  `TEST_PASS` consumption across reusable workflows.
- The final `03 Checkpoint - E2E` reporter is a tiny trusted lane.
- Secrets are removed from PR validation unless they are genuinely necessary.
- Workflow privilege is scoped per job, not granted broadly at the workflow
  level.
- Any remaining privileged execution of PR-controlled code is documented as an
  explicit exception with a migration path away from it.

## Recommended Target Architecture

The recommended target keeps the current two-layer topology for now, because it
still provides practical benefits:

- `GHCR` remains the simplest cache/transport between build and test layers.
- The custom `03 Checkpoint - E2E` reporter remains available.
- Fork and non-fork PRs keep a comparable external UX.

However, the trust model is tightened into four distinct lanes:

Implementation choice for this hardening effort:

- Lane D should be extracted out of `e2e-tests.yml` into a separate post-merge
  workflow triggered only on `push`/`release`.
- If extraction proves too disruptive for the first pass, then keeping Lane D in
  the same file is an explicitly temporary state that must still satisfy strict
  job isolation and must not be treated as the target architecture.

### Lane A: Shared Build / Cache Publisher

Purpose:

- Build Docker images once.
- Publish transient E2E cache images under a dedicated `e2e-cache` namespace.

Rules:

- This lane may have `packages: write`.
- It must not have `statuses: write`.
- It must not publish DockerHub or any long-lived release artifact.
- Cache images must remain namespaced and documented as ephemeral test assets.

### Lane B: Test Executors

Purpose:

- Run Playwright core, analyzer harness, and any remaining PR validation tests.

Rules:

- These jobs should have only `packages: read`, `contents: read`, and whatever
  minimal artifact-read capability is required.
- These jobs should not have `statuses: write`.
- These jobs should not inherit broad secrets.
- These jobs should run with deterministic CI credentials that are not treated
  as protected repository secrets.

### Lane C: 03 Checkpoint Reporter

Purpose:

- Post `pending` and final `03 Checkpoint - E2E` results back to the PR.

Rules:

- This job gets `statuses: write`.
- It should not check out PR code.
- It should not build, publish, or execute test commands.
- It may read workflow conclusions or artifacts, but should remain tiny and
  trusted.

### Lane D: Post-Merge Publication

Purpose:

- Publish tested images or other release artifacts after merge only.

Rules:

- Runs on `push` to `develop` and/or `release`.
- Consumes tested outputs only after the E2E gate is green.
- Remains isolated from PR validation concerns entirely.

## Workplan

## Phase 1: Inventory The Real Trust Boundaries

Goal:

- Replace intuition with an explicit privilege map of the current pipeline.

Tasks:

1. Enumerate the permissions and secret inputs currently used by each job in:
   - `e2e-playwright.yml`
   - `e2e-tests.yml`
   - `e2e-playwright-analyzer-harness-reusable.yml`
   - `e2e-playwright-analyzer-harness-manual.yml`
   - any called Cypress reusable workflows
2. Mark which jobs:
   - check out PR-controlled code
   - run Maven, npm, Docker, or Playwright against PR-controlled inputs
   - push to `GHCR`
   - post statuses
   - use `secrets: inherit`
   - rely on `TEST_PASS`
3. Record the current fork and non-fork execution graphs separately.

Deliverable:

- A one-page privilege matrix covering every job, token scope, secret input, and
  PR-code execution point.

Checkpoint:

- No implementation begins until the privilege map is complete and reviewed.

## Phase 2: Separate Required Privilege From Convenience

Goal:

- Distinguish what the pipeline genuinely requires from what was added for speed
  or simplicity.

Questions to answer explicitly:

1. Is `packages: write` required for correctness, or only for the transient
   `GHCR` cache handoff?
2. Is `TEST_PASS` a true secret, or can it be replaced with deterministic test
   credentials?
3. Is the custom `03 Checkpoint - E2E` status required, or could branch
   protection rely directly on gate jobs?
4. Which parts of the current downstream workflow exist only because fork PRs
   cannot push cache images from the `pull_request` lane?
5. Does Lane D remain a job-isolated post-merge section temporarily, or is it
   extracted immediately into its own post-merge workflow? The recommended
   target is extraction.

Deliverable:

- A short architecture note with two columns:
  - `required for validation`
  - `optimization or convenience`

Checkpoint:

- The team signs off on the set of capabilities that are truly allowed in PR CI.

## Phase 3: Isolate GHCR As A Dedicated E2E Cache Lane

Goal:

- Keep the performance and simplicity benefits of `GHCR` while making its role
  narrow and explicit.

Tasks:

1. Formalize the `ghcr.io/<owner>/openelis-global-2/e2e-cache/...` namespace as
   transient CI cache only.
2. Ensure only the cache-publisher lane can write to that namespace.
3. Ensure test lanes are read-only consumers of cache artifacts.
4. Verify no other workflow path reuses this namespace for release publication.
5. Add cleanup and retention guidance so the cache lane stays operationally
   isolated from long-lived artifacts.

Checkpoint:

- The pipeline has exactly two package permission classes:
  - `packages: write` for cache publishing
  - `packages: read` for cache consumption

## Phase 4: Remove Nonessential Secret Dependence

Goal:

- Make PR validation independent of protected secrets wherever possible.

Tasks:

1. Replace `TEST_PASS` with deterministic CI fixture credentials or other
   non-sensitive values that can safely exist in unprivileged lanes.
2. Write down the exact CI auth contract before implementation:
   - where the credential comes from
   - which jobs consume it
   - whether it is a repo variable, checked-in fixture value, or another
     deterministic non-secret input
   - how manual and reusable workflows receive it without `secrets: inherit`
3. Remove `secrets: inherit` from any called workflow that does not require a
   specific explicit secret.
4. Pass only named inputs or named secrets where a true secret remains
   necessary.
5. Verify that analyzer harness seeding, application login, and Playwright setup
   still work in both same-repo and fork PR contexts.

Checkpoint:

- PR validation no longer depends on broad inherited secrets.

## Phase 5: Move To Job-Scoped Least Privilege

Goal:

- Shrink permissions to the minimum needed for each lane.

Tasks:

1. Remove broad workflow-level permissions from `e2e-tests.yml` where possible.
2. Assign job-scoped permissions:
   - cache publisher: `packages: write`
   - test executors: `packages: read`
   - reporter: `statuses: write`
3. Set `persist-credentials: false` on checkouts that run PR-controlled code and
   do not need git writes.
4. Confirm that reporter jobs do not check out PR code at all.
5. Replace wrapper-level `secrets: inherit` with named inputs/secrets, including
   the manual analyzer-harness entrypoint.

Checkpoint:

- No job has both broader write privilege and a larger-than-necessary execution
  surface.

## Phase 6: Resolve The Fork PR Trust Exception

Goal:

- Decide deliberately whether the privileged fork rebuild remains acceptable.

This is the one design area where the conversation leaves a real trade-off:

- Keeping fork rebuild in the downstream lane preserves current parity and keeps
  `GHCR` as the simplest cache handoff.
- Removing it yields a stricter security model, but likely requires either:
  - a pull-request-only validation path
  - slower rebuilds
  - different branch protection mechanics
  - or a less uniform fork/non-fork experience

Required output:

- A written decision with one of two states:

State A: **Short-term accepted exception**

- Fork rebuild remains, but it is documented as the only privileged execution of
  PR-controlled code.
- Its permissions and secret surface are minimized aggressively.
- It gets a follow-up migration plan.

State B: **Strict no-privileged-fork-execution**

- Fork PR validation moves fully into the unprivileged `pull_request` lane.
- `workflow_run` becomes reporter-only or is removed from PR validation
  altogether.

Checkpoint:

- This decision is made explicitly before implementation is considered complete.

## Phase 7: Validate Merge-to-Develop Behavior

Goal:

- Ensure the refined design works not only on PRs, but also after merge.

Tasks:

1. Confirm that merged `develop` uses one coherent workflow + payload lineage.
2. Validate non-fork PR flow end-to-end.
3. Validate fork PR flow end-to-end, including approval requirements if
   applicable.
4. Validate `push` to `develop` and `release` publish paths remain unaffected
   except where intentionally changed.
5. Validate the manual analyzer-harness entrypoint after the auth/secret
   contract changes so it does not silently diverge from the main reusable
   workflow.
6. Confirm `03 Checkpoint - E2E` stays green in the intended branch protection
   model.

Checkpoint:

- The new setup is proven green for:
  - same-repo PRs
  - fork PRs
  - merged develop
  - post-merge publish flows

## Phase 8: Document The Operator Model

Goal:

- Stop future confusion about what GitHub picks up from PR code versus default-
  branch workflows.

Required documentation:

1. Which changes are picked up pre-merge because they live in checked-out repo
   payload:
   - scripts
   - configs
   - tests
   - compose files
   - submodule pointers
2. Which changes require default-branch merge to affect `workflow_run`
   orchestration:
   - workflow topology
   - job wiring
   - top-level permissions
   - artifact plumbing
3. How local reusable workflow references resolve.
4. How to reason about fork versus non-fork PR behavior safely.

Checkpoint:

- The repo has a concise operator note that future contributors can follow
  without rediscovering these boundaries by trial and error.

## Success Criteria

This plan is complete only when all of the following are true:

- The project retains a useful and comprehensible E2E gate for same-repo and
  fork PRs.
- `GHCR` is isolated as a transient `e2e-cache` lane, not treated as general
  publishing.
- Test execution jobs are read-only consumers, not broad privileged workflows.
- The deterministic CI auth contract is documented and replaces ad hoc
  `TEST_PASS` reliance in routine PR validation.
- The only clearly privileged PR-facing action left is the `03 Checkpoint - E2E`
  reporter, unless the team deliberately accepts a documented fork exception.
- `TEST_PASS` and broad secret inheritance are removed from routine PR
  validation.
- Lane D publication is either extracted into its own post-merge workflow or is
  explicitly documented as a temporary exception with strict job isolation.
- Merge to `develop` remains green and does not regress the fork/non-fork model.
- The repo has documentation explaining the GitHub Actions trust boundary and
  default-branch-versus-checked-out-payload behavior clearly.

## Recommended Order Of Execution

When work resumes, execute in this order:

1. Phase 1 privilege inventory
2. Phase 2 required-vs-convenience classification
3. Phase 3 GHCR cache-lane isolation
4. Phase 4 secret removal
5. Phase 5 job-scoped permissions
6. Phase 6 fork trust decision
7. Phase 7 validation across PR and merged flows
8. Phase 8 operator documentation

This order keeps the design evidence-driven: we first map the current risk, then
shrink it, then decide whether the final remaining fork exception is acceptable
or must be eliminated.
