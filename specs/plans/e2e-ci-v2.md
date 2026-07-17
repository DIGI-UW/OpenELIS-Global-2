---
name: E2E CI V2
overview:
  "Fold determinism, retries=0 truthfulness, and CI topology cleanup into PR
  #3265 by replacing the synthetic single-checkpoint model with two
  always-required native E2E workflows: one authoritative for non-fork PRs and
  one authoritative for fork PRs, with the fork-native path triggered via
  pull_request_target after maintainer approval and GHCR/image-map/plugin-jar
  handoff validated as a hard requirement."
todos:
  - id: save-plan-in-specs
    content:
      Save the approved plan into specs/plans inside the repository as the first
      execution step so the rollout record lives with the codebase.
    status: in_progress
  - id: validate-plan-consistency
    content:
      Validate the saved repo-local plan for consistency against current
      workflow names, required checks, artifact contracts, fork/non-fork
      authority rules, and the retries=0 determinism goal before implementing
      changes.
    status: completed
  - id: fix-ogc284-runtime-flake
    content:
      Define and validate the fix path for the optimistic-locking /
      StaleObjectStateException bug exposed by retries=0 in ogc-284 before
      treating CI cleanup as complete.
    status: pending
  - id: inline-nonfork-e2e
    content:
      Plan inline non-fork execution inside .github/workflows/e2e-playwright.yml
      using same-run artifacts and make 03 Checkpoint - E2E authoritative only
      for non-forks.
    status: pending
  - id: native-fork-e2e
    content:
      Plan a native fork executor workflow derived from
      .github/workflows/e2e-tests.yml, triggered via pull_request_target,
      authoritative only for fork PRs, and explicit no-op for non-forks.
    status: pending
  - id: adopt-single-pr-cutover
    content: Land the CI topology cutover in PR #3265 as one determinism-focused PR with logical checkpoints or commits, not as separately merged CI phases.
    status: pending
  - id: validate-fork-approval-trust-model
    content:
      Treat maintainer approval of fork workflow runs as the trust boundary for
      privileged fork E2E execution and validate that approval behavior in
      GitHub UI/gh is compatible with the planned trigger model.
    status: pending
  - id: validate-ghcr-handoff
    content:
      Validate the GHCR/image-map/plugin-jar contract for both paths, including
      same-run artifact access for non-forks and deterministic rebuild/cache
      semantics for forks.
    status: pending
  - id: remove-synthetic-checks
    content:
      Plan removal of e2e-pending-status.yml and synthetic check ownership after
      native required checks are proven.
    status: pending
  - id: stabilize-publish-gating
    content:
      Plan minimal publish-images.yml adjustments only after the new required
      check topology is proven on develop.
    status: pending
isProject: false
---

# E2E CI V2 Plan

## Goal

Adopt the simpler v2 architecture:

- Non-fork PRs use native inline E2E execution in
  [`.github/workflows/e2e-playwright.yml`](/Users/pmanko/code/OpenELIS-Global-2/.github/workflows/e2e-playwright.yml).
- Fork PRs use a separate native fork workflow derived from
  [`.github/workflows/e2e-tests.yml`](/Users/pmanko/code/OpenELIS-Global-2/.github/workflows/e2e-tests.yml)
  and triggered via `pull_request_target`.
- Both checks are always required, but exactly one is authoritative per PR type
  and the other exits as an explicit successful no-op.
- No synthetic `checks.create()` / `checks.update()` checkpoint ownership.
- GHCR/image-map/plugin-jar handoff is validated as a hard functional
  requirement, not assumed.
- All determinism work, including the retries=`0` blocker fix, lands in PR
  `#3265` as one coordinated changeset with logical checkpoints.

## Why This Direction

The current split model has two independent problems:

- Presentation instability: synthetic `03 Checkpoint - E2E` is attached to
  nondeterministic check suites.
- Execution fragility: downstream `workflow_run` jobs can fail before tests
  start because artifacts like `e2e-image-map-base` and `e2e-plugin-jars` are
  missing.

Recent `develop` failures show both classes are real:

- Topology failure: downstream artifact lookup failed before E2E execution.
- Runtime failure: `Playwright Core 2/2` failed after setup, exposing the
  `ogc-284` patient optimistic-locking flake once retries are removed.

The v2 plan is intended to remove the first class on the common non-fork path
without hiding the second class.

## Required Architecture

### Required Check Model

