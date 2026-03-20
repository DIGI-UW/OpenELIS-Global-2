# Playwright E2E Quality Report

> **Date:** 2026-03-20
> **Scope:** All files in `frontend/playwright/` (tests, helpers, fixtures)
> **Purpose:** Identify anti-patterns, establish modern best practices, and
> recommend tooling improvements for OpenELIS Playwright E2E tests.

---

## Anti-Patterns Found in OpenELIS Tests

### 1. `waitForResponse` as Primary Assertion

**File:** `frontend/playwright/helpers/accept-results.ts` (lines 68-92)

The `acceptAndVerifyResults` helper uses `page.waitForResponse()` to intercept
the POST to `/rest/AnalyzerResults` and then checks `saveResponse.ok()` as the
primary success signal. While the helper does follow up with UI assertions
(waiting for the page to reload and verifying staged rows disappear), the
`waitForResponse` acts as the critical gate -- if the server returns 500, the
helper throws a programmatic error with body text rather than letting the UI
failure manifest naturally.

```typescript
// accept-results.ts lines 68-92
const saveResponsePromise = page.waitForResponse(
  (res) =>
    res.url().includes("/rest/AnalyzerResults") &&
    res.request().method() === "POST",
  { timeout: 60_000 },
);
await saveButton.click();
// ... then checks saveResponse.ok() and throws if not
```

**Why it matters:** When the backend returns HTTP 500, this throws a generic
error before the UI has a chance to render an error notification. CI screenshots
capture a stale page state, making diagnosis impossible.

**Better approach:** Use `waitForResponse` purely for synchronization (knowing
the POST completed), then assert on the visible success/error UI state. Let the
UI tell the story.

### 2. Autocomplete Polling with 12s Timeout

**File:** `frontend/playwright/tests/ogc-284-barcode-workflow.spec.ts` (lines
18-32)

The `pickFirstAutosuggestOptional` function uses `expect.poll()` with a 12-second
timeout and custom intervals to wait for autocomplete dropdown suggestions to
appear, then clicks the first one. If the dropdown never appears, it falls back
to pressing Tab.

```typescript
// ogc-284-barcode-workflow.spec.ts lines 18-32
async function pickFirstAutosuggestOptional(page: Page, pause: PauseFn) {
  try {
    await expect
      .poll(async () => page.locator('[data-cy="auto-suggestion"]').count(), {
        timeout: 12_000,
        intervals: [400, 800, 1500],
      })
      .toBeGreaterThan(0);
    await page.locator('[data-cy="auto-suggestion"]').first().click();
  } catch {
    await page.keyboard.press("Tab");
  }
}
```

**Why it matters:** This is called twice in `fillOrderDetails` (for site name and
requester lookup), adding up to 24 seconds of potential polling time per order.
The `[data-cy="auto-suggestion"]` selector is a Cypress-era attribute being used
in Playwright tests. The try/catch silently swallows failures, masking real
issues.

**Better approach:** For known values in E2E fixtures, type the full value and
press Tab. Autocomplete is a UX convenience, not the interaction path tests
should exercise.

### 3. `{ force: true }` Bypassing Actionability Checks

Multiple files use `{ force: true }` on click and check operations:

| File | Line | Usage |
|------|------|-------|
| `ogc-284-barcode-workflow.spec.ts` | 163 | `await firstRadio.check({ force: true })` |
| `ogc-284-barcode-workflow.spec.ts` | 199 | `await genderMale.check({ force: true })` |
| `ogc-284-labels-ui.spec.ts` | 9 | `.click({ force: true })` |
| `accept-results.ts` | 53 | `await acceptAllCheckbox.check({ force: true })` |
| `barcode-configuration.spec.ts` | 60 | `await collectionDateCheckbox.setChecked(false, { force: true })` |

**Why it matters:** `{ force: true }` bypasses Playwright's actionability checks
(visibility, enabled state, not intercepted by overlay). This means the test may
"pass" even when the element is not actually interactable by a real user. In the
Carbon Design System, checkboxes and radio buttons have overlaid label elements
that intentionally receive pointer events -- `force: true` works around this but
masks real actionability regressions.

**Better approach:** Click the associated `<label>` element instead of forcing
the hidden `<input>`. For Carbon checkboxes/radios, this is the correct user
interaction path.

### 4. No Assertions in Demo/Video Test Steps

Several demo tests contain long sequences of UI interactions with no `expect()`
assertions. For example:

**File:** `ogc-284-barcode-workflow.spec.ts` -- "US3" test (lines 597-713)

Large portions of US3 are purely navigational with no assertions:
- Lines 651-665: Scrolls to print dialog elements, checks visibility with `.catch(() => false)`, but never asserts on outcome
- Lines 676-679: Checks for Done button visibility with `.catch(() => false)` but no assertion
- Lines 693-703: Navigates to Print Barcode page and scrolls around with no assertions

