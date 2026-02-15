# Fixture Strategy Decision Record (M3)

**Feature**: `201-e2e-playwright-risk-parity`  
**Milestone**: `M3`  
**Status**: Selected (M3)

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

## Evaluation Summary

| Option | Independence | Best-Practice Alignment | Runtime Impact | Operational Complexity | Outcome                                        |
| ------ | ------------ | ----------------------- | -------------- | ---------------------- | ---------------------------------------------- |
| A      | High         | High                    | Low            | Medium                 | Rejected: too expensive for dual-run window    |
| B      | Medium       | Medium                  | High           | Low                    | Rejected: not strong enough for mutating flows |
| C      | High         | High                    | Medium-High    | Medium                 | Selected                                       |

## Decision Criteria

- Independence score (test rerun isolation, cross-test contamination risk)
- Best-practice compliance score (Playwright/Cypress + project guidance)
- Runtime impact score (CI wall-clock, shard balance, setup overhead)
- Operational complexity score (maintainability, debugging ergonomics)

## Selected Strategy

- **Selected Option**: Option C (Hybrid by scope)
- **Rationale**:
  - Preserves independent behavior where it matters most (mutating flows).
  - Keeps read/navigation runs fast using verify-and-reuse baseline checks.
  - Aligns with Playwright isolation guidance and Cypress transitional reality.
- **Scope of application**:
  - **Read-only/non-mutating flows**: verify-and-reuse baseline fixtures.
  - **Mutating/stateful flows**: reset-load-verify fixture mode.
  - **Auth model**:
    - Read-only: shared session/account allowed.
    - Mutating parallel flows: worker/account isolation by default.

## Auth Strategy Contract (T053)

- **Playwright default strategy mode**: `hybrid`
- **Playwright read mode**: `shared-session`
- **Playwright mutating mode**: `worker-isolated`
- **Credential resolution order for worker-isolated mode**:
  1. `TEST_USER_WORKER_<index>` / `TEST_PASS_WORKER_<index>`
  2. `TEST_USER_WORKER` / `TEST_PASS_WORKER`
  3. `TEST_USER` / `TEST_PASS` (fallback)
- **Cypress transition mode**:
  - Keep existing session behavior for broad legacy compatibility.
  - Storage setup command accepts mutating flow intent and applies
    reset-load-verify behavior when requested.

## Required Implementation Touchpoints

- `src/test/resources/load-test-fixtures.sh`
- `frontend/cypress.config.js`
- `frontend/cypress/support/storage-setup.js`
- `frontend/cypress/support/load-storage-fixtures.js`
- `frontend/playwright/fixtures/storage-fixtures.ts`
- `frontend/playwright/fixtures/e2e-base.ts`
- `frontend/playwright/tests/*` (where fixture/auth behavior is consumed)

## Validation Checklist

- [ ] Repeat run reliability validated (runtime execution pending environment)
- [ ] No cross-test contamination observed in mutation-heavy flows (pending
      runtime)
- [ ] Runtime remains within agreed budget bounds (tracked in M7 runtime budget
      artifact)
- [x] Behavior documented in quickstart and parity runbook
