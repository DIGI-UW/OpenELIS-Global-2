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
      Record the agreed workplan in specs/plans/ on a dedicated branch/worktree
      before implementation work begins.
    status: completed
  - id: map-current-trust-boundaries
    content:
      Inventory every current privilege boundary in 03 - E2E and E2E / Tests and
      Publish, including reusable/manual wrapper entrypoints, packages write,
      statuses write, actions read, inherited secrets, GHCR image transfer, and
      PR-code checkout points. Output a privilege matrix in
      specs/plans/e2e-ci-privilege-matrix.md.
    status: completed
  - id: classify-required-vs-accidental-privilege
    content:
      Separate truly required capabilities from convenience-driven ones so the
      workflow design reflects the rule that PR CI validates only and merged
      branches alone publish reusable outputs.
    status: completed
  - id: isolate-ghcr-e2e-cache-lane
    content:
      Redesign the GHCR usage model as an explicit transient e2e-cache lane with
      distinct write and read permission scopes, separate from any future real
      image publication concerns. Implement a scheduled cleanup workflow with
      7-day retention for e2e-cache images.
    status: completed
  - id: extract-lane-d-publication
    content:
      Extract publish-images into a separate publish-images.yml workflow
      triggered by workflow_run on 03 - E2E filtered to push/release events.
      Create a publish environment with deployment branch restriction on develop
      for DockerHub credentials.
    status: completed
  - id: remove-nonessential-secret-dependence
    content:
      Replace secrets.TEST_PASS with vars.TEST_PASS defaulting to adminADMIN!
      across all workflows. Replace secrets inherit with named inputs/secrets on
      all reusable workflow calls including Cypress and analyzer harness. Remove
      environment e2e from all jobs; move TEST_USER and TEST_PASS to repo-level
      variables.
    status: completed
  - id: shrink-job-permissions
    content:
      Move from workflow-wide permissions to job-scoped least privilege so cache
      publishers, test executors, and the 03 Checkpoint reporter each get only
      the minimum permissions they require. Set persist-credentials false on all
      checkout steps. Verify e2e-gate treats all-skipped as failure.
    status: completed
  - id: resolve-fork-pr-trust-model
    content:
      Fork-rebuild stays as a short-term accepted exception (State A). Scope
      fork-rebuild job to packages write only — no statuses write, no broad
      secrets. Status reporting remains in Lane C reporter which already covers
      fork PRs. Document as explicit exception with migration path.
    status: completed
  - id: verify-develop-merge-behavior
    content:
      Prove that the resulting design remains green after merge to develop and
      preserves expected behavior for same-repo PRs, fork PRs, and push/release
      publish flows.
    status: pending
  - id: document-operator-model
    content:
      Write clear repo guidance in .github/ describing which workflow changes
      require default branch merge, which code/config changes are picked up
      pre-merge, and how to reason about the split safely.
    status: completed
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
- Real publication happens only on `push`/`release` after merge, in a separate
  extracted workflow with a branch-restricted `publish` environment.
- `GHCR` cache writes for PRs are treated as transient E2E transport, not as
  product publishing. A scheduled cleanup workflow enforces 7-day retention.
- Cache publication and cache consumption are separate permission classes.
- CI auth uses one documented deterministic contract: `vars.TEST_PASS`
  (repo-level variable, default `'adminADMIN!'`) replaces `secrets.TEST_PASS`.
  No secrets are needed for PR test execution.
- The final `03 Checkpoint - E2E` reporter is a tiny trusted lane.
- Secrets are removed from PR validation unless they are genuinely necessary.
- Workflow privilege is scoped per job, not granted broadly at the workflow
  level. All checkouts set `persist-credentials: false`.
- CI auth fallback is explicit and uniform everywhere in E2E test execution:
  - `TEST_USER: ${{ vars.TEST_USER || 'admin' }}`
  - `TEST_PASS: ${{ vars.TEST_PASS || 'adminADMIN!' }}`
- The `e2e` environment is removed; test jobs use repo-level variables. A
  `publish` environment with deployment branch restriction on `develop` gates
  DockerHub credentials.
- Fork PR E2E is a first-class requirement. The fork-rebuild job is a documented
  short-term exception scoped to `packages: write` only, with status reporting
  handled by Lane C.