**Why it matters:** A test without assertions is a "fire-and-forget" navigation
script. It provides no regression protection. If the feature breaks, the test
still passes.

**Better approach:** Even demo tests should have at least one meaningful
assertion per logical step using `test.step()` annotations.

### 5. Manual POM Construction Instead of Fixture Injection

Most tests that use Page Objects construct them manually:

```typescript
// Common pattern across tests
const list = new AnalyzerListPage(page);
const form = new AnalyzerFormPage(page);
```

While some tests use POMs well (analyzer-list.spec.ts, analyzer-form.spec.ts,
error-dashboard.spec.ts), others bypass POMs entirely and use raw locators
(ogc-284-barcode-workflow.spec.ts, file-import-ui.spec.ts, navbar.spec.ts).

**Why it matters:** Manual construction is boilerplate-heavy and error-prone.
Playwright's `test.extend()` fixture system provides automatic dependency
injection, proper setup/teardown lifecycle, and composability.

**Better approach:** Define POMs as Playwright fixtures:

```typescript
import { test as base } from '@playwright/test';
const test = base.extend<{ analyzerList: AnalyzerListPage }>({
  analyzerList: async ({ page }, use) => {
    const list = new AnalyzerListPage(page);
    await use(list);
  },
});
```

### 6. Pure API Tests Without UI Interaction

**File:** `frontend/playwright/tests/file-import.spec.ts` (entire file)

This test file contains three test groups that exclusively use `page.request.*`
API calls with zero UI interactions. No page navigation, no DOM assertions.

```typescript
// file-import.spec.ts lines 48-69 (representative)
const listRes = await page.request.get(`${api}/analyzers`);
expect(listRes.ok()).toBeTruthy();
const { analyzers } = (await listRes.json()) as { ... };
const found = analyzers.find((a) => a.name === analyzer.name);
expect(found).toBeDefined();
```

**Why it matters:** These are API integration tests masquerading as E2E tests.
They inflate E2E suite runtime without testing any user-facing behavior. They
belong in backend integration tests or a separate API test suite.

### 7. Catching and Ignoring Errors

Multiple files use `.catch(() => false)` or `.catch(() => {})` to silently
swallow errors:

| File | Line | Pattern |
|------|------|---------|
| `accept-results.ts` | 82 | `}).catch(() => { /* Some runs complete quickly */ })` |
| `ogc-284-barcode-workflow.spec.ts` | 84 | `.isVisible({ timeout: 2_000 }).catch(() => false)` |
| `ogc-284-barcode-workflow.spec.ts` | 124 | `.isVisible({ timeout: 3000 }).catch(() => false)` |
| `ogc-284-barcode-workflow.spec.ts` | 188 | `.isVisible({ timeout: 5000 }).catch(() => false)` |
| `ogc-284-barcode-workflow.spec.ts` | 197-198 | `.isChecked().catch(() => false)` |
| `ogc-284-barcode-workflow.spec.ts` | 199 | `.isVisible().catch(() => false)` |
| `ogc-284-barcode-workflow.spec.ts` | 205 | `.isVisible().catch(() => false)` |
| `analyzer-plugin-config.spec.ts` | 32 | `.catch(() => {})` |
| `analyzer-plugin-config.spec.ts` | 60 | `.catch(() => {})` |

**Why it matters:** Silent error swallowing creates invisible failures. When a
DOM element is unexpectedly missing, the test silently skips the interaction
instead of failing. This means broken features can pass CI green.

### 8. `page.waitForTimeout()` in Non-Video Helpers

**File:** `frontend/playwright/helpers/title-card.ts` (lines 63, 107)

The `showTitleCard` and `showStepCard` functions use `page.waitForTimeout()` for
their display duration. While these are no-op outside video projects (gated by
`isVideoProject`), the pattern normalizes `waitForTimeout` usage.

**File:** `frontend/playwright/helpers/results-ui.ts` (lines 78-106)

The `navigateUntilVisible` function implements a retry loop that navigates to a
URL and waits for a locator to become visible, retrying on failure. This is a
polling pattern that works around timing issues rather than using proper
Playwright auto-waiting.

### 9. Network Interception Used as Assertion

**File:** `frontend/playwright/tests/barcode-configuration.spec.ts` (lines 28-45)

This test intercepts `**/rest/BarcodeConfiguration` to mock the API, then
asserts on the mock's state. While API mocking is a valid technique, this test
never verifies the actual network call happened -- it only verifies the mock
was set up correctly.

---

## Modern Best Practices (2025-2026)

### 1. Use `waitForResponse` ONLY for Synchronization

