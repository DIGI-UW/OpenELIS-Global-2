---
name: authoritative harness path
overview:
  Adopt the CI analyzer-harness stack as the single authoritative base, add a
  dedicated local CI-parity runner for exact reproduction, and refactor local
  harness scripts to be thin wrappers or overlays on that same base. After Phase
  1 proves exact local reproduction of the current develop failure, pause for a
  formal review before Phase 2 remediation begins.
todos:
  - id: save-plan-to-specify
    content:
      Copy the finalized plan to .specify/plans/ as the durable project record
      before any other execution begins.
    status: in_progress
  - id: adopt-authoritative-base
    content:
      Treat the CI analyzer-harness stack as the single authoritative base and
      document that local harness tooling must reconcile to it.
    status: pending
  - id: build-exact-ci-runner
    content:
      Create a dedicated local CI-parity runner with an explicit preflight that
      verifies prerequisite state, then mirrors the analyzer-harness reusable
      workflow step-for-step and captures deterministic evidence bundles.
    status: pending
  - id: prove-current-develop-failure
    content:
      Use the local CI-parity runner on a clean stack to reproduce the current
      develop failure exactly before any remediation begins.
    status: pending
  - id: phase1-review-gate
    content:
      Stop after Phase 1 and review the reproduced failure, evidence bundle, and
      confirmed root-cause class before finalizing Phase 2 scope.
    status: pending
  - id: standardize-harness-scripts
    content:
      Refactor reset and bootstrap harness tooling so local workflows are
      wrappers or thin overlays on the authoritative CI base instead of a
      separate stack definition.
    status: pending
  - id: remove-soft-failures
    content:
      Eliminate fallback and warning-only behavior on the harness critical path
      and replace it with explicit hard failures and post-seed verification.
    status: pending
  - id: remediate-residual-tests
    content:
      After harness parity and hardening are proven, fix any remaining
      Playwright or product-level failures one at a time with the CI-parity
      runner as the regression gate.
    status: pending
isProject: false
---

# Authoritative Harness Path

## Validation Summary

This direction matches established best practices:

