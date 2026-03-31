# E2E CI Required vs Convenience Classification (Phase 2)

This note classifies current E2E pipeline capabilities into:

- required for validation correctness
- optimization or convenience (or mixed concern needing relocation)

## Required For Validation

| Capability                                                           | Why required                                                                   |
| -------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| Build once and share image outputs across E2E lanes                  | Avoids inconsistent test surfaces between Playwright/Cypress/analyzer harness. |
| Fork + non-fork PR E2E support                                       | Project requirement for trustworthy contribution gate coverage.                |
| Final PR-facing `03 Checkpoint - E2E` status                         | Required for branch-protection-friendly, single checkpoint UX.                 |
| Read-only package consumption in test executors                      | Needed to pull prebuilt GHCR cache images.                                     |
| Cross-run artifact reads (`actions: read`) in downstream workflow    | Required for image maps/plugin jars from triggering run.                       |
| Deterministic CI login contract (`TEST_USER` / `TEST_PASS` fallback) | Required for noninteractive E2E auth in all test lanes.                        |
| E2E gate aggregation job                                             | Required to fail PR when any required test lane fails.                         |

## Optimization / Convenience / Mixed Concern

| Capability                                         | Classification           | Why not fundamentally required in current location                                                       |
| -------------------------------------------------- | ------------------------ | -------------------------------------------------------------------------------------------------------- |
| Workflow-wide `packages: write` in `e2e-tests.yml` | Over-scoped convenience  | Only cache-publishing lanes need write; tests should be read-only.                                       |
| Workflow-wide `statuses: write` in `e2e-tests.yml` | Over-scoped convenience  | Only status reporter jobs need it.                                                                       |
| `secrets: inherit` on reusable calls               | Convenience anti-pattern | Named inputs/secrets are sufficient and safer.                                                           |
| `secrets.TEST_PASS` usage in PR test execution     | Legacy convenience       | CI defaults are intentionally non-secret; vars fallback is adequate.                                     |
| `environment: e2e` on test/build jobs              | Legacy coupling          | Not required for E2E validation; introduces unnecessary secret surface.                                  |
| Publish-to-DockerHub logic inside `e2e-tests.yml`  | Mixed concern            | Publication is post-merge concern, should live in separate workflow lane.                                |
| Fork rebuild running in privileged `workflow_run`  | Temporary exception      | Needed today for parity/performance, but architectural migration target remains artifact-based transfer. |

## Explicit Policy Decisions For Implementation

1. PR CI validates only; real publication is post-merge.
2. GHCR `e2e-cache` stays, but as explicit transient cache lane.
3. Executor jobs become read-only package consumers.
4. Status reporting is isolated to tiny reporter jobs.
5. CI auth contract is uniform everywhere:
   - `TEST_USER: ${{ vars.TEST_USER || 'admin' }}`
   - `TEST_PASS: ${{ vars.TEST_PASS || 'adminADMIN!' }}`
6. `fork-rebuild` remains a documented short-term exception with minimal scope:
   `packages: write` only.

## What This Means For Next Edits

1. Extract post-merge `publish-images` to dedicated `publish-images.yml`.
2. Move from workflow-level to job-scoped permissions in `e2e-tests.yml`.
3. Remove `secrets: inherit` and pass only named inputs/secrets.
4. Remove `environment: e2e`; introduce `environment: publish` for publish lane.
5. Add e2e-cache cleanup workflow (7-day retention).
6. Add operator runbook for `gh`-automated and manual fallback repo settings.
