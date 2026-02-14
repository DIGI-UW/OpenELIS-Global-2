# Assertion Quality Checklist (M3)

Use this checklist when porting Cypress scenarios to Playwright.

## Required Checks per Migrated Scenario

- [ ] Assertions verify **user-visible outcomes**, not only internal helper
      state.
- [ ] Assertions include at least one **post-action effect** (URL change, table
      row update, toast, persisted value, etc.).
- [ ] Assertions avoid fixed sleeps (`waitForTimeout`) unless documented as a
      temporary stabilization workaround.
- [ ] Assertions are deterministic under CI retry behavior.
- [ ] Assertions validate the expected **business effect** for the workflow (not
      just element presence).

## Recommended Strengthening

- Prefer semantic locators (`getByRole`, `getByLabel`, `getByTestId`) over
  brittle CSS selectors.
- Add negative assertions where relevant (e.g., stale value is not shown).
- Keep each scenario focused; avoid monolithic “everything” tests for parity.
