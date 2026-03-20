# Write Playwright Test

Use this command to author new Playwright tests using a source-first,
first-time-correct workflow.

## User Input

```text
$ARGUMENTS
```

Interpret input best-effort:

- `/write-playwright-test` - scaffold a new Playwright test from requirements
- `/write-playwright-test <spec-file>` - target/create a specific spec file
- `/write-playwright-test <spec-file> --project <name>` - force target project

## Workflow

1. **Preflight (TDD Red phase)**

   - Confirm target user story / acceptance criteria.
   - Identify feature route and expected user-visible behavior.
   - Decide project target: `core-app`, `core-demo`, `harness`, `harness-demo`,
     or the matching `*-demo-video` for recording.

2. **Read source before writing selectors**

   - Read relevant component source for the page under test.
   - Read existing Playwright fixture/helper patterns to reuse.
   - If selectors are unstable, add/plan `data-testid` support first.

3. **Write from template**

   - Start from
     `.ai/skills/playwright/templates/PlaywrightE2E.spec.ts.template`.
   - Keep `testInfo` and `videoPause()` conventions.
   - Prefer `getByRole` / `getByLabel` / `data-testid` selectors.
   - Add debug-context capture hooks for fragile or async-heavy workflows.

4. **Register spec in Playwright config**

   - Add new spec to the correct `testMatch` project list in
     `frontend/playwright.config.ts`.
   - For demo workflow tests, update `CORE_DEMO_TESTS` or `HARNESS_DEMO_TESTS`.

5. **Validate and run narrow scope**

   - Validate registration:
     `python3 .ai/skills/playwright/scripts/validate-playwright-project.py <spec>`
   - Run the narrowest command:
     `cd frontend && npm run pw:test -- <spec> --project=<project>`

6. **Hand off lifecycle**

   - Run `/audit-playwright` on the authored test before finalizing.
   - If runtime failure remains, switch to `/debug-playwright`.

## Required References

- `.ai/skills/playwright/reference/write-workflow.md`
- `.ai/skills/playwright/reference/selector-policy.md`
- `.ai/skills/playwright/reference/debug-workflow.md`