- Any remaining privileged execution of PR-controlled code is documented as an
  explicit exception with a migration path away from it.
- Cypress E2E remains in the PR gate (migration is a separate long-term effort);
  it is hardened like all other test executors.

## Delivery Strategy And Constraints

Primary delivery mode:

- Implement in one PR on the hardening branch when possible.

Allowed split condition:

- If GitHub Actions workflow update constraints materially block safe rollout
  validation in one PR (especially around `workflow_run` default-branch
  orchestration behavior), split into the minimum number of PRs required to
  preserve correctness and reviewer clarity.

Execution preference:

- Keep one PR unless a split is technically necessary; document the exact reason
  and boundary if split is required.

## Recommended Target Architecture

The recommended target keeps the current two-layer topology for now, because it
still provides practical benefits:

- `GHCR` remains the simplest cache/transport between build and test layers.
- The custom `03 Checkpoint - E2E` reporter remains available.
- Fork and non-fork PRs keep a comparable external UX.

However, the trust model is tightened into four distinct lanes:

Implementation decision (confirmed via QA):

- Lane D will be extracted out of `e2e-tests.yml` into a separate
  `publish-images.yml` workflow triggered by `workflow_run` on `03 - E2E`
  filtered to `push`/`release` events.
- The extracted workflow uses `environment: publish` with a deployment branch
  restriction on `develop` to gate DockerHub credentials.

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

- Run Playwright core, analyzer harness, Cypress (deprecated but still active),
  and any remaining PR validation tests.

Rules:

- These jobs should have only `packages: read`, `contents: read`, and whatever
  minimal artifact-read capability is required.
- These jobs should not have `statuses: write`.
- These jobs should not inherit broad secrets. All reusable workflow calls
  (including Cypress and analyzer harness) use named inputs/secrets, not
  `secrets: inherit`.
- These jobs should run with deterministic CI credentials via `vars.TEST_PASS`
  (repo-level variable, default `'adminADMIN!'`) and `vars.TEST_USER` (default
  `'admin'`). No environment is needed.
- All checkouts set `persist-credentials: false`.

### Lane C: 03 Checkpoint Reporter

Purpose:

- Post `pending` and final `03 Checkpoint - E2E` results back to the PR.

Rules:

- This job gets `statuses: write`.
- It should not check out PR code.
- It should not build, publish, or execute test commands.
- It may read workflow conclusions or artifacts, but should remain tiny and
  trusted.

### Lane D: Post-Merge Publication (Extracted Workflow)

Purpose:

- Publish tested images or other release artifacts after merge only.

Rules:

- Lives in a separate `publish-images.yml` workflow, triggered by `workflow_run`
  on `03 - E2E` filtered to `push`/`release` events.
- Uses `environment: publish` with deployment branch restriction on `develop`.
- DockerHub credentials (`DOCKERHUB_TOKEN`) are scoped to the `publish`
  environment.
- Consumes tested outputs only after the E2E gate is green.
- Remains isolated from PR validation concerns entirely.

## Workplan

## Cross-Phase Drift Guardrails

To prevent drift during implementation, enforce these checkpoints after each
phase:

1. Re-run privilege inventory snippets against edited workflow files and compare
   with the expected lane model.
2. Confirm no reintroduction of:
   - workflow-level broad write permissions
   - `secrets: inherit` in reusable workflow calls
   - `environment: e2e`
   - missing `persist-credentials: false` on checkout steps
3. Confirm the CI auth contract remains uniform:
   - `TEST_USER: ${{ vars.TEST_USER || 'admin' }}`
   - `TEST_PASS: ${{ vars.TEST_PASS || 'adminADMIN!' }}`
4. Validate that reporter jobs remain tiny and trusted (no checkout/build/test).
5. Record any divergence from plan intent immediately in this document before
   proceeding.

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

- A privilege matrix in `specs/plans/e2e-ci-privilege-matrix.md` covering every
  job, token scope, secret input, and PR-code execution point.

Checkpoint:

- No implementation begins until the privilege map is complete and reviewed.

## Phase 2: Separate Required Privilege From Convenience

Goal:

- Distinguish what the pipeline genuinely requires from what was added for speed
  or simplicity.

Questions to answer explicitly:

