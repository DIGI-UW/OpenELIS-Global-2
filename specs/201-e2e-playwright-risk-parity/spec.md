# Feature Specification: E2E Test Battery Revamp to Playwright with Risk-Based Parity

**Feature Branch**: `[201-e2e-playwright-risk-parity]`  
**Created**: 2026-02-14  
**Status**: Draft  
**Input**: User description: "Migrate E2E to Playwright using risk-based parity, plug critical coverage gaps, do a big-bang cutover while full legacy Cypress remains running for comparison, and do not plan Cypress removal yet."

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Build an Authoritative E2E Coverage Inventory (Priority: P1)

As a QA/engineering lead, I need an accurate and reviewable inventory of current E2E coverage so migration decisions are based on evidence, not assumptions.

**Why this priority**: All migration and parity work depends on a trusted baseline of what Cypress currently validates, what is skipped, and where critical business risk is exposed.

**Independent Test**: Can be tested independently by reviewing generated inventory artifacts and verifying they map all existing Cypress specs, scenarios, and risk tiers.

**Acceptance Scenarios**:

1. **Given** the current Cypress and Playwright suites, **When** the inventory process runs, **Then** every active E2E spec is listed with business domain, scenario count, and execution status.
2. **Given** known skipped or partial Cypress tests, **When** inventory artifacts are produced, **Then** those scenarios are explicitly flagged as coverage gaps with risk classification.
3. **Given** current CI workflows, **When** inventory is generated, **Then** workflow-level execution/sharding coverage is mapped to inventory entries.

---

### User Story 2 - Define and Deliver Risk-Based Parity in Playwright (Priority: P1)

As a release owner, I need Playwright coverage to achieve risk-based parity against legacy E2E behavior so critical workflows are protected during cutover.

**Why this priority**: Migration value is only realized if critical user workflows remain protected or improved; counting files alone is insufficient.

**Independent Test**: Can be tested independently by validating a parity matrix that links high-risk Cypress scenarios to passing Playwright scenarios and identifies remaining non-critical deltas.

**Acceptance Scenarios**:

1. **Given** a risk-tiered parity matrix, **When** P0/P1 workflows are evaluated, **Then** each required legacy scenario has a corresponding passing Playwright scenario (equal or stronger assertion depth).
2. **Given** legacy tests with weak or placeholder assertions, **When** migrated Playwright scenarios are reviewed, **Then** migrated tests use stronger user-visible and real-effect assertions where applicable.
3. **Given** parity reporting, **When** comparison is performed, **Then** unresolved parity gaps are explicitly tracked with owner, risk, and planned remediation milestone.

---

### User Story 3 - Plug Critical Coverage Gaps During Migration (Priority: P1)

As a quality steward, I need critical legacy gaps addressed during migration so the new suite does not inherit known blind spots.

**Why this priority**: Some current tests are skipped, flaky, or incomplete; migration must improve protection, not preserve testing debt.

**Independent Test**: Can be tested independently by showing that critical skipped/partial scenarios now exist as passing Playwright tests with stable execution evidence.

**Acceptance Scenarios**:

1. **Given** critical skipped Cypress scenarios, **When** migration work is completed, **Then** those scenarios are covered by active, passing Playwright tests.
2. **Given** previously flaky legacy scenarios, **When** Playwright replacements are executed repeatedly, **Then** they meet agreed reliability thresholds in CI.
3. **Given** critical workflows with no prior reliable automation, **When** gap-closure scope is delivered, **Then** new Playwright scenarios are added and linked to business risk items.

---

### User Story 4 - Execute Big-Bang Cutover with Legacy Comparison Kept Running (Priority: P2)

As a delivery team, we need to switch primary E2E framework execution to Playwright in a single cutover event while still running full Cypress for side-by-side comparison.

**Why this priority**: The user-requested migration style is big-bang cutover for primary framework usage, but with legacy comparison retained to reduce uncertainty.

**Independent Test**: Can be tested independently by CI showing Playwright as primary E2E gate and full Cypress still running and reported in parallel.

**Acceptance Scenarios**:

1. **Given** cutover readiness criteria are met, **When** the big-bang cutover is applied, **Then** Playwright becomes the primary E2E execution framework in CI and local guidance.
2. **Given** comparison mode is required, **When** pull requests run, **Then** full Cypress battery also executes and publishes comparable results.
3. **Given** temporary result divergence between frameworks, **When** CI completes, **Then** parity reports identify divergence location and severity without silently masking failures.

---

### User Story 5 - Retain Cypress Suite Without Decommission Plan (Priority: P2)

