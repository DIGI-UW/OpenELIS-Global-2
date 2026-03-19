# Selector Policy

Use this priority order:

1. `getByRole(...)` with accessible name
2. `getByLabel(...)` for form fields
3. `locator('[data-testid=\"...\"]')`
4. Controlled text locators where appropriate
5. CSS selectors only as a last resort

## Rules

- Prefer unique, intention-revealing selectors.
- Avoid broad fallback chains such as:
  `input[name=\"x\"], input[placeholder*=\"y\"], input[id*=\"z\"]`
- Avoid blind `.first()` and `.nth(0)`; make uniqueness explicit instead.
- For input values, assert value-oriented state (`toHaveValue`) instead of text
  APIs.
- Use `exact: true` when label ambiguity is possible.

## Wait Strategy

- Use Playwright auto-retrying assertions.
- Use event/response waits for async transitions.
- Avoid `page.waitForTimeout()` except controlled demo pacing handled by
  `videoPause()`.