- [12-Factor dev/prod parity](https://12factor.net/dev-prod-parity): keep
  environments as similar as possible, minimize divergent tooling, use the same
  backing services everywhere.
- [Docker Compose merge guidance](https://docs.docker.com/compose/how-tos/multiple-compose-files/merge):
  one canonical base file plus focused overrides, not multiple independent
  environment definitions.

Design principles:

- One authoritative harness base (the CI compose pair).
- Optional local flavors layered on top as thin overlays.
- One explicit way to run the exact CI flavor locally for full parity.
- Hard failures on critical-path setup drift.

## Target Goal

The target outcome is unambiguous:

- Green analyzer-harness CI on the required gate.
- A stable, exact local CI-parity runner that reproduces CI failures before
  remediation and passes after remediation.
- No critical-path fallback behavior, no silent drift, and no guessing-based
  debugging.

Execution discipline:

- We iterate until the target goal is reached.
- We do not trade correctness for short-term green builds.
- Every change must be justified by evidence from code, logs, traces, runtime
  state, or explicit instrumentation.
- If the next step requires guessing, the correct action is to improve
  instrumentation or add a checkpoint first.

## Scope Boundary

The E2E pipeline has a clean separation between image build/publish and test
execution:

- `e2e-playwright.yml` runs `shared-build` (Maven build, plugin jars, Docker
  image build, GHCR push, artifact upload).
- `e2e-tests.yml` consumes those artifacts and runs `playwright-core`,
  `analyzer-harness`, and `cypress`.
- The analyzer harness reusable workflow starts from "images and plugins are
  available."

This plan's scope begins after the shared build. We do not touch image build,
plugin compilation, GHCR publish, or artifact upload workflows. The local
CI-parity runner assumes images are already built locally (via prior
`docker compose build` or equivalent) and plugin jars are already in
`volume/plugins/`.

## Decision

The analyzer-harness CI path is the authoritative base because it is the
required gate and the thing we must reproduce exactly.

Authoritative base files:

- [build.docker-compose.yml](build.docker-compose.yml) -- base stack definition
- [.github/ci/ci.analyzer-harness.yml](.github/ci/ci.analyzer-harness.yml) --
  harness service overlay
- [.github/workflows/e2e-playwright-analyzer-harness-reusable.yml](.github/workflows/e2e-playwright-analyzer-harness-reusable.yml)
  -- step-by-step CI workflow (source of truth for execution order)

Local harness scripts must be reconciled to this base, not maintained as a
separate behaviorally drifting stack.

## Phase 1: Exact CI-Parity Reproduction

Create a dedicated local runner that mirrors the CI `demo-shards` workflow order
exactly.

Primary deliverable:

- `projects/analyzer-harness/ci-parity-test.sh` -- the exact local CI simulator

Execution prerequisites (the script must verify these explicitly before it runs
the parity flow):

- Required compose images exist locally for `--no-build` startup.
- Plugin jars are already staged in `volume/plugins/`.
- `.env` exists (or the script can materialize it from `.env.example` exactly as
  CI does).
- `TEST_USER` and `TEST_PASS` are available (env or `.env`).
- Frontend dependencies are installed in `frontend/`.
- Playwright Chromium is installed.

Required behavior:

- The local CI simulator must not assume prerequisite state silently.
- Before Phase 1 execution, it must run a dedicated preflight and print a
  precise pass/fail report for every prerequisite.
- If the environment is not ready, it must stop before stack startup with
  actionable diagnostics.
- If we later add a convenience prep mode, it must be separate from the exact
  CI-parity execution path so the parity run itself remains explicit and
  auditable.

Exact CI step sequence the script must mirror (from reusable workflow lines
137-203):

1. Create
   `projects/analyzer-harness/volume/analyzer-imports/{quantstudio-5,quantstudio-7,fluorocycler-xt}/incoming`
   directories and fix permissions.
2. `docker compose -f build.docker-compose.yml -f .github/ci/ci.analyzer-harness.yml up -d --no-build --wait --wait-timeout 900`
3. Readiness: `curl -k -s -f https://localhost/`,
   `curl -k -s -f https://localhost:8442/actuator/health`,
   `curl -s -f http://localhost:8085/health` (each with 120s timeout).
4. `./src/test/resources/load-test-fixtures.sh --profile=harness --no-verify`
5. `bash projects/analyzer-harness/seed-analyzers.sh` with
   `BASE_URL=https://localhost`, `TEST_USER`, `TEST_PASS`,
   `DB_CONTAINER=openelisglobal-database`.
6. Wait for bridge-created import dirs to appear, then fix permissions.
7. Verify analyzer test mappings (warning-only in CI -- reproduce as-is).
8. `npm run pw:test -- --project=harness-demo --workers=1` with `CI=true`,
   `ANALYZER_HARNESS=true`, `BASE_URL=https://localhost`,
   `FILE_IMPORT_POLL_MS=5000`, `FILE_IMPORT_DROP_BUFFER_MS=45000`.

Hard checkpoints in the script (beyond CI's current checks):

- Preflight passes and proves the local machine is in the required ready state.
- Stack startup succeeds on the CI compose pair.
- All three readiness checks pass.
- Fixture load exits zero.
- Seed exits zero with no fallback warnings in output.
- Bridge registry (`GET http://localhost:8442/api/analyzers`) returns expected
  source bindings.
- The script captures and stores evidence bundles (OE logs, bridge logs,
  simulator logs, seed output, Playwright traces/screenshots).
- The script exits non-zero on the same failure CI is currently hitting.

Phase 1 success criteria:

- The local CI simulator can prove the machine is ready via preflight.
- The exact CI-parity path runs end-to-end on a clean stack.
- The same develop failure is reproduced locally with a durable evidence bundle.
- The failure is classified based on evidence, not speculation.

Important constraints:

- The script must reproduce CI as-is first, including soft checks and known
  mismatches (e.g., `https` readiness probe with `SERVER_SSL_ENABLED=false`).
  Hardening belongs to later phases, after exact reproduction is proven.
- `CI=true` is set to match CI's Playwright retry and reporting behavior. This
  is a conscious choice for parity; local-only runs without `CI=true` can use
  the regular `reset-env.sh` path once it is reconciled.
- The script's preflight is allowed to validate prerequisite state, but the
  exact parity run must remain a faithful execution of the CI harness path once
  preflight passes.
- When useful and safe, local and remote CI validation should run in parallel to
  reduce feedback time, but local evidence must remain the primary debugging
  surface and CI should be used to confirm parity rather than replace local
  diagnosis.

## Phase 1 Review Gate

After Phase 1 completes, stop and review before Phase 2.

Required review outputs:

- The exact reproduced failure
- The evidence bundle
- The confirmed divergence points between intended harness contract and observed
  behavior
- A validated decision on whether the failure is environment/setup, harness
  contract, product behavior, or test robustness
- The exact instrumentation and checkpoints needed so Phase 2 remediation can
  proceed without guesswork

Only after this review do we finalize and execute Phase 2 remediation scope.

## Phase 2: Standardize Harness Scripts Around The Authoritative Base

Refactor the existing local harness tooling so it no longer defines a separate
harness contract.

Key files:

- [projects/analyzer-harness/reset-env.sh](/Users/pmanko/code/OpenELIS-Global-2/projects/analyzer-harness/reset-env.sh)
- [projects/analyzer-harness/bootstrap.sh](/Users/pmanko/code/OpenELIS-Global-2/projects/analyzer-harness/bootstrap.sh)
- [projects/analyzer-harness/build.sh](/Users/pmanko/code/OpenELIS-Global-2/projects/analyzer-harness/build.sh)
- [projects/analyzer-harness/docker-compose.dev.yml](/Users/pmanko/code/OpenELIS-Global-2/projects/analyzer-harness/docker-compose.dev.yml)
- [projects/analyzer-harness/docker-compose.analyzer-test.yml](/Users/pmanko/code/OpenELIS-Global-2/projects/analyzer-harness/docker-compose.analyzer-test.yml)

Target state:

- `reset-env.sh` becomes a wrapper over the authoritative CI base path, or a
  thin local overlay on top of it.
- Any local-only flavor is clearly additive, not a second independent
  implementation.
- Shared concerns like bootstrap, plugins, fixtures, seed, readiness, and
  evidence capture are single-sourced.

Hard rule:

- No separate dev harness stack is allowed to drift behaviorally from the CI
  base on critical-path analyzer flows.

Phase 2 checkpoints:

- Existing local harness scripts are mapped to the authoritative CI base.
- Redundant or divergent stack definitions are either removed or converted into
  thin overlays.
- The local CI-parity runner remains the canonical reproduction path while local
  convenience wrappers stay additive only.

## Phase 3: Remove Soft Failures And Fix Harness Contract Violations

After parity reproduction and script consolidation, harden the harness contract.

Files likely involved:

- [projects/analyzer-harness/seed-analyzers.sh](/Users/pmanko/code/OpenELIS-Global-2/projects/analyzer-harness/seed-analyzers.sh)
- [tools/analyzer-mock-server/api.py](/Users/pmanko/code/OpenELIS-Global-2/tools/analyzer-mock-server/api.py)
- [tools/analyzer-mock-server/analyzer_network_manager.py](/Users/pmanko/code/OpenELIS-Global-2/tools/analyzer-mock-server/analyzer_network_manager.py)
- [.github/ci/ci.analyzer-harness.yml](/Users/pmanko/code/OpenELIS-Global-2/.github/ci/ci.analyzer-harness.yml)

Required hardening:

- Remove fallback IP behavior.
- Remove inferred critical-path setup where explicit contracts are possible.
- Convert setup warnings into hard failures for critical-path prerequisites.
- Add explicit post-seed verification of topology, OE registration, bridge
  registration, and analyzer reachability.
- Enforce one identity model end-to-end for the failing analyzer lane.

Debugging rules for this phase:

- No remediation by symptom alone.
- Every failure class must be backed by direct evidence from logs, traces, code
  inspection, registry state, or explicit runtime probes.
- If existing signals are insufficient, add instrumentation or a checkpoint
  before changing behavior.

Phase 3 checkpoints:

- Seed/setup no longer continues after critical-path failure.
- Identity and registration invariants are observable and verified
  automatically.
- The local CI-parity runner either reproduces the pre-fix failure or proves the
  corrected behavior with evidence.

## Phase 4: Address Residual Test-Level Failures One By One

Only after the environment and harness contract are stable should test-level
fixes be applied.

Likely targets:

- [frontend/playwright/tests/analyzer-test-connection.spec.ts](/Users/pmanko/code/OpenELIS-Global-2/frontend/playwright/tests/analyzer-test-connection.spec.ts)
- [frontend/playwright/tests/astm-genexpert-results.spec.ts](/Users/pmanko/code/OpenELIS-Global-2/frontend/playwright/tests/astm-genexpert-results.spec.ts)
- [frontend/playwright/tests/file-import-results.spec.ts](/Users/pmanko/code/OpenELIS-Global-2/frontend/playwright/tests/file-import-results.spec.ts)
- [frontend/playwright/helpers/results-ui.ts](/Users/pmanko/code/OpenELIS-Global-2/frontend/playwright/helpers/results-ui.ts)

Rules:

- One failing test at a time.
- Classify the failure before changing code.
- Re-run the local CI simulator before and after each substantive harness
  change.
- When useful and low-risk, run local validation and CI validation in parallel
  so remote confirmation does not wait on local completion, but do not skip
  local evidence-based diagnosis.

Phase 4 checkpoints:

- Each remaining failure is explicitly labeled as environment, harness contract,
  product logic, or test robustness.
- Each fix is validated locally first with the CI-parity runner.
- CI is used in parallel when it can shorten the loop without replacing
  disciplined local debugging.

## Exit Criteria

The work is complete only when:

- The local CI simulator reproduces the current develop failure exactly on a
  clean stack.
- Phase 1 review confirms the root-cause class and Phase 2 scope.
- Local harness scripts are standardized around the authoritative CI base path.
- Critical-path setup no longer contains soft failures or fallback behavior.
- Remote analyzer-harness CI passes with the same proven invariants.
- The exact CI-parity runner is the required first step for future harness
  remediation.
- The final green CI result is supported by checkpointed, evidence-based
  remediation rather than guesswork.
