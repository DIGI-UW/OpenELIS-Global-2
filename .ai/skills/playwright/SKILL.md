---
name: playwright
description:
  Playwright planning, test authoring, auditing, and debugging skill for
  OpenELIS with source-first investigation, runtime evidence checks, stable
  selector policy, project-registration validation, and project-aware recording
  workflows. Use when planning flows for a feature or PR,
  creating/writing/adding new Playwright tests in TDD red phase, recording demo
  videos correctly, auditing selector quality, or debugging runtime failures.
---

# Playwright Skill

Use this skill when:

- planning one or multiple Playwright flows for a feature or PR
- authoring new Playwright tests
- debugging failing Playwright tests
- auditing existing tests for flaky selectors or anti-patterns

## Primary Entrypoints

- `/plan-record-playwright` - plan scope and flow inventory for one or multiple
  tests, then orchestrate write, audit, run, and recording stages
- `/write-playwright-test` - author new tests from requirements and register
  them correctly in Playwright project allowlists
- `/debug-playwright` - failure diagnosis and remediation workflow
- `/audit-playwright` - quality and maintainability audit workflow

## Lifecycle

For AI-assisted development, prefer this sequence:

1. `/plan-record-playwright`
2. `/write-playwright-test`
3. `/audit-playwright`
4. `/debug-playwright` (only when failures persist)

## Core Non-Negotiables

- Read component/page-object/helper source before changing selectors.
- For failures, inspect screenshot/trace/runtime DOM before changing code.
- Prefer `data-testid`, `getByRole`, and `getByLabel` over CSS fallback chains.
- Avoid `.first()` unless uniqueness is proven and documented.
- Use `videoPause()` for demo pacing and avoid `page.waitForTimeout()` for
  waits.
- Do not create new Cypress tests. Cypress is deprecated; all new E2E work must
  use Playwright.

## Execution Invariants

- Never use `npx playwright test` (or raw `playwright test`) to run tests.
  Always use `npm run pw:test` and related scripts (`pw:test:ui`,
  `pw:test:headed`, `pw:test:video`, `pw:test:harness`, `pw:test:demo`).
- For `harness`, `demo`, and `demo-video` projects, verify analyzer harness
  stack preflight before execution (see `/restart-analyzer-harness`).
- `TEST_USER` and `TEST_PASS` must be set for all Playwright projects.
- Utility exceptions are allowed: `npx playwright show-trace` and
  `npx playwright install`.
- Canonical operational reference: `frontend/playwright/README.md`.

## Run Discipline

1. Validate project registration first:
   `python .ai/skills/playwright/scripts/validate-playwright-project.py <spec>`
2. Run narrow scope first:
   `cd frontend && npm run pw:test -- <spec> --project=<project>`
3. Expand to broader scope only after narrow runs are green.

## Reusable Assets

- Reference: `reference/plan-record-workflow.md`
- Reference: `reference/write-workflow.md`
- Reference: `reference/debug-workflow.md`
- Reference: `reference/selector-policy.md`
- Template: `templates/PlaywrightE2E.spec.ts.template`
- Script: `scripts/validate-playwright-project.py`
