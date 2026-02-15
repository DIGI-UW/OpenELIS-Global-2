# M2 Data Model: Risk Rubric and Parity Mapping

This milestone defines the logical data model used for risk-based parity
tracking between legacy Cypress scenarios and Playwright migration targets.

## Scenario Unit of Record

For this feature, a **scenario record** is one legacy Cypress spec workflow
tracked in `parity-matrix.csv`.

- A scenario may contain multiple `it(...)` test cases.
- Migration phases can later split a scenario into multiple Playwright specs.
- The matrix remains the single source of truth for ownership and status.

## Risk Tiers

### P0 (Critical)

Business-critical patient/order/result/reporting workflows, authentication, and
admin/storage capabilities that can block operations or release quality.

Characteristics:

- User-facing regression has high operational impact.
- Workflow is frequently used or safety/compliance relevant.
- Must be migrated in early milestones (M4a-M6).

### P1 (High)

Important workflows that should reach parity in feature scope, but are lower
immediacy than P0.

Characteristics:

- Impacts productivity or quality confidence.
- Not typically an immediate lab-operational blocker.
- Scheduled after or alongside P0 based on domain milestones.

### P2 (Long Tail)

Lower-impact or specialized flows that may be sequenced later than P0/P1.

Characteristics:

- Limited blast radius.
- May be deferred only with explicit milestone assignment or approved exception.

## Parity Status Values

- `LEGACY_ONLY`: Legacy scenario exists; no Playwright parity yet.
- `GAP`: Coverage gap exists (e.g., mostly skipped/inactive legacy scenario).
- `PARTIAL`: Initial migration exists but parity incomplete (not acceptable as
  final status for P0 at signoff).
- `PASS`: Scenario migrated and parity-verified.
- `EXCEPTION_APPROVED`: Exception documented and accepted.

## Gap Classification Values

- `NONE`: No known structural gap in legacy baseline.
- `SKIP_HEAVY`: Legacy scenario has significant skipped coverage.
- `NO_ACTIVE_LEGACY`: Legacy scenario has no active test case.
- `ASSERTION_REVIEW`: Legacy assertions exist but need quality review.
- `COVERAGE_HOLE`: Workflow risk exists without adequate automation confidence.

## Ownership and Milestone Targeting

- `owner` identifies accountable team role (`qa-e2e` at baseline).
- `milestone_target` aligns migration with plan milestones:
  - `M4a`, `M4b`, `M5`, `M6` for migration waves
  - `M9` for stabilization/parity closure
  - `M10` for final Cypress gate retirement/archival handoff

## Reliability Tracking

The `reliability_id` column links selected baseline-flaky scenarios to
`baseline-flaky-scenarios.md`.

- Reliability SLO target: **>=95% pass rate over 20 CI-equivalent runs**
- IDs use `RLY-###` format.

## Matrix Schema

`parity-matrix.csv` columns:

1. `scenario_id`
2. `risk_tier`
3. `domain`
4. `legacy_spec`
5. `playwright_target_spec`
6. `parity_status`
7. `gap_class`
8. `owner`
9. `milestone_target`
10. `reliability_id`
11. `notes`

## Validation Rules (M2 Gate)

`scripts/e2e/validate-parity-mapping.js` enforces:

- Every active Cypress spec in inventory has one matrix row.
- No duplicate `legacy_spec` rows.
- Every P0/P1 row has required fields populated.
- Optional write mode appends validator pass evidence into `notes` for P0/P1
  rows (T026).
