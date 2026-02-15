# Feature Specification: Full E2E Battery Migration and Refactor to Playwright

**Feature Branch**: `[201-e2e-playwright-risk-parity]`  
**Created**: 2026-02-14  
**Updated**: 2026-02-15 (`/speckit.clarify` scope update)  
**Status**: Draft  
**Input**: User direction: "Fully incorporate findings into the specs. The goal
of this feature is full migration and full refactoring of the E2E battery,
including proper and stable test data and fixture management."

## Clarify Findings Incorporated (Codebase + Research)

Current codebase baseline (from inventory + parity artifacts):

- Active spec count is still legacy-heavy (`Cypress: 47`, `Playwright: 20`).
- Scenario parity status remains incomplete (`PARTIAL: 23`, `LEGACY_ONLY: 24`,
  `GAP: 1` in `parity-matrix.csv`).
- Critical risk remains in storage and selected core/admin gaps, including one
  P0 workflow still `LEGACY_ONLY` (`LEG-CYP-045`).

Current architecture/quality findings needing spec-level correction:

- `frontend/cypress.config.js` runs with `testIsolation: false`, which conflicts
  with isolation-first guidance for independent tests.
- Cypress fixture behavior is split across multiple command files (including
  duplicate command registration for `loadStorageFixtures`), increasing drift
  risk.
- Several Playwright parity tests still use fallback/early-return patterns that
  indicate smoke-level coverage rather than full parity assertions.
- Dual framework operation is active, but the previous spec still codified "no
  Cypress decommission", which conflicts with this updated feature objective.

External best-practice research incorporated:

- Playwright recommends isolated tests, locator-first assertions, and sharding
  with report merge (`blob` + `merge-reports`) for scalable CI.
- Playwright recommends auth strategy selection by test behavior: shared-account
  setup for read-only state; per-worker accounts for stateful parallel tests.
- Cypress guidance emphasizes independent tests, state cleanup before each test,
  and programmatic state control over implicit cross-test coupling.

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Establish an Authoritative E2E Baseline (Priority: P0)

As a QA/engineering lead, I need a complete and trusted inventory/parity
baseline so full migration decisions are evidence-based.

**Why this priority**: Migration cannot be considered complete while parity
status is unknown or partially tracked.

**Independent Test**: Inventory and parity validators pass and produce
consistent counts across CI/local runs.

**Acceptance Scenarios**:

1. **Given** Cypress + Playwright suites, **When** inventory generation runs,
   **Then** every active spec and scenario is represented with domain, risk,
   execution status, and skip/weak-signal metadata.
2. **Given** parity mapping, **When** validators run, **Then** all legacy P0/P1
   scenarios are mapped with explicit parity status and owners.
3. **Given** migration planning artifacts, **When** reviewed, **Then**
   unresolved risks are ranked by risk tier and operational impact.

---

### User Story 2 - Build a Stable, Unified Fixture and Test Data Platform (Priority: P0)

As an E2E maintainer, I need deterministic and unified fixture management so
both migration work and long-term Playwright execution are stable and
repeatable.

**Why this priority**: Flaky or drifting test data invalidates parity confidence
and can mask real regressions.

**Independent Test**: Repeated clean runs with reset/load/verify yield stable
outcomes and no cross-test data contamination.

**Acceptance Scenarios**:

1. **Given** fixture setup for E2E tests, **When** setup executes, **Then** both
   frameworks consume the same canonical fixture source and loader contract.
2. **Given** stateful mutation tests, **When** tests run in parallel, **Then**
   account/data isolation strategy prevents cross-test interference.
3. **Given** cleanup/reset operations, **When** tests finish or rerun, **Then**
   fixture baseline and test-created data are reliably separated and auditable.

---

### User Story 3 - Complete Risk-Based Parity Migration to Playwright (Priority: P0)

As a release owner, I need all required workflows migrated to Playwright at full
parity depth (not smoke-only) before migration is declared complete.

**Why this priority**: Full migration requires behavior protection, not just
presence of similarly named specs.

**Independent Test**: Parity matrix reaches target end-state where required
scenarios are `PASS` or explicitly exceptioned with approval records.

