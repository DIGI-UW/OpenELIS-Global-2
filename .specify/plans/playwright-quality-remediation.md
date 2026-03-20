# Playwright Quality Remediation Plan

> **Date:** 2026-03-20 **Status:** DRAFT **Research Report:** >
> `.specify/guides/playwright-e2e-quality-report.md` > **Scope:** Prevent new
> anti-patterns, fix existing ones, improve architecture

---

## Phase 1: Guardrails (Prevent New Anti-Patterns)

### 1.1 CLAUDE.md Additions

Add the following section after the existing "Playwright E2E -- RECOMMENDED"
block (after line 109):

```markdown
### Playwright Anti-Patterns (CRITICAL)

**Top 3 anti-patterns that cause flaky tests -- MUST AVOID:**

1. **`waitForResponse` as assertion** -- Use it for sync only, then assert on
   visible UI state (`toBeVisible`, `toBeEnabled`, `toHaveText`)
2. **Autocomplete polling** -- Type the full known value + Tab; never poll for
   dropdown suggestions with `expect.poll()`
3. **`{ force: true }`** -- Click the `<label>` instead of forcing hidden Carbon
   inputs; `force` masks real actionability regressions

**Full guide:** `.specify/guides/playwright-best-practices.md` **Quality
report:** `.specify/guides/playwright-e2e-quality-report.md`
```

### 1.2 AGENTS.md Additions

Add the following block inside the "E2E Tests (Playwright) -- RECOMMENDED"
section, after the "Key Patterns" subsection (after line 1606):

```markdown
#### Playwright Anti-Patterns (MUST AVOID)

These patterns cause flaky tests and invisible failures:

- **`waitForResponse` as primary assertion** -- Use `waitForResponse` for
  synchronization only. Always follow with a visible UI assertion
  (`toBeVisible`, `toHaveText`). Never check `response.ok()` as the pass/fail
  signal.
- **Autocomplete polling** -- For known fixture values, type the full value +
  Tab. Never use `expect.poll()` to wait for autocomplete dropdowns.
- **`{ force: true }`** -- Carbon overlays labels on hidden inputs. Click the
  `<label>` element instead of forcing the hidden `<input>`. `force` bypasses
  actionability checks and masks real regressions.
- **Silent `.catch(() => {})`** -- Never swallow errors silently. If an element
  may not exist, use conditional logic with `isVisible()` followed by an
  explicit code path, not a blanket catch.
- **Tests without assertions** -- Every test (including demo tests) must have at
  least one `expect()` call. Use `test.step()` for multi-step workflows.

**Full guide:** `.specify/guides/playwright-best-practices.md`
```

### 1.3 Updates to `.specify/guides/playwright-best-practices.md`

Add the following sections to the best practices guide:

**A) Replace the "Anti-Patterns" table (line 499-510) with an expanded
version:**

````markdown
## Anti-Patterns

### Critical Anti-Patterns (Cause Flaky Tests)

| Anti-Pattern                       | Why It Fails                                                     | Fix                                 |
| ---------------------------------- | ---------------------------------------------------------------- | ----------------------------------- |
| `waitForResponse` as assertion     | Backend 500 throws before UI renders error notification          | Use for sync only, assert on UI     |
| `expect.poll()` for autocomplete   | 12s polling adds flakiness; dropdown timing is non-deterministic | Type full value + Tab               |
| `{ force: true }` on Carbon inputs | Bypasses actionability; masks overlay/focus regressions          | Click the `<label>` element         |
| `.catch(() => false)` silently     | Swallows real failures; broken features pass green               | Use explicit conditional paths      |
| No `expect()` in test body         | Test provides zero regression protection                         | Add at least one assertion per step |

### Structural Anti-Patterns

