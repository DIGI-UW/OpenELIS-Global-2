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

### User Story 4 - Cut Over to Playwright with Cypress Side-by-Side (Priority: P1)

As stakeholders, we need Playwright to become the primary migration target while
full Cypress wiring stays active side by side until migration results are
reviewed.

**Why this priority**: We need strict migration quality gates now, while
deferring Cypress sunset execution to a later follow-on decision.

**Independent Test**: CI runs show pre-cutover parity completion and Playwright
primary execution, while Cypress full battery still runs side by side.

**Acceptance Scenarios**:

1. **Given** current Cypress and Playwright run side by side, **When** the
   pre-cutover parity milestone is executed, **Then** all currently non-skipped
   Cypress coverage in scope has passing Playwright parity coverage in CI.
2. **Given** parity and reliability gates are met, **When** cutover occurs,
   **Then** Playwright is the primary E2E check and emits merged shard reports.
3. **Given** migration review is pending, **When** pull requests run, **Then**
   the full Cypress battery remains wired and executed in CI side by side.
4. **Given** migration closes, **When** signoff docs are prepared, **Then** a
   Cypress sunset recommendation is documented for a later follow-on feature.

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
- **FR-015**: This feature MUST keep full Cypress CI wiring active through the
  migration and stabilization milestones.
- **FR-016**: Migration documentation MUST define post-migration artifact
  retention policy for legacy Cypress evidence.
- **FR-017**: The cutover sequence MUST include a dedicated side-by-side parity
  milestone where the current active Cypress battery and Playwright battery run
  together in CI and parity is verified before enabling cutover.
- **FR-018**: The side-by-side parity milestone MUST have zero active
  `LEGACY_ONLY`, `GAP`, or `PARTIAL` parity statuses for current **non-skipped**
  Cypress coverage in the cutoff scope.
- **FR-019**: The parity scope for cutover MUST be frozen to a documented
  Cypress coverage cutoff snapshot (inventory artifact + run IDs) before M8a.
- **FR-020**: Fixture strategy MUST be explicitly evaluated and selected during
  implementation using criteria that balance atomic/independent test behavior,
  best-practice isolation, and CI/runtime performance.

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
- **CR-005**: Framework policy for this feature MUST keep dual-framework
  migration mode active (Playwright primary + full Cypress side by side), with
  Cypress sunset execution deferred to a follow-on scope.

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
  completion and Cypress sunset recommendation decision.

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
- **SC-008**: Playwright is primary for migration cutover while full Cypress
  wiring remains active side by side for review.
- **SC-009**: A pre-cutover side-by-side parity milestone is completed and
  evidenced in CI, with full parity for the current non-skipped Cypress coverage
  cutoff.
- **SC-010**: A documented fixture strategy decision (with rationale and chosen
  mode) is produced and adopted for remaining implementation milestones.

## Strategy Decisions Locked for Implementation

1. **Cypress coverage cutoff is frozen** for migration parity scope.
2. **Anything currently non-skipped in Cypress must pass** in Playwright parity
   before cutover (`M8a` gate).
3. **Full Cypress wiring remains active side by side** through this feature;
   sunset execution is deferred until after migration review.
4. **Fixture strategy is an implementation deliverable**: evaluate options and
   pick a best-practice approach that preserves atomic/independent testing
   behavior without sacrificing runtime performance.