As stakeholders, we want to keep Cypress tests intact and running with no immediate retirement plan while migration confidence is established.

**Why this priority**: The current directive is explicit: do not plan Cypress removal yet; maintain legacy evidence and comparison capability.

**Independent Test**: Can be tested independently by confirming Cypress specs remain in repository, remain runnable, and remain included in CI after Playwright cutover.

**Acceptance Scenarios**:

1. **Given** migration implementation, **When** repository and workflows are reviewed, **Then** Cypress test assets and supporting utilities are preserved.
2. **Given** post-cutover CI execution, **When** E2E checks run, **Then** Cypress still executes as a full legacy comparison battery.
3. **Given** project planning artifacts, **When** roadmap is reviewed, **Then** no Cypress decommission milestone is included in this feature scope.

---

### Edge Cases

- How are parity decisions handled when a legacy Cypress scenario is green but has weak assertions that do not verify true business effect?
- What happens when Playwright and Cypress disagree for the same workflow in the same commit (e.g., one passes and one fails)?
- How is parity judged for legacy tests marked `skip`, commented out, or known flaky?
- How are environment/setup failures (not test assertion failures) separated from real parity regressions?
- How is risk tiering handled for very large, monolithic legacy specs that contain many loosely related scenarios?

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: The project MUST produce an authoritative E2E coverage inventory artifact that maps all current Cypress and Playwright scenarios by domain and risk tier.
- **FR-002**: The project MUST define a risk-based parity model (at minimum P0/P1/P2 tiers) and use it as the acceptance basis for migration.
- **FR-003**: The project MUST map each high-risk (P0/P1) Cypress scenario to a corresponding Playwright scenario with explicit parity status.
- **FR-004**: The project MUST prioritize and implement closure of critical coverage gaps (including currently skipped/partial critical scenarios) as part of migration scope.
- **FR-005**: The migration MUST support a big-bang cutover event where Playwright becomes the primary E2E framework gate.
- **FR-006**: After cutover, CI MUST continue to run the full legacy Cypress battery for comparison and divergence monitoring.
- **FR-007**: This feature MUST NOT include Cypress decommissioning or Cypress test removal from repository/workflow scope.
- **FR-008**: The project MUST generate parity comparison output per run, including scenario-level pass/fail divergence and risk impact.
- **FR-009**: Any unresolved parity gaps after cutover MUST be tracked with owner, risk classification, and remediation target.
- **FR-010**: Migration documentation MUST include local and CI execution guidance for dual-framework comparison mode.

### Constitution Compliance Requirements (OpenELIS Global)

- **CR-001**: E2E strategy MUST comply with Constitution Section V.5 workflow: focused development execution plus mandatory full-suite pre-push validation.
- **CR-002**: New UI-facing tests MUST use stable selectors and user-visible assertions aligned with project testing roadmap and Playwright guidance.
- **CR-003**: Test changes MUST preserve realistic end-to-end behavior expectations (real browser, real backend, real database for tests classified as E2E).
- **CR-004**: CI/CD changes MUST maintain clear, reviewable evidence artifacts (logs, screenshots/traces on failure, parity output) for debugging and auditability.
- **CR-005**: Framework policy MUST remain consistent with constitution and testing roadmap: Playwright preferred for new development, Cypress retained per current scope decision.

### Key Entities _(include if feature involves data)_

- **Coverage Inventory Entry**: Scenario-level record containing source suite, domain, risk tier, execution status, and artifact references.
- **Parity Mapping Entry**: Relationship between one legacy Cypress scenario and one or more Playwright scenarios, including parity verdict and notes.
- **Critical Gap Item**: Identified high-risk coverage deficiency with impact, owner, and remediation status.
- **Cutover Readiness Gate**: Aggregated criteria object defining required parity/risk conditions to perform big-bang primary-framework switch.
- **Dual-Run Comparison Report**: Per-run output showing Cypress vs Playwright outcomes, divergences, and risk-severity annotations.

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: 100% of active Cypress specs are represented in the inventory with domain and risk classification.
- **SC-002**: 100% of P0/P1 legacy scenarios have mapped Playwright parity status (pass/gap/blocked), with no unmapped high-risk items.
- **SC-003**: All critical (P0/P1) coverage gaps identified at baseline are either closed with passing Playwright tests or formally documented as approved exceptions.
- **SC-004**: Big-bang cutover is completed with Playwright as primary E2E gate while full Cypress suite remains enabled and green in comparison mode.
- **SC-005**: For an agreed stabilization window, parity comparison reports are produced on each CI run and contain no untriaged high-risk divergences.