1. Is `packages: write` required for correctness, or only for the transient
   `GHCR` cache handoff?
2. ~~Is `TEST_PASS` a true secret?~~ **Resolved:** No. Replace with
   `vars.TEST_PASS` defaulting to `'adminADMIN!'`.
3. Is the custom `03 Checkpoint - E2E` status required, or could branch
   protection rely directly on gate jobs?
4. Which parts of the current downstream workflow exist only because fork PRs
   cannot push cache images from the `pull_request` lane?
5. ~~Lane D extraction timing?~~ **Resolved:** Extract now into
   `publish-images.yml`.

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
2. Ensure only the cache-publisher lane (and fork-rebuild) can write to that
   namespace.
3. Ensure test lanes are read-only consumers of cache artifacts.
4. Verify no other workflow path reuses this namespace for release publication.
5. Implement a scheduled cleanup workflow (e.g., weekly cron) that deletes
   e2e-cache package versions older than 7 days using the GitHub API. This can
   be delivered in this PR and is required for this effort.

Checkpoint:

- The pipeline has exactly two package permission classes:
  - `packages: write` for cache publishing (shared-build + fork-rebuild)
  - `packages: read` for cache consumption (all test executors)

## Phase 4: Remove Nonessential Secret Dependence

Goal:

- Make PR validation independent of protected secrets wherever possible.

Decisions (confirmed via QA):

- `TEST_PASS` is not a real secret — the value `'adminADMIN!'` is already
  hardcoded as a fallback in the Cypress workflow.
- Risk posture is explicit: these CI credentials are intentionally non-secret
  defaults already broadly exposed in existing OpenELIS CI/dev usage; removing
  secret plumbing does not create a new trust boundary regression.
- CI auth contract: `vars.TEST_PASS` (repo-level variable, default
  `'adminADMIN!'`) and `vars.TEST_USER` (repo-level variable, default
  `'admin'`). No secret is needed for E2E login.
- Strict fallback expressions are required in all relevant workflow jobs:
  - `TEST_USER: ${{ vars.TEST_USER || 'admin' }}`
  - `TEST_PASS: ${{ vars.TEST_PASS || 'adminADMIN!' }}`
- The `e2e` environment is removed. `TEST_USER` and `TEST_PASS` move to
  repo-level variables. A `publish` environment (branch-restricted to `develop`)
  is created for DockerHub credentials only.

Tasks:

1. Replace all `secrets.TEST_PASS` with `vars.TEST_PASS || 'adminADMIN!'` across
   `e2e-tests.yml`, `e2e-playwright-analyzer-harness-reusable.yml`, and
   `e2e-cypress-deprecated.yml`.
2. Replace `secrets: inherit` on the analyzer-harness and Cypress reusable
   workflow calls with named inputs for `TEST_PASS` and `TEST_USER`.
3. Remove `environment: e2e` from all jobs (shared-build, fork-rebuild,
   playwright-core, demo-shards).
4. Create `publish` environment with deployment branch restriction on `develop`;
   move `DOCKERHUB_TOKEN` into it.
5. Pass only named inputs or named secrets where a true secret remains necessary
   (e.g., `DOCKERHUB_TOKEN` in the extracted publish workflow).
6. Verify that analyzer harness seeding, application login, and Playwright setup
   still work in both same-repo and fork PR contexts.

Checkpoint:

- PR validation no longer depends on broad inherited secrets.
- No job references `environment: e2e`.
- The only environment in use is `publish` on the extracted Lane D workflow.

## Phase 5: Move To Job-Scoped Least Privilege

Goal:

- Shrink permissions to the minimum needed for each lane.

Decisions (confirmed via QA):

- No private submodules exist — `persist-credentials: false` is a clean sweep
  across all checkouts with no complications.
- The `e2e-gate` job must be verified to treat "all test jobs skipped" as
  failure, not success. Fix if needed (one-line check).

Tasks:

1. Remove broad workflow-level permissions from `e2e-tests.yml` where possible.
2. Assign job-scoped permissions:
   - cache publisher (shared-build, fork-rebuild): `packages: write`
   - test executors (playwright-core, analyzer-harness, cypress):
     `packages: read`
   - reporter (set-pending-status, report-status): `statuses: write`
   - fork-rebuild: `packages: write` only — no `statuses: write`