| Anti-Pattern                   | Fix                                                        |
| ------------------------------ | ---------------------------------------------------------- |
| `page.waitForTimeout(ms)`      | Use auto-retrying `expect()` assertions                    |
| Manual POM construction        | Use `test.extend()` fixture injection                      |
| Pure API tests in E2E suite    | Move to backend integration tests                          |
| Long tests with many concerns  | Split into focused single-concern tests with `test.step()` |
| Hardcoded credentials          | Use `TEST_USER`/`TEST_PASS` environment variables          |
| CSS class assertions for state | Use ARIA role/attribute assertions                         |

### Correct `waitForResponse` Pattern

```typescript
// Synchronize on network, then assert on UI
const responsePromise = page.waitForResponse("**/api/save");
await saveButton.click();
await responsePromise; // sync only -- do not check .ok()

// This is the real assertion
await expect(page.getByText("Saved successfully")).toBeVisible();
```
````

### Correct Autocomplete Pattern

```typescript
// For known test data values -- type and move on
await siteInput.fill("CAMES MAN");
await siteInput.press("Tab");
// Assert the field retained its value
await expect(siteInput).toHaveValue("CAMES MAN");
```

### Correct Carbon Checkbox Pattern

```typescript
// Click the label, not the hidden input
// Carbon checkbox: click the visible label, not the hidden input
const label = page.locator('label[for="saveallresults"]');
await label.click();
```

````

**B) Add a "Multi-Step Workflow Pattern" section:**

```markdown
## Multi-Step Workflow Pattern

Use `test.step()` to organize long workflows. Each step should have at least one
assertion.

```typescript
test('complete sample order workflow', async ({ page }) => {
  await test.step('Search and select patient', async () => {
    await page.goto('/SamplePatientEntry');
    await page.locator('input#lastName').fill('TEST-Smith');
    await page.locator('button#local_search').click();
    await expect(page.locator('[data-cy="radioButton"]').first()).toBeVisible();
  });

  await test.step('Fill sample details', async () => {
    // ... fill sample type, tests, etc.
    await expect(page.getByRole('button', { name: 'Next' })).toBeEnabled();
  });

  await test.step('Submit and verify success', async () => {
    await page.getByRole('button', { name: 'Submit' }).click();
    await expect(page.locator('.orderEntrySuccessMsg')).toBeVisible();
  });
});
````

````

**C) Add "Soft Assertions for Transient UI" section:**

```markdown
## Soft Assertions for Transient Notifications

Toasts and notifications may disappear before assertions run. Use soft
assertions with descriptive messages:

```typescript
await expect.soft(
  page.getByRole('alert'),
  'Success notification should appear after save'
).toBeVisible({ timeout: 10_000 });
````

````

**D) Update the Authentication Strategy section** to match the actual
`auth.setup.ts` implementation (API-based login, not UI-based).

### 1.4 Updates to `.ai/skills/playwright/` Files

**A) `SKILL.md` -- Add to "Core Non-Negotiables" list:**

```markdown
- Never use `waitForResponse` as the primary pass/fail signal. Use it for sync,
  assert on visible UI.
- Never poll for autocomplete dropdowns. Type the full known value and Tab.
- Never use `{ force: true }` on Carbon inputs. Click the label instead.
- Every test must contain at least one `expect()` assertion.
````

**B) `commands/audit-playwright.md` -- Add to Audit Checklist:**

```markdown
6. **Anti-pattern detection**
   - Flag `waitForResponse` used as primary assertion (checking `.ok()` or
     `.status()` as pass/fail)
   - Flag `expect.poll()` for autocomplete/dropdown waiting
   - Flag `{ force: true }` on input/checkbox/radio elements
   - Flag tests with no `expect()` calls
   - Flag `.catch(() => false)` or `.catch(() => {})` patterns
```

**C) `commands/write-playwright-test.md` -- Add to step 3 "Write from
template":**

```markdown
- Never use `waitForResponse` as assertion; use for sync only
- For autocomplete fields, type full value + Tab (never poll for suggestions)
- For Carbon checkboxes/radios, click the label (never `{ force: true }`)
- Use `test.step()` for multi-step workflows
```

**D) `reference/selector-policy.md` -- Add "Actionability" section:**

```markdown
## Actionability Rules

