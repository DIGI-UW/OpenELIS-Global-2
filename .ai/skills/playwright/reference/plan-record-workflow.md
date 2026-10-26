# Plan Record Workflow

## Intent

Provide a consistent process for planning, writing, validating, and recording
Playwright E2E flows with correct project usage.

## 1) Scope Review

Before writing tests, review:

- feature/PR summary
- acceptance criteria
- changed frontend/backend integration points
- existing related Playwright specs/helpers

Output: a flow inventory with explicit test intent.

## 2) Flow Inventory

For each user flow, capture:

- flow name
- page/route
- setup prerequisites
- user actions
- expected user-visible outcomes
- target Playwright project

Keep flows granular. Prefer one behavior per test.

## 3) Project Selection Rules

- `core-app`: core UI behavior without analyzer harness dependencies
- `core-demo` / `core-demo-video`: workflow demos on the build stack only
- `harness`: analyzer infrastructure behavior requiring bridge/simulator/plugins
- `harness-demo`: analyzer story demos (CI)
- `evidence-video`: every `@evidence` story, core or harness, recorded locally

Recordings should run in `core-demo-video` or `evidence-video`; routine
assertions should still pass in non-video projects.

## 4) Author + Register

For each planned flow:

1. Author with `/write-playwright-test`.
2. Register the spec in `frontend/playwright.config.ts` (`testMatch` or
   `CORE_DEMO_TESTS` / `HARNESS_DEMO_TESTS`).
3. Validate with:
   `python3 .ai/skills/playwright/scripts/validate-playwright-project.py <spec>`

## 5) Audit + Execute

1. Run `/audit-playwright`.
2. Execute narrow normal run first (`core-app`, `harness`, or `demo`).
3. If recording required, run `demo-video`.

## 6) Recording Safety Rules

- Use `videoPause(page, ms, testInfo)` for pacing; no-op outside the video
  projects.
- Use `showTitleCard/showStepCard` overlays only through helper APIs so they
  no-op outside the video projects.
- Do not gate assertions on video-only behavior.

## 7) Artifact Verification

For each recorded flow, verify:

- expected `video.webm` exists in `frontend/test-results/`
- no critical console/runtime errors in test output

If failures occur, switch to `/debug-playwright` and diagnose with
screenshot/trace evidence.