`waitForResponse` should synchronize test flow (wait for a network call to
complete before proceeding), never serve as the primary assertion. Always follow
with a visible UI state assertion.

```typescript
// Synchronize on network completion
const responsePromise = page.waitForResponse('**/api/save');
await saveButton.click();
await responsePromise; // Wait for network, but don't assert on it

// Assert on visible UI state
await expect(page.getByText('Saved successfully')).toBeVisible();
```

### 2. Type Full Text + Tab for Known Values

For autocomplete fields with known test data values, type the complete value and
Tab out. Never poll for autocomplete dropdown suggestions in tests.

```typescript
// BAD: Poll for autocomplete dropdown
await input.fill("CAMES");
await expect.poll(() => page.locator('.suggestion').count()).toBeGreaterThan(0);
await page.locator('.suggestion').first().click();

// GOOD: Type full value and move on
await input.fill("CAMES MAN");
await input.press("Tab");
```

### 3. Use Web-First Assertions

Playwright's `expect()` assertions auto-retry. Never use one-shot checks like
`isVisible()` for assertions.

```typescript
// BAD: One-shot check (no retry)
const visible = await element.isVisible();
expect(visible).toBeTruthy();

// GOOD: Web-first assertion (auto-retries)
await expect(element).toBeVisible();
```

### 4. Use `test.step()` for Multi-Step Workflows

Instead of long sequential test bodies, use `test.step()` to create named
sections. This improves trace readability and failure diagnostics.

```typescript
test('complete order workflow', async ({ page }) => {
  await test.step('Select patient', async () => {
    // ...
  });

  await test.step('Fill sample details', async () => {
    // ...
  });

  await test.step('Submit order', async () => {
    // ...
  });
});
```

### 5. API-First Test Data Setup

Use REST API calls in `beforeAll` or fixtures to create test data instead of
navigating through the UI. This is 10x faster and more reliable.

```typescript
test.beforeAll(async ({ request }) => {
  await request.post('/api/test-data/patients', {
    data: { lastName: 'TEST-Smith', firstName: 'John' },
  });
});
```

### 6. ARIA Snapshot Testing for Carbon Forms

Playwright 1.49+ supports ARIA snapshot testing, which captures the accessible
tree of a component. This is ideal for Carbon Design System forms where the
accessible structure is well-defined.

```typescript
await expect(page.getByTestId('order-form')).toMatchAriaSnapshot(`
  - form "Add Order":
    - textbox "Patient Name"
    - combobox "Sample Type"
    - button "Submit"
`);
```

### 7. Soft Assertions for Transient Notifications

Use soft assertions with custom error messages for elements like toast
notifications that may disappear quickly.

```typescript
await expect.soft(
  page.getByRole('alert'),
  'Success notification should appear after save'
).toBeVisible({ timeout: 10_000 });
```

---

## Tooling Recommendations

### 1. `eslint-plugin-playwright`

**Status:** Not installed (confirmed by checking `frontend/package.json`).

**Install:**

```bash
cd frontend && npm install -D eslint-plugin-playwright
```

**Configure** (add to `.eslintrc` or create `frontend/.eslintrc.playwright.js`):

```javascript
module.exports = {
  extends: ['plugin:playwright/recommended'],
  rules: {
    'playwright/no-wait-for-timeout': 'error',
    'playwright/prefer-web-first-assertions': 'warn',
    'playwright/no-force-option': 'warn',
    'playwright/no-page-pause': 'error',
    'playwright/expect-expect': 'warn',
    'playwright/no-conditional-expect': 'warn',
    'playwright/no-conditional-in-test': 'warn',
  },
};
```

**Key rules:**

| Rule | Purpose | Current Violations |
|------|---------|-------------------|
| `no-wait-for-timeout` | Prevents `page.waitForTimeout()` | title-card.ts (2), video-pause.ts (1), results-ui retry loop |
| `prefer-web-first-assertions` | Enforces auto-retrying assertions | Multiple `.isVisible()` one-shot checks |
| `no-force-option` | Flags `{ force: true }` | 5 instances across 3 files |
| `expect-expect` | Requires assertions in tests | Demo test steps with no expects |
| `no-conditional-expect` | Flags `expect` inside try/catch | accept-results.ts, analyzer-test-connection.spec.ts |

### 2. Playwright MCP Server

Enable AI-assisted test development with the Playwright MCP server:

```bash
claude mcp add playwright npx @playwright/mcp@latest
```

This provides the LLM with direct browser interaction capabilities for
debugging and understanding DOM state during test development.

### 3. Playwright Test Agents

Playwright's experimental test agent support (planner/generator/healer pattern):

```bash
npx playwright init-agents --loop=claude
```

This enables three-phase test generation:
1. **Planner**: Analyzes the page and creates a test plan
2. **Generator**: Writes the test code from the plan
3. **Healer**: Fixes broken selectors automatically on failure