3. Set `persist-credentials: false` on ALL checkout steps across all workflows.
4. Confirm that reporter jobs do not check out PR code at all.
5. Replace wrapper-level `secrets: inherit` with named inputs/secrets, including
   the manual analyzer-harness entrypoint.
6. Verify `e2e-gate` treats "all test jobs skipped" as failure; add explicit
   check if needed.

Checkpoint:

- No job has both broader write privilege and a larger-than-necessary execution
  surface.
- Every checkout sets `persist-credentials: false`.

## Phase 6: Resolve The Fork PR Trust Exception

Decision (confirmed via QA): **State A — Short-term accepted exception**

Rationale:

- Fork PR E2E support is a firm requirement for the project.
- Eliminating fork rebuild would require artifact-based image transfer or
  local-only Docker builds — a larger architectural change out of scope here.
- The risk is mitigated by scoping fork-rebuild to the minimum:

Mitigations:

- Fork-rebuild job gets `packages: write` only — no `statuses: write`, no broad
  secrets, no other write permissions.
- `persist-credentials: false` on the checkout.
- Status reporting for fork PRs is handled by Lane C (the reporter), which
  already works for both fork and non-fork paths.
- Fork-rebuild is documented as the sole remaining privileged execution of
  PR-controlled code.
- Migration path: move to artifact-based image transfer so fork execution can
  move fully into the unprivileged `pull_request` lane.

Checkpoint:

- Fork-rebuild is the only job that both checks out PR-controlled code and has
  write permissions, and this is explicitly documented.

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

Operational note:

- Pre-merge workflow behavior and post-merge behavior can differ for
  `workflow_run` orchestration. If a discrepancy appears, stop and record
  whether it is due to default-branch workflow pickup rules versus checked-out
  payload behavior.

## Phase 8: Document The Operator Model

Goal:

- Stop future confusion about what GitHub picks up from PR code versus default-
  branch workflows.

Location: `.github/` (near the workflows, as a living document).

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
5. The CI auth contract: where test credentials come from, how they flow.
6. The `publish` environment and its branch restriction.

Checkpoint:

- The repo has a concise operator note that future contributors can follow
  without rediscovering these boundaries by trial and error.

## GitHub Configuration Execution Model

Goal:

- Minimize manual operator work by automating repository configuration via `gh`
  where possible, and provide deterministic manual steps only where API/CLI
  controls are insufficient.

Automation-first:

- Use `gh`/GitHub API to:
  - create or update the `publish` environment
  - set deployment branch policy restriction to `develop`
  - configure required environment secrets/variables where API permissions allow
    (for example `DOCKERHUB_TOKEN`, `DOCKERHUB_USERNAME`)
  - set/update repository-level variables:
    - `TEST_USER` (default: `admin`)
    - `TEST_PASS` (default: `adminADMIN!`)

Manual fallback:

- If any repo setting is not fully manageable via `gh` due to permission model,
  provide exact click-path and values in a checklist with no ambiguity.

Required output:

- Add an operator runbook under `.github/` with:
  - exact `gh` commands used
  - exact manual fallback steps for any unsupported setting
  - verification commands/checks to confirm effective configuration

## Success Criteria

This plan is complete only when all of the following are true:

- The project retains a useful and comprehensible E2E gate for same-repo and
  fork PRs.
- `GHCR` is isolated as a transient `e2e-cache` lane, not treated as general
  publishing.
- Test execution jobs are read-only consumers, not broad privileged workflows.
- The deterministic CI auth contract (`vars.TEST_PASS` / `vars.TEST_USER` with
  hardcoded defaults) is documented and replaces `secrets.TEST_PASS`.
- The only privileged PR-facing actions are the `03 Checkpoint - E2E` reporter
  (Lane C) and the fork-rebuild job (documented exception, `packages: write`
  only).
- `secrets.TEST_PASS` and broad `secrets: inherit` are removed from PR
  validation.
- Lane D publication is extracted into `publish-images.yml` with
  `environment: publish` (branch-restricted to `develop`).
- The `e2e` environment is removed; all test jobs use repo-level variables.
- A scheduled e2e-cache cleanup workflow enforces 7-day retention.
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
