# Audit Playwright

Use this command to audit Playwright tests and helpers for stability,
maintainability, and adherence to OpenELIS best practices.

## User Input

```text
$ARGUMENTS
```

Interpret input best-effort:

- `/audit-playwright` - audit changed Playwright files
- `/audit-playwright <spec-or-helper>` - audit specific file(s)

## Audit Checklist

1. **Selector quality**

   - Prefer `data-testid` / semantic locators over fragile CSS selectors.
   - Flag broad fallback selector chains and unnecessary `.first()` usage.

2. **Runtime correctness**

   - Ensure assertions align with actual DOM behavior (e.g., input value vs
     text).
   - Ensure waits rely on Playwright assertions/events, not fixed sleeps.

3. **Project integration**

   - Verify new specs are included in `playwright.config.ts` `testMatch`.
   - For demo tests, verify they are included in shared `DEMO_TESTS`.

4. **Debuggability**

   - Ensure tests emit enough context on failure (URL, key state/logs).
   - Confirm helpers keep deterministic behavior.

5. **Repo policy alignment**
   - Use `videoPause()` for demo pacing, not `waitForTimeout()`.
   - Keep tests focused and avoid multi-concern mega-tests when possible.

## Required References

- `.ai/skills/playwright/reference/selector-policy.md`
- `.ai/skills/playwright/reference/debug-workflow.md`
- `.specify/guides/playwright-best-practices.md`
