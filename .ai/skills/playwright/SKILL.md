---
name: playwright
description:
  Playwright test authoring, auditing, and debugging skill for OpenELIS with
  source-first investigation, runtime evidence checks, stable selector policy,
  and project-registration validation.
---

# Playwright Skill

Use this skill when:

- authoring new Playwright tests
- debugging failing Playwright tests
- auditing existing tests for flaky selectors or anti-patterns

## Primary Entrypoints

- `/debug-playwright` - failure diagnosis and remediation workflow
- `/audit-playwright` - quality and maintainability audit workflow

## Core Non-Negotiables

- Read component/page-object/helper source before changing selectors.
- For failures, inspect screenshot/trace/runtime DOM before changing code.
- Prefer `data-testid`, `getByRole`, and `getByLabel` over CSS fallback chains.
- Avoid `.first()` unless uniqueness is proven and documented.
- Use `videoPause()` for demo pacing and avoid `page.waitForTimeout()` for
  waits.

## Reusable Assets

- Reference: `reference/debug-workflow.md`
- Reference: `reference/selector-policy.md`
- Template: `templates/PlaywrightE2E.spec.ts.template`
- Script: `scripts/validate-playwright-project.py`