- Never use `{ force: true }` unless documented and justified.
- For Carbon `<input type="checkbox">` and `<input type="radio">`: click the
  associated `<label>` element (e.g., `label[for="inputId"]`). Do NOT use
  `getByLabel().check()` — Carbon hides inputs with `visually-hidden`.
- For Carbon `ComboBox` / `Dropdown`: click the trigger button
  (`button[role="combobox"]` or `.cds--list-box__field`), not the wrapper div.
```

**E) `templates/PlaywrightE2E.spec.ts.template` -- Add `test.step()` example:**

Add a commented example of `test.step()` usage and a note about when to use
console/pageerror listeners (non-demo tests only).

### 1.5 `eslint-plugin-playwright` Installation

**Install:**

```bash
cd frontend && npm install -D eslint-plugin-playwright
```

**Create** `frontend/.eslintrc.playwright.js`:

```javascript
module.exports = {
  extends: ["plugin:playwright/recommended"],
  files: ["playwright/**/*.ts"],
  rules: {
    "playwright/no-wait-for-timeout": "error",
    "playwright/prefer-web-first-assertions": "warn",
    "playwright/no-force-option": "warn",
    "playwright/no-page-pause": "error",
    "playwright/expect-expect": "warn",
    "playwright/no-conditional-expect": "warn",
  },
};
```

**Add npm script** to `frontend/package.json`:

```json
"pw:lint": "eslint --config .eslintrc.playwright.js playwright/"
```

### 1.6 Playwright MCP Server Setup

Add to project setup documentation:

```bash
claude mcp add playwright npx @playwright/mcp@latest
```

---

## Phase 2: Existing Test Fixes (Fix Current Anti-Patterns)

Priority ordering: highest flakiness risk first.

### P1 (Critical -- Causes CI Flakes)

#### 2.1 Fix `accept-results.ts` -- Remove `waitForResponse` as Assertion

**File:** `frontend/playwright/helpers/accept-results.ts` **Lines:** 68-92
**Risk:** HIGH -- Backend 500 causes generic error before UI notification
renders

**Changes needed:**

1. Keep `waitForResponse` for synchronization (line 68-73) but remove the
   `saveResponse.ok()` check and the programmatic throw (lines 86-92)
2. Instead, after `await saveResponsePromise`, assert on visible UI state:
   - Success:
     `await expect(page).toHaveURL(/AnalyzerResults/, { timeout: 45_000 })`
   - Error:
     `await expect(page.getByRole('alert').or(page.locator('.cds--inline-notification--error'))).not.toBeVisible()`
3. Keep the existing post-navigation UI assertions (lines 97-106) which are
   already correct

#### 2.2 Fix `ogc-284-barcode-workflow.spec.ts` -- Remove Autocomplete Polling

**File:** `frontend/playwright/tests/ogc-284-barcode-workflow.spec.ts`
**Function:** `pickFirstAutosuggestOptional` (lines 18-32) **Risk:** HIGH -- 12s
polling per call, 2 calls per order = up to 24s flake window

**Changes needed:**

1. Replace `pickFirstAutosuggestOptional` with a simple Tab press:
   ```typescript
   async function tabOutOfAutocomplete(page: Page) {
     await page.keyboard.press("Tab");
   }
   ```
2. In `fillOrderDetails` (lines 263-342), change the site/requester fill
   pattern:
   - For `siteInput`: fill full value ("CAMES MAN"), press Tab
   - For `requesterLookup`: fill full value ("Prime"), press Tab
3. Remove the `[data-cy="auto-suggestion"]` locator usage entirely (Cypress-era
   selector in Playwright tests)

#### 2.3 Fix `accept-results.ts` -- Replace `{ force: true }`

**File:** `frontend/playwright/helpers/accept-results.ts` **Line:** 53 **Risk:**
MEDIUM -- May mask actionability regression on Accept All checkbox

**Changes needed:**

1. Replace `await acceptAllCheckbox.check({ force: true })` with either:
   - `await page.getByLabel('Select all results').check()` (if label exists)
   - `await page.locator('label[for="saveallresults"]').click()` (click the
     label)

### P2 (Medium -- Masks Real Failures)

#### 2.4 Fix `ogc-284-barcode-workflow.spec.ts` -- Replace `{ force: true }`

**Lines:** 163, 199 **Changes needed:**

1. Line 163: Replace `await firstRadio.check({ force: true })` with clicking the
   Carbon radio label element
2. Line 199: Replace `await genderMale.check({ force: true })` with
   `await page.getByLabel('Male').check()` or clicking the label

#### 2.5 Fix `ogc-284-labels-ui.spec.ts` -- Replace `{ force: true }`

**Line:** 9 **Changes needed:** Replace `.click({ force: true })` with a proper
wait for the element to be actionable, then click without force.

#### 2.6 Fix `barcode-configuration.spec.ts` -- Replace `{ force: true }`

**Line:** 60 **Changes needed:** Replace
`await collectionDateCheckbox.setChecked(false, { force: true })` with
`await page.getByLabel('Collection Date and Time').uncheck()`.

#### 2.7 Fix `ogc-284-barcode-workflow.spec.ts` -- Remove Silent Error Swallowing

**Lines:** 84, 124, 188, 197-199, 205 **Changes needed:** Replace
`.catch(() => false)` patterns with explicit conditional checks:

```typescript
// Instead of: if (await element.isVisible().catch(() => false))
const isPresent = (await element.count()) > 0;
if (isPresent) {
  await expect(element).toBeVisible();
  // ... proceed with interaction
}
```

#### 2.8 Add Assertions to Demo Test Steps

**File:** `ogc-284-barcode-workflow.spec.ts` -- US3 test (lines 597-713)
**Changes needed:**

1. Add `await expect(successArea).toBeVisible()` after order submission
2. Add
   `await expect(page.getByRole('heading', { name: /print bar code labels/i })).toBeVisible()`
   in reprint section
3. Wrap logical sections in `test.step()` blocks

### P3 (Low -- Structural Improvements)

#### 2.9 Move `file-import.spec.ts` to API Test Suite

**File:** `frontend/playwright/tests/file-import.spec.ts` **Changes needed:**
This file contains zero UI interactions. Either:

- Move assertions into backend integration tests, or
- Convert to use `page.goto()` + UI assertions to verify the configurations are
  visible in the UI (making it a real E2E test)

#### 2.10 Add `test.step()` to Long Workflow Tests

**Files:**

- `ogc-284-barcode-workflow.spec.ts` (3 tests, each 100+ lines)
- `astm-genexpert-results.spec.ts` (1 test with multiple phases)
- `file-import-results.spec.ts` (1 test with multiple phases)

**Changes needed:** Wrap existing logical sections in `test.step()` blocks for
better trace readability and failure diagnostics.

---

## Phase 3: Architecture Improvements

### 3.1 Fixture Injection Migration

**Current state:** 10+ test files manually construct POMs with
`new AnalyzerListPage(page)`.

**Target state:** POMs provided via Playwright `test.extend()` fixtures.

**Migration plan:**

1. Create `frontend/playwright/fixtures/index.ts` with extended test:

   ```typescript
   import { test as base } from "@playwright/test";
   import { AnalyzerListPage } from "./analyzer-list";
   import { AnalyzerFormPage } from "./analyzer-form";
   import { ErrorDashboardPage } from "./error-dashboard";
   import { Sidenav } from "./sidenav";

   export const test = base.extend<{
     analyzerList: AnalyzerListPage;
     analyzerForm: AnalyzerFormPage;
     errorDashboard: ErrorDashboardPage;
     sidenav: Sidenav;
   }>({
     analyzerList: async ({ page }, use) => {
       await use(new AnalyzerListPage(page));
     },
     analyzerForm: async ({ page }, use) => {
       await use(new AnalyzerFormPage(page));
     },
     errorDashboard: async ({ page }, use) => {
       await use(new ErrorDashboardPage(page));
     },
     sidenav: async ({ page }, use) => {
       await use(new Sidenav(page));
     },
   });

   export { expect } from "@playwright/test";
   ```

2. Update test files to import from `../fixtures` instead of `@playwright/test`
3. Use destructured fixtures:
   `test('...', async ({ page, analyzerList }) => {})`

### 3.2 API-First Test Data Setup

**Current state:** Tests create data through UI navigation (slow, flaky) or rely
on SQL fixtures loaded externally.

**Target state:** Tests create required data via REST API in `beforeAll` hooks.

**Migration plan:**

1. Create `frontend/playwright/helpers/api-data.ts` with helper functions:
   - `createPatient(request, data)` - creates test patient via API
   - `createOrder(request, data)` - creates test order via API
   - `createAnalyzer(request, data)` - creates test analyzer via API (already
     exists in `ensure-analyzer.ts`)
2. Create shared fixtures that use API setup:
   ```typescript
   export const test = base.extend<{}, { testPatient: PatientData }>({
     testPatient: [async ({ request }, use) => {
       const patient = await createPatient(request, { ... });
       await use(patient);
       await deletePatient(request, patient.id);
     }, { scope: 'worker' }],
   });
   ```

### 3.3 ARIA Snapshot Testing Adoption

**Applicable to:** Carbon form components that have stable accessible structure.

**Migration plan:**

1. Start with `barcode-configuration.spec.ts` as a pilot -- the form has clear
   ARIA structure
2. Add ARIA snapshot tests alongside existing assertions (additive, not
   replacement)
3. Document the pattern in `.specify/guides/playwright-best-practices.md`

### 3.4 Playwright Test Agents Integration

**Timeline:** After Phase 1 and Phase 2 are complete.

**Steps:**

1. Install: `npx playwright init-agents --loop=claude`
2. Configure for OpenELIS-specific patterns (Carbon selectors, auth setup)
3. Document in CLAUDE.md as an optional development tool
4. Use for new test generation, not for fixing existing tests

---

## Appendix: Draft CLAUDE.md Addition

Insert after line 109 (after the "Playwright E2E -- RECOMMENDED" section):

```markdown
### Playwright Anti-Patterns (CRITICAL)