Use two always-required native checks:

- `03 Checkpoint - E2E`
- `E2E (Fork PR)`

Behavior:

- Non-fork PRs:
  - `03 Checkpoint - E2E` runs the real suite and is authoritative.
  - `E2E (Fork PR)` detects non-fork and exits `success` with an explicit
    `not_applicable` summary.
- Fork PRs:
  - `E2E (Fork PR)` runs the real privileged suite and is authoritative.
  - `03 Checkpoint - E2E` detects fork delegation and exits `success` with an
    explicit `not_applicable` summary.

This avoids conditional branch protection while preserving functional parity.

### Executor Ownership

- [`.github/workflows/e2e-playwright.yml`](/Users/pmanko/code/OpenELIS-Global-2/.github/workflows/e2e-playwright.yml)
  - Remains the main `03 - E2E` workflow.
  - Becomes the native inline executor for non-forks.
  - Keeps `shared-build` and same-run artifact publishing.
  - Adds/retains an internal gate job named `03 Checkpoint - E2E` that is real
    only for non-forks.
  - For forks, this gate does not fail or hang; it emits delegated/no-op
    success.
- [`.github/workflows/e2e-tests.yml`](/Users/pmanko/code/OpenELIS-Global-2/.github/workflows/e2e-tests.yml)
  - Is repurposed into a native fork-specific executor workflow, renamed to
    something like `E2E (Fork PR)`.
  - Trigger changes from `workflow_run` to `pull_request_target` so the native
    fork check exists directly on the PR instead of being synthesized later.
  - Removes `set-pending-status` and `report-status` synthetic check ownership.
  - Keeps fork rebuild and real test execution.
  - For non-forks, exits as explicit successful no-op.
- [`.github/workflows/e2e-pending-status.yml`](/Users/pmanko/code/OpenELIS-Global-2/.github/workflows/e2e-pending-status.yml)
  - Deleted after native checks replace synthetic checkpoint ownership.

### Fork Approval And Trust Boundary

- The fork-native executor is intentionally planned around
  `pull_request_target`.
- Maintainer approval of the workflow run is treated as the trust-escalation
  boundary for that PR, not as friction to eliminate.
- After approval, the fork-native workflow is allowed to execute PR-controlled
  build/test code in a base-repo privileged context.
- The implementation must keep permissions minimal and intentional, and it must
  check out the PR merge ref or equivalent reviewed PR code path explicitly
  rather than relying on implicit refs.
- This trust model is acceptable for the determinism plan only because approval
  is an explicit maintainer action for that PR.

### Publish Path

- [`.github/workflows/publish-images.yml`](/Users/pmanko/code/OpenELIS-Global-2/.github/workflows/publish-images.yml)
  - Publish is expected to remain conceptually compatible because it already
    waits on `03 Checkpoint - E2E`, which will become a native job check rather
    than a synthetic one.
  - Do not redesign it in the same PR.
  - Validate that it continues to work with minimal or no change; only make
    targeted adjustments if the native-check transition exposes a real
    incompatibility.

## GHCR / Artifact Validation Requirement

This is the key functional requirement for v2.

### What Must Be Proven

The plan is not complete until both of these are demonstrated:

- Non-fork path:
  - `shared-build` can hand off the exact image/plugin artifacts needed by
    inline E2E jobs without cross-run artifact lookup.
  - GHCR pull/retag still works from same-run image maps or direct local image
    availability as intended.
- Fork path:
  - the fork executor can still obtain equivalent runnable images and plugin
    jars in a deterministic way.
  - if the fork path rebuilds from cache instead of consuming same-run
    artifacts, that cache-based rebuild must be verified to produce the same
    executable test stack.

### Current Workflow Hooks Relevant To Validation

Existing files already encode two artifact modes that the v2 plan should
preserve where possible:

- [`.github/workflows/e2e-tests.yml`](/Users/pmanko/code/OpenELIS-Global-2/.github/workflows/e2e-tests.yml)
  - Non-fork path currently downloads `e2e-image-map-base` using
    `run-id: ${{ needs.setup.outputs.build_run_id }}`.
  - Fork fallback currently publishes `e2e-plugin-jars`, `e2e-image-map`, and
    `e2e-image-map-base` from `fork-rebuild`.
