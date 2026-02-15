# Research: Full E2E Migration + Fixture Refactor Clarify Pass

**Feature**: `201-e2e-playwright-risk-parity`  
**Date**: 2026-02-15  
**Purpose**: Capture codebase evidence and external best practices used to
update `spec.md` and `plan.md` toward full migration + full E2E battery
refactor.

## 1) Codebase Review Findings

### 1.1 Current parity and coverage posture

- `coverage-inventory.md` shows the suite remains legacy-heavy:
  - Cypress active specs: 47
  - Playwright active specs: 20
- `parity-matrix.csv` shows migration is incomplete:
  - `PARTIAL`: 23
  - `LEGACY_ONLY`: 24
  - `GAP`: 1
- P0 still contains legacy-only risk (`LEG-CYP-045` / `storageSearch.cy.js`),
  which blocks full migration signoff.

### 1.2 Fixture/data architecture signals

- Unified loader exists (`src/test/resources/load-test-fixtures.sh`) and
  supports reset/load/verify behavior, but migration usage is still mixed
  between legacy Cypress helpers and newer Playwright helpers.
- Cypress support commands include duplicate registration of
  `loadStorageFixtures` across:
  - `frontend/cypress/support/commands.js`
  - `frontend/cypress/support/load-storage-fixtures.js`
- Playwright storage fixture loading is opt-in (`PW_LOAD_STORAGE_FIXTURES=true`)
  in `frontend/playwright/fixtures/storage-fixtures.ts`, which is useful for
  safety but needs clear policy to avoid accidental drift across CI/local runs.

### 1.3 Reliability and assertion-depth signals

- `frontend/cypress.config.js` currently sets `testIsolation: false`.
- Some Playwright parity specs still use fallback/early-return behavior when key
  UI controls are absent, which can reduce confidence from "parity pass" to
  "smoke presence" only.
- A skipped Playwright test remains in
  `frontend/playwright/tests/sidenav.spec.ts`.

## 2) External Best-Practice Research (Internet)

## Playwright

### Source: Best Practices

`https://raw.githubusercontent.com/microsoft/playwright/main/docs/src/best-practices-js.md`

- Tests should be isolated and independently runnable.
- Prefer locator-first, user-facing assertions.
- Use web-first assertions (auto-retry) instead of manual/non-waiting checks.
- Use parallelism and sharding to scale CI runtime.

### Source: Authentication

`https://raw.githubusercontent.com/microsoft/playwright/main/docs/src/auth.md`

- Recommended shared-account setup project for non-mutating state.
- Recommended one-account-per-worker model for mutating tests running in
  parallel.
- Store `storageState` under a gitignored auth directory and explicitly manage
  auth strategy by scenario type.

### Source: Sharding

`https://raw.githubusercontent.com/microsoft/playwright/main/docs/src/test-sharding-js.md`

- Use `--shard=x/y` for scalable parallel CI.
- Prefer `fullyParallel: true` for more balanced shard distribution.
- Use `blob` reporter and `merge-reports` for unified multi-shard evidence.

## Cypress

### Source: Cypress Core Concepts pages

`https://docs.cypress.io/app/core-concepts/best-practices`  
`https://docs.cypress.io/app/core-concepts/writing-and-organizing-tests`

- Tests should be independently runnable and state-clean.
- Avoid cross-test coupling and order dependencies.
- Programmatic state control/setup is preferred over brittle UI-only setup for
  test prerequisites.

## 3) Decisions Applied to Updated Specs

1. Replace "indefinite Cypress retention" assumption with a **full migration**
   endpoint that includes a formal Cypress sunset recommendation handoff.
2. Elevate fixture/data management to first-class scope: deterministic contract,
   reset/load/verify policy, and test-created data boundaries.
3. Tighten parity acceptance:
   - P0 cannot close at `PARTIAL`
   - P1 must be `PASS` or approved exception
4. Require scalable Playwright CI evidence: sharding + merged reports +
   classified failure signals.
5. Preserve migration safety via time-boxed dual-run stabilization before
   decommission decision.

## 4) Strategy Clarifications Resolved

See `spec.md` section **Strategy Decisions Locked for Implementation** for the
resolved decisions now governing cutover scope, parity gating, Cypress sunset
timing, and fixture strategy selection.
