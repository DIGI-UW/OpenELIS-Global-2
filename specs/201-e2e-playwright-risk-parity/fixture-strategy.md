# Fixture Strategy Decision Record (M3)

**Feature**: `201-e2e-playwright-risk-parity`  
**Milestone**: `M3`  
**Status**: Pending selection

## Goal

Select a fixture strategy that:

1. Preserves atomic/independent E2E behavior
2. Follows framework/project best practices
3. Does not sacrifice CI/runtime performance

## Candidate Strategies

### Option A: Hard reset + full fixture load every run

- **Isolation**: High
- **Performance**: Lower
- **Risk**: Longer CI wall-clock

### Option B: Verify-and-reuse baseline fixtures (reload only when missing/drifted)

- **Isolation**: Medium-high (if verification is strict)
- **Performance**: High
- **Risk**: Potential hidden drift if verification is weak

### Option C: Hybrid by scope

- Hard reset for stateful/mutating critical flows
- Verify-and-reuse for read-heavy/navigation suites
- **Isolation**: High for mutation-heavy paths
- **Performance**: Medium-high
- **Risk**: More orchestration complexity

## Decision Criteria

- Independence score (test rerun isolation, cross-test contamination risk)
- Best-practice compliance score (Playwright/Cypress + project guidance)
- Runtime impact score (CI wall-clock, shard balance, setup overhead)
- Operational complexity score (maintainability, debugging ergonomics)

## Selected Strategy

- **Selected Option**: _TBD (M3)_
- **Rationale**: _TBD_
- **Scope of application**: _TBD_

## Required Implementation Touchpoints

- `src/test/resources/load-test-fixtures.sh`
- `frontend/cypress.config.js`
- `frontend/cypress/support/*fixture*`
- `frontend/playwright/fixtures/storage-fixtures.ts`
- `frontend/playwright/tests/*` (where fixture behavior is consumed)

## Validation Checklist

- [ ] Repeat run reliability validated
- [ ] No cross-test contamination observed in mutation-heavy flows
- [ ] Runtime remains within agreed budget bounds
- [ ] Behavior documented in quickstart and parity runbook