- [`.github/workflows/e2e-playwright-analyzer-harness-reusable.yml`](/Users/pmanko/code/OpenELIS-Global-2/.github/workflows/e2e-playwright-analyzer-harness-reusable.yml)
  - Already supports `build_run_id == ''` for same-run artifact download and
    `is_fork` branching.
- [`.github/workflows/e2e-cypress-deprecated.yml`](/Users/pmanko/code/OpenELIS-Global-2/.github/workflows/e2e-cypress-deprecated.yml)
  - Already supports `build_run_id == ''` for same-run `e2e-image-map-base`
    download and `is_fork` branching.

### Validation Matrix

The architecture should only be considered correct if all of these pass:

- Non-fork success:
  - shared build publishes image/plugin artifacts
  - inline Playwright/Cypress/harness jobs consume them in the same workflow
  - `03 Checkpoint - E2E` is red/green authoritative
  - `E2E (Fork PR)` is explicit no-op success
- Non-fork plumbing failure:
  - broken image-map/plugin-jar handoff turns `03 Checkpoint - E2E` red
- Non-fork runtime failure:
  - real test failure turns `03 Checkpoint - E2E` red
- Fork success:
  - fork executor obtains equivalent build artifacts or cache-rebuilt images
  - `E2E (Fork PR)` is red/green authoritative
  - `03 Checkpoint - E2E` is explicit no-op success
- Fork plumbing failure:
  - broken cache rebuild / artifact restore turns `E2E (Fork PR)` red
- Fork runtime failure:
  - real test failure turns `E2E (Fork PR)` red

## Relation To The `#3265` Blocker

This plan must explicitly account for the fact that CI topology cleanup is not
the only blocker.

PR `#3265` currently removes Playwright retries (`retries: 1 -> 0`). That
exposes a real application/test flake rather than causing it:

- `ogc-284` optimistic locking / `StaleObjectStateException`
- patient concurrency issue around `Patient#9001000`

Implications:

- The flake must be fixed inside `#3265` as the first logical determinism
  checkpoint, not deferred to a separate PR.
- The v2 CI topology must not reintroduce retries or otherwise mask this
  failure.
- Verification for the topology work must explicitly run with retries still at
  `0` on the PR that carries the determinism change.

## Recommended Rollout Sequence

### Phase 0: Save Plan In Repo

Before any CI or runtime work:

- Create `specs/plans/` in the repository if it does not already exist.
- Save an in-repo markdown copy of this approved plan there so the execution
  record lives alongside the workflow changes.
- Treat that repo-local plan as the implementation checklist for the remaining
  phases.

## Planning-Time Consistency Validation

This validation has been completed during planning and should be treated as an
input to implementation, not a future rollout step.

Validated findings:

- The simpler v2 design does not require conditional branch protection if both
  native checks are always required and the non-authoritative path exits as an
  explicit successful no-op.
- Current `develop` branch protection still requires only
  `01 Checkpoint - Backend`, `02 Checkpoint - Frontend`, and
  `03 Checkpoint - E2E`, so adopting v2 will require an explicit
  branch-protection update if `E2E (Fork PR)` becomes a required native context.
- [`.github/workflows/e2e-pending-status.yml`](/Users/pmanko/code/OpenELIS-Global-2/.github/workflows/e2e-pending-status.yml)
  is purely synthetic checkpoint ownership and is removable once native checks
  replace it.
- The current split model already distinguishes non-fork cross-run artifact
  download from fork same-run artifact download in
  [`.github/workflows/e2e-tests.yml`](/Users/pmanko/code/OpenELIS-Global-2/.github/workflows/e2e-tests.yml).
- The reusable workflows already support same-run operation via
  `build_run_id == ''`:
  - [`.github/workflows/e2e-playwright-analyzer-harness-reusable.yml`](/Users/pmanko/code/OpenELIS-Global-2/.github/workflows/e2e-playwright-analyzer-harness-reusable.yml)
  - [`.github/workflows/e2e-cypress-deprecated.yml`](/Users/pmanko/code/OpenELIS-Global-2/.github/workflows/e2e-cypress-deprecated.yml)
- This means inline non-fork execution in `03 - E2E` is technically compatible
  with same-run artifact consumption and does not inherently require the current
  downstream `workflow_run` split.
- The native fork-executor trigger decision is now made: the plan adopts
  `pull_request_target`, because the two-required-checks model only works
  cleanly if the fork-native check is visible directly on the PR.
- Public-fork workflow approval behavior is treated as part of the design, not
  an incidental inconvenience: maintainer approval is the intended trust
  boundary before the privileged fork-native executor runs.
