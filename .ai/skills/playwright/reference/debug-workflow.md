# Debug Workflow

## 1) Source First

Read in this order before changing selectors:

1. Failing spec file
2. Any page-object/helper invoked by the failing step
3. Relevant UI component source when runtime behavior is unclear

## 2) Runtime Evidence First

Before editing:

1. Inspect screenshot(s) from `test-results/`
2. Inspect trace (`npx playwright show-trace ...`) when available
3. If still ambiguous, run headed/UI mode and inspect DOM state

## 3) Root Cause Statement

State one concise hypothesis from source + runtime evidence:

- Selector mismatch
- Input/value vs text assertion mismatch
- Data setup mismatch
- Navigation/timing race
- Overlay/focus/popup interference

Do not proceed with "guess-and-retry" locator churn.

## 4) Fix Strategy

- Apply the smallest change that resolves the diagnosed root cause.
- Prefer stable semantic/test-id locators.
- If a missing test-id is the real issue, add it in UI code and use it.
- Use explicit waits/assertions over fixed timeouts.

## 5) Validation Strategy

1. Validate project registration:
   `python .ai/skills/playwright/scripts/validate-playwright-project.py <spec>`
2. Run narrow scope:
   `cd frontend && npm run pw:test -- <spec> --project=<project>`
3. Expand only after narrow scope passes.
