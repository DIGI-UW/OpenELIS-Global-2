# File Workflow Remediation Archive Header

- Source worktree: `<worktree-path>`
- Source branch: `feat/014-file-workflow-remediation`
- Source plan: `<plan-path>`
- Date archived: `2026-03-18`
- Statement: This archived specs plan is the execution source of truth for 014
  remediation.

# 014 File Workflow Remediation (Worktree-First)

## Non-Negotiable Start Conditions

1. **Create a brand-new worktree from `origin/develop` before any
   implementation.**
2. **Immediately copy/archive this plan into that new worktree at**
   `specs/014-hjra-file-stream-alignment/file-workflow-remediation-plan.md`.
3. Do not start code/spec updates until both items above are completed and
   verified.

## Phase 0: Fresh Worktree + Plan Archive (Required First)

- Create new worktree and branch from remote develop:
  - `git fetch origin develop`
  - `git worktree add <new-worktree-path> -b feat/014-file-workflow-remediation origin/develop`
- Enter the new worktree and verify:
  - branch is `feat/014-file-workflow-remediation`
  - HEAD matches `origin/develop` baseline
  - submodules are initialized/clean (`git submodule update --init --recursive`)
- **Copy this plan into the new worktree specs folder first:**
  - target file:
    `specs/014-hjra-file-stream-alignment/file-workflow-remediation-plan.md`
- Add a short header in the archived plan:
  - source branch/worktree
  - date
  - statement: "This archived specs plan is the execution source of truth for
    014 remediation."

## Phase 1: Spec/Doc Ownership Sync (Parent + Submodules)

Objective: remove ambiguity and make ownership explicit everywhere.

- Parent repo spec sync:
  - `specs/014-hjra-file-stream-alignment/spec.md`
  - `specs/014-hjra-file-stream-alignment/plan.md`
  - `specs/014-hjra-file-stream-alignment/quickstart.md`
  - `specs/011-madagascar-analyzer-integration/research.md`
  - `specs/011-madagascar-analyzer-integration/templates/GENERIC-TEST-RECIPE.md`
  - `specs/011-madagascar-analyzer-integration/contracts/supported-analyzers.md`
- Guardrails/docs sync:
  - `AGENTS.md`
  - `docs/analyzers/playwright-ci-stabilization.md`
  - `plugins/README.md`
- Bridge docs/config comments sync:
  - `tools/openelis-analyzer-bridge/README.md`
  - `tools/openelis-analyzer-bridge/configuration.yml`

Required architecture statement everywhere:

- Bridge owns FILE polling/transport lifecycle.
- OE owns configuration + ingest/processing/domain persistence.
- OE app poller is optional future fallback only. It is not implemented on the
  current consolidation branch and must remain disabled by default if added
  later.

## Phase 2: Bridge Control Plane + Delivery Path

Implement on bridge submodule branch off bridge `origin/develop`.

- `tools/openelis-analyzer-bridge/src/main/java/org/itech/ahb/file/FileWatcher.java`
  - remove static-only lifecycle assumptions
  - support runtime add/remove watch directories
  - active when registrations exist, idle otherwise
- `tools/openelis-analyzer-bridge/src/main/java/org/itech/ahb/controller/AnalyzerRegistrationController.java`
  - FILE register/update/delete must mutate live watcher state
- `tools/openelis-analyzer-bridge/src/main/java/org/itech/ahb/file/FileMessageHandler.java`
  - route FILE deliveries to OE direct-import endpoint for analyzer-specific
    processing
- keep `AnalyzerRegistryConfig` as authoritative registration metadata, but
  ensure watcher consumes runtime registration effects

## Phase 3: OE Separation of Concerns

Implement on parent repo branch in the new worktree.

- Add direct bridge-ingest endpoint in OE for single-step import (non-preview
  human upload path):
  - `src/main/java/org/openelisglobal/analyzer/controller/AnalyzerUploadRestController.java`
    (or dedicated controller)
- Keep OE-side polling out of the active runtime path on this branch:
  - `src/main/resources/application.properties`
  - if `FileImportWatchService.java` is introduced later, it must be disabled by
    default and documented as fallback-only
- Full bridge lifecycle sync for analyzer CRUD + startup recovery:
  - `src/main/java/org/openelisglobal/analyzer/controller/AnalyzerRestController.java`
  - `src/main/java/org/openelisglobal/analyzer/service/BridgeRegistrationService.java`
  - add startup re-registration of active FILE analyzers after OE boot

## Phase 4: Mock + Plugin Alignment

- Mock server FILE simulation support:
  - `tools/analyzer-mock-server/server.py` add `/simulate/file/<template>` API
  - `tools/analyzer-mock-server/test_protocols.py` add coverage
- Validate plugin compatibility (no unexpected behavior regressions):
  - `plugins/analyzers/GenericFile/...`

## Phase 5: Runtime Wiring (CI + Dev)

- Update CI harness wiring so bridge can actually watch shared dirs:
  - `.github/ci/ci.analyzer-harness.yml`
- Update dev compose wiring equivalently:
  - `dev.docker-compose.yml`
  - `projects/analyzer-harness/docker-compose.dev.yml`
- Remove obsolete OE poll interval tuning and temporary debug-only CI steps
  where no longer needed.

## Phase 6: Tests and Regression Guarantees

- Bridge tests: watcher lifecycle, registration coupling, delivery path
  correctness.
- OE tests: direct-import endpoint, poller-disabled default, CRUD+startup
  registration sync.
- E2E updates:
  - `frontend/playwright/tests/file-import-results.spec.ts` should validate
    bridge-owned flow and remain stable
  - verify no regressions in existing file-import config specs
- Full CI validation target:
  - no regressions in Playwright core, analyzer harness shards, and existing E2E
    suite.

## Completion Gate

Work is complete only when all are true:

- Started from fresh worktree off `origin/develop`.
- Plan archived inside that worktree specs directory first.
- Specs/docs aligned across parent repo + bridge/mock/plugins.
- Bridge/mock/OE/plugins are consistent on FILE import path ownership and
  behavior.
- E2E remains green with no regressions; OE+bridge test coverage updated for new
  ownership boundaries.