**Acceptance Scenarios**:

1. **Given** P0 scenarios, **When** migration is complete, **Then** every P0 row
   is `PASS` in Playwright with strong user-visible and real-effect assertions.
2. **Given** P1 scenarios, **When** migration is complete, **Then** each is
   either `PASS` or formally approved as an exception.
3. **Given** legacy weak assertions or skip-heavy cases, **When** Playwright
   replacements are reviewed, **Then** assertion depth is upgraded and evidence
   is linked in parity artifacts.

---

### User Story 4 - Cut Over to Playwright and Finalize Full Migration (Priority: P1)

As stakeholders, we need Playwright to become the sole required E2E gate after
stabilization, with Cypress retired from active gate duty in a controlled
manner.

**Why this priority**: The updated goal is full migration and refactoring, not
indefinite dual-framework operation.

**Independent Test**: CI required checks and project guidance reflect Playwright
as the final E2E framework, with Cypress status explicitly transitioned to
archival/non-gating mode.

**Acceptance Scenarios**:

1. **Given** current Cypress and Playwright run side by side, **When** the
   pre-cutover parity milestone is executed, **Then** all currently active
   Cypress scenarios have passing Playwright parity coverage in CI.
2. **Given** parity and reliability gates are met, **When** cutover occurs,
   **Then** Playwright is the required E2E gate and emits merged shard reports.
3. **Given** a temporary dual-run window, **When** stabilization completes,
   **Then** Cypress is removed from required gate checks per approved cutover
   policy.
4. **Given** historical comparison needs, **When** migration closes, **Then**
   legacy Cypress artifacts/links are preserved according to retention policy.

---

### Edge Cases

- How are scenarios treated when a migrated Playwright test early-returns
  instead of asserting behavior (false confidence risk)?
- How is parity judged when legacy Cypress behavior was dependent on shared
  state (`testIsolation: false`) and migration enforces isolation?
- How are per-worker test accounts provisioned/cleaned for workflows that mutate
  server-side state under parallel execution?
- What is the cutover rule when Playwright is green but residual Cypress
  comparison failures remain during the transition window?
- How are fixture regressions distinguished from product regressions in parity
  reporting?

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: The project MUST maintain an authoritative scenario-level
  inventory for both frameworks, including active/inactive/skip/weak-signal
  status.
- **FR-002**: The project MUST maintain risk-tiered parity mapping (P0/P1/P2)
  with explicit status values and owner fields.
- **FR-003**: Every legacy P0/P1 scenario MUST map to one or more Playwright
  scenarios with traceable parity evidence.
- **FR-004**: P0 migration completion criteria MUST require `PASS` parity status
  (no `PARTIAL`, `LEGACY_ONLY`, or `GAP` allowed for P0 at signoff).
- **FR-005**: P1 migration completion criteria MUST require either `PASS` or
  `EXCEPTION_APPROVED` with documented rationale and approver.
- **FR-006**: The feature MUST implement and enforce a unified fixture/test-data
  contract shared by Cypress (during transition) and Playwright.
- **FR-007**: Fixture tooling MUST support deterministic `reset`, `load`,
  `verify`, and optional `cleanup` workflows with machine-readable evidence.
- **FR-008**: Test-created data MUST be namespaced/ranged and cleanly separable
  from baseline fixtures.
- **FR-009**: E2E tests MUST be independent and runnable in isolation; framework
  configuration and test patterns MUST not rely on cross-test state coupling.
- **FR-010**: Authentication strategy MUST be explicit: shared-account setup for
  read-only/non-mutating flows, and worker/account isolation for mutating flows
  in parallel.
- **FR-011**: Playwright CI MUST use scalable sharding and merged report output
  (`blob` + merge) to support growing suite size.
- **FR-012**: During migration window, dual-run comparison reporting MUST
  include scenario-level divergence and failure classification.
- **FR-013**: Failure classification MUST include at least:
  infrastructure/setup, test assertion, and cross-framework parity divergence.
- **FR-014**: Runtime metrics MUST be collected per run and evaluated against an
  agreed migration/runtime budget.