- Fork execution still has one unresolved artifact-contract design point:
  whether the native fork executor should rely on rebuilt GHCR-tagged images,
  same-run uploaded artifacts, or cache-only rebuild semantics. That contract
  must be chosen explicitly during implementation.
- `publish-images.yml` on current `develop` already waits on
  `03 Checkpoint - E2E` via check-run polling, so publish safety should be
  adapted conservatively rather than simplified immediately.
- The retries=`0` / `ogc-284` optimistic-locking issue is a real runtime truth
  condition and must remain a hard invariant throughout CI refactoring.

Open consistency risks that remain intentionally unresolved until
implementation:

- Final branch-protection cutover order when moving from one required E2E check
  to two required native contexts after `#3265` merges.
- The precise artifact contract for the fork-native path under GHCR/cache
  constraints.

### Phase 1: Runtime Truth First

Within `#3265`, before topology cleanup logic is considered complete:

- Diagnose and fix the `ogc-284` patient optimistic-locking bug.
- Prove the relevant Playwright shard passes without retry masking.

### Phase 2: Native Check Cutover In One PR

Implement the non-fork and fork-native topology cutover together inside `#3265`.

- Do not merge the non-fork path separately from the fork path.
- Keep the work as separate logical checkpoints or commits for review clarity,
  but treat it as one branch-protection cutover.

### Phase 3: Native Non-Fork Path

In
[`.github/workflows/e2e-playwright.yml`](/Users/pmanko/code/OpenELIS-Global-2/.github/workflows/e2e-playwright.yml):

- Inline the non-fork Playwright/Cypress/harness execution.
- Reuse same-run artifact semantics already supported by the reusable workflows
  (`build_run_id == ''`).
- Make `03 Checkpoint - E2E` authoritative only for non-forks.
- Preserve explicit fork delegation behavior.

### Phase 4: Native Fork Path

In
[`.github/workflows/e2e-tests.yml`](/Users/pmanko/code/OpenELIS-Global-2/.github/workflows/e2e-tests.yml):

- Convert to native `E2E (Fork PR)` executor.
- Change the trigger to `pull_request_target`.
- Remove synthetic checkpoint behavior.
- Make non-fork behavior explicit no-op success.
- Validate GitHub UI and `gh` approval behavior for fork PRs and document the
  approval step as part of the trust model.
- Validate whether fork execution should consume rebuilt GHCR-tagged images,
  same artifacts, or cache-only rebuilds; document whichever contract is chosen.

### Phase 5: Remove Synthetic Checkpoint Ownership

- Delete
  [`.github/workflows/e2e-pending-status.yml`](/Users/pmanko/code/OpenELIS-Global-2/.github/workflows/e2e-pending-status.yml).
- Remove any remaining `checks.create()` / `checks.update()` ownership logic
  from the fork workflow.
- After `#3265` merges and both native checks exist, update branch protection to
  require both native checks.

### Phase 6: Publish Safety Follow-up

- Update
  [`.github/workflows/publish-images.yml`](/Users/pmanko/code/OpenELIS-Global-2/.github/workflows/publish-images.yml)
  only as much as needed to trust the new authoritative path.
- Do not collapse publish waiting logic until `develop` is stable under the new
  topology.

## Key Risks To Explicitly Test

- Same-run reusable workflow artifact resolution when `build_run_id == ''`
- GHCR tag and pull consistency after moving non-fork tests inline
- Fork-native executor behavior under `pull_request_target`, including
  maintainer approval flow in GitHub UI/`gh` and minimal-permission correctness
  after approval
- Avoiding accidental green states from skipped/not-applicable paths
- Preserving `retries: 0` truthfulness while topology changes land

## Decision Criteria

The v2 direction should be adopted only if all are true:

- Non-fork E2E no longer depends on cross-run artifact downloads.
- Fork E2E has a deterministic artifact/cache contract.
- Both PR types can fail on plumbing and real test failures.
- No synthetic checkpoint suite roulette remains.
- `#3265` can pass with retries at `0` because the underlying `ogc-284` flake is
  fixed, not hidden.
- The fork-native path works with the repository’s real approval gate semantics,
  and that approval is explicitly treated as the trust boundary for privileged
  fork execution.
- The saved repo-local plan remains consistent with the implemented workflow
  names, required checks, and verification matrix at each phase boundary.