**Top 3 anti-patterns that cause flaky tests -- MUST AVOID:**

1. **`waitForResponse` as assertion** -- Use it for sync only, then assert on
   visible UI state (`toBeVisible`, `toBeEnabled`, `toHaveText`)
2. **Autocomplete polling** -- Type the full known value + Tab; never poll for
   dropdown suggestions with `expect.poll()`
3. **`{ force: true }`** -- Click the `<label>` instead of forcing hidden Carbon
   inputs; `force` masks real actionability regressions

**Full guide:** `.specify/guides/playwright-best-practices.md` **Quality
report:** `.specify/guides/playwright-e2e-quality-report.md`
```

## Appendix: Draft AGENTS.md Addition

Insert after line 1606 (after the "Key Patterns" subsection in the Playwright
section):

```markdown
#### Playwright Anti-Patterns (MUST AVOID)

These patterns cause flaky tests and invisible failures:

- **`waitForResponse` as primary assertion** -- Use for sync only. Always follow
  with a visible UI assertion. Never check `response.ok()` as the pass/fail
  signal.
- **Autocomplete polling** -- Type the full known value + Tab. Never use
  `expect.poll()` to wait for dropdown suggestions.
- **`{ force: true }`** -- Carbon overlays labels on hidden inputs. Click the
  `<label>` element instead. `force` bypasses actionability checks.
- **Silent `.catch(() => {})`** -- Never swallow errors. Use explicit
  conditional paths.
- **Tests without assertions** -- Every test must have `expect()`. Use
  `test.step()` for multi-step workflows.

**Full guide:** `.specify/guides/playwright-best-practices.md`
```
