# Debug Playwright

Use this command to debug failing Playwright tests in a source-first,
evidence-first workflow.

## User Input

```text
$ARGUMENTS
```

Interpret input best-effort:

- `/debug-playwright` - diagnose current failing test context
- `/debug-playwright <spec-file>` - target a specific spec
- `/debug-playwright <spec-file> --project <name>` - target specific project

## Workflow

1. **Read source before edits**

   - Read the failing spec
   - Read any page object/helper methods it calls
   - Read relevant UI component source when selector behavior is unclear

2. **Gather runtime evidence**

   - Review failure screenshot(s) under `frontend/test-results/`
   - Open Playwright trace if available
   - Use headed or UI mode if runtime state is still ambiguous

3. **Diagnose before trying alternatives**

   - State root cause hypothesis from source + runtime evidence
   - Confirm whether failure is selector, timing, data, or navigation
   - Do not iterate through locator syntax guesses blindly

4. **Apply minimal fix**

   - Prefer stable `data-testid`/semantic locator updates
   - Replace brittle CSS fallback selectors where possible
   - Avoid `.first()` unless there is no stable unique selector

5. **Validate**
   - Run the narrowest command first (single spec + project)
   - Verify project registration with:
     `python .ai/skills/playwright/scripts/validate-playwright-project.py <spec>`
   - Re-run broader scope only after targeted pass

## Required References

- `.ai/skills/playwright/reference/debug-workflow.md`
- `.ai/skills/playwright/reference/selector-policy.md`
