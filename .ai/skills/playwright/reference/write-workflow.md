# Write Workflow

## Intent

Use this flow to produce stable Playwright tests on first pass during
implementation/TDD.

## 1) Story To Test Contract

- Convert acceptance criteria into concrete user actions and assertions.
- Keep one main behavior per test. Split unrelated assertions into separate
  tests.
- Prefer failing-first (TDD Red) when feature behavior is not yet implemented.

## 2) Project Selection

Choose target project before writing:

- `core-app`: core UI behavior that does not need analyzer harness infra
- `harness`: analyzer integrations requiring bridge/simulator/plugins
- `demo`: workflow demo tests validated in CI
- `demo-video`: local recording mode using same demo tests

## 3) Source-First Selector Design

- Read the React component and existing fixture/helper code before choosing
  selectors.
- Prefer selector priority from `selector-policy.md`.
- When semantic selectors are insufficient, add explicit `data-testid` in UI
  code instead of fallback CSS chains.

## 4) Author Using Template

- Start from `templates/PlaywrightE2E.spec.ts.template`.
- Preserve `testInfo` + `videoPause()` conventions.
- Add debug-context capture (`frontend/playwright/helpers/debug-context.ts`) for
  async-sensitive paths.

## 5) Registration And Validation

After creating the spec:

1. Register it in `frontend/playwright.config.ts` `testMatch`.
2. If demo flow, include it in `DEMO_TESTS`.
3. Validate registration:
   `python3 .ai/skills/playwright/scripts/validate-playwright-project.py <spec>`
4. Run narrow scope:
   `cd frontend && npm run pw:test -- <spec> --project=<project>`

## 6) Lifecycle Handoff

- Run `/audit-playwright` for quality and selector hardening.
- Use `/debug-playwright` when runtime failures persist after audit.