- **FR-015**: Feature completion MUST include controlled Cypress gate retirement
  (decommission from required CI checks) after cutover/stabilization criteria
  are met.
- **FR-016**: Migration documentation MUST define post-migration artifact
  retention policy for legacy Cypress evidence.
- **FR-017**: The cutover sequence MUST include a dedicated side-by-side parity
  milestone where the current active Cypress battery and Playwright battery run
  together in CI and parity is verified before enabling cutover.
- **FR-018**: The side-by-side parity milestone MUST have zero active
  `LEGACY_ONLY`, `GAP`, or `PARTIAL` parity statuses for the current active
  Cypress scenario set.

### Constitution Compliance Requirements (OpenELIS Global)

- **CR-001**: E2E implementation MUST comply with Constitution Section V.5:
  focused development runs plus mandatory full-suite validation at defined
  gates.
- **CR-002**: Tests MUST preserve real E2E semantics (real browser, backend,
  database) for workflows classified as E2E.
- **CR-003**: New/updated tests MUST prioritize stable selectors and
  user-observable assertions per testing roadmap and Playwright guidance.
- **CR-004**: CI/CD changes MUST produce auditable artifacts
  (logs/screenshots/traces/parity reports) for triage and compliance evidence.
- **CR-005**: Framework policy for this feature MUST transition from
  dual-framework migration mode to Playwright-final mode, with Cypress archived
  per approved migration policy.

### Key Entities _(include if feature involves data)_

- **Coverage Inventory Entry**: Scenario-level record with suite, domain, risk,
  status, and evidence links.
- **Parity Mapping Entry**: Legacy-to-Playwright mapping with parity verdict,
  assertion-depth notes, and ownership.
- **Fixture Contract Entry**: Definition of baseline dataset, ID namespaces,
  setup/reset/verify operations, and cleanup rules.
- **Reliability Evaluation Record**: Run-level pass/fail and classification data
  for baseline-flaky migrated scenarios.
- **Cutover Readiness Gate**: Aggregated criteria object for migration
  completion and Cypress gate retirement decision.

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: 100% of active Cypress and Playwright scenarios are represented in
  inventory and parity artifacts.
- **SC-002**: 100% of P0 legacy scenarios are `PASS` in Playwright at feature
  signoff.
- **SC-003**: 100% of P1 legacy scenarios are `PASS` or `EXCEPTION_APPROVED`
  (with approver and rationale).
- **SC-004**: Baseline critical gaps (`SKIP_HEAVY`, `COVERAGE_HOLE`,
  `NO_ACTIVE_LEGACY`) are closed or exceptioned with no unowned P0/P1 items.
- **SC-005**: Migrated baseline-flaky scenarios reach >=95% pass rate over 20
  CI-equivalent runs.
- **SC-006**: Fixture workflow reproducibility is demonstrated by repeated
  clean-run validations with stable outcome classification.
- **SC-007**: Playwright shard results are merged into a single report artifact
  per CI run with trace/screenshot evidence on failures.
- **SC-008**: Playwright is the required E2E gate at completion, and Cypress is
  retired from required checks per approved migration policy.
- **SC-009**: A pre-cutover side-by-side parity milestone is completed and
  evidenced in CI, with full parity across the current active Cypress battery.

## Clarification Questions (`/speckit.clarify`)

1. **Cypress end-state policy**: After Playwright signoff, do you want Cypress
   to be:
   - A) fully removed from CI and repository,
   - B) removed from required checks but retained in repo (archived),
   - C) retained as optional scheduled run?
2. **Parity threshold for completion**: Must **all P2** also be migrated in this
   feature, or can P2 remain as approved deferred backlog?
3. **Fixture reset policy in CI**: Should every shard/run enforce a hard
   `--reset` fixture load, or allow reuse when verification passes?
4. **Account isolation strategy**: For mutating tests in parallel, should we
   require one account per worker now, or allow serial execution with shared
   account until account pool automation is ready?
5. **Dual-run stabilization window**: What is the target duration/number of runs
   before Cypress decommission decision (for example, N consecutive green runs)?