### 4. Flakiness Detection

Before merging any new or modified Playwright test, run with `--repeat-each=3`:

```bash
cd frontend && npm run pw:test -- --repeat-each=3 --project=core-app
```

This catches flaky tests before they enter the CI pipeline.

---

## Guardrail Document Audit

### CLAUDE.md

**Current Playwright guidance (lines 105-109):**
- Points to AGENTS.md for full details
- Mentions `npm run pw:test` invariant
- No anti-pattern warnings

**Gaps:**
- No mention of the top 3 anti-patterns to avoid
- No pointer to `.specify/guides/playwright-best-practices.md`
- No mention of `eslint-plugin-playwright`

### AGENTS.md

**Current Playwright section (lines 1543-1685):**
- Comprehensive project structure documentation
- Good CI workflow documentation
- Good local execution examples
- Lists `videoPause()` pattern

**Gaps:**
- No anti-pattern list specific to Playwright (only has Cypress anti-patterns)
- No mention of `waitForResponse` misuse
- No mention of `{ force: true }` risks
- No mention of autocomplete polling anti-pattern
- No mention of `eslint-plugin-playwright`
- Section V.5 of constitution references Playwright but anti-patterns are
  generic (not Playwright-specific)

### `.specify/guides/playwright-best-practices.md`

**Current content:**
- Good selector strategy table
- Good POM pattern
- Good authentication strategy
- Anti-patterns table at line 499-510

**Gaps:**
- Anti-pattern table is generic (copied from Playwright docs)
- No mention of `waitForResponse` misuse pattern
- No mention of autocomplete polling
- No mention of `{ force: true }` for Carbon checkboxes
- No mention of `test.step()` for workflow tests
- No API-first test data setup guidance
- No ARIA snapshot testing guidance
- No soft assertion guidance for transient notifications
- No `eslint-plugin-playwright` integration
- Template in "Authentication Strategy" section shows outdated UI-based login
  (the actual auth.setup.ts uses API-based login -- docs should match)

### `.ai/skills/playwright/SKILL.md`

**Current content:**
- Good lifecycle sequence
- Good non-negotiables list
- Good execution invariants

**Gaps:**
- No anti-pattern list
- No mention of `waitForResponse` vs UI assertion pattern
- No mention of `{ force: true }` risks

### `.ai/skills/playwright/commands/*.md`

**audit-playwright.md:**
- Has 5-point checklist but all are generic
- No specific anti-patterns to flag
- No mention of `waitForResponse`, `force: true`, or autocomplete polling

**write-playwright-test.md:**
- Good source-first workflow
- No mention of avoiding `waitForResponse` as assertion
- No mention of avoiding autocomplete polling
- No mention of `test.step()` for workflows

**debug-playwright.md:**
- Good source-first, evidence-first workflow
- No mention of checking for `waitForResponse` masking real failures

**plan-record-playwright.md:**
- Good planning workflow
- No quality criteria for what makes a test "ready to record"

### `.ai/skills/playwright/reference/*.md`

**selector-policy.md:**
- Good selector priority order
- Good wait strategy rules
- No mention of `{ force: true }` as a selector-adjacent concern

**write-workflow.md:**
- Good 6-step workflow
- No anti-pattern checklist at the end

**debug-workflow.md:**
- Good 5-step diagnostic workflow
- No mention of `waitForResponse` masking failures

### `.ai/skills/playwright/templates/PlaywrightE2E.spec.ts.template`

**Current content:**
- Good debug context capture pattern
- Good `videoPause()` usage

**Gaps:**
- Uses `console`/`pageerror` listeners that are flagged as inappropriate for
  demo specs in playwright-best-practices.md -- template should have a note
- No `test.step()` example for multi-step workflows
- No example of assertion-only pattern (no `waitForResponse` example to copy)

---

## Summary of Findings

| Category | Count | Severity |
|----------|-------|----------|
| `waitForResponse` as primary assertion | 1 file (accept-results.ts) | High -- masks backend failures |
| Autocomplete polling (12s timeout) | 1 file (ogc-284-barcode-workflow.spec.ts) | High -- adds 24s flaky wait |
| `{ force: true }` bypasses | 5 instances in 3 files | Medium -- masks actionability issues |
| Tests with no assertions | Parts of 3 demo tests | Medium -- no regression protection |
| Silent `.catch(() => false/{})`  | 15+ instances in 4 files | Medium -- invisible failures |
| Pure API tests in E2E suite | 1 file (file-import.spec.ts) | Low -- wrong test type |
| Manual POM construction | 10+ test files | Low -- boilerplate |
| `page.waitForTimeout()` in helpers | 3 helpers (title-card, video-pause, results-ui) | Low -- most are video-only |
