# Playwright E2E Testing Best Practices

> **Purpose:** Guide for writing efficient, maintainable Playwright E2E tests
> for OpenELIS Global 2's React + Carbon Design System frontend.

## Quick Reference

```bash
# Install browsers (first time only)
npm run pw:install

# Run all tests
npm run pw:test

# Run with UI (debugging)
npm run pw:test:ui

# Run headed (see browser)
npm run pw:test:headed

# Run specific file
npx playwright test sidenav.spec.ts

# Run specific test
npx playwright test -g "home page has collapsed nav"
```

---

## Core Principles

### 1. Test User-Visible Behavior

Test what users see and do, not implementation details.

```typescript
// ❌ BAD: Testing CSS class (implementation detail)
await expect(page.locator(".btn-primary")).toBeVisible();

// ✅ GOOD: Testing user-visible behavior
await expect(page.getByRole("button", { name: "Submit" })).toBeVisible();
```

### 2. Keep Tests Isolated

Each test runs in a fresh browser context. Don't rely on state from other tests.

```typescript
// ❌ BAD: Test depends on previous test's state
test("step 2", async ({ page }) => {
  // Assumes 'step 1' already ran
});

// ✅ GOOD: Test is self-contained
test("complete workflow", async ({ page }) => {
  await page.goto("/storage/samples");
  // All setup within this test
});
```

### 3. Use Auto-Retrying Assertions

Playwright assertions automatically retry. Never use `waitForTimeout`.

```typescript
// ❌ BAD: Arbitrary wait
await page.waitForTimeout(2000);
await expect(element).toBeVisible();

// ✅ GOOD: Auto-retrying assertion
await expect(element).toBeVisible(); // Retries until visible or timeout
```

---

## Project Structure

```
frontend/
├── playwright.config.ts          # Configuration
├── playwright/
│   ├── .auth/
│   │   └── user.json             # Cached auth state (gitignored)
│   ├── fixtures/
│   │   └── sidenav.ts            # Page Objects
│   └── tests/
│       ├── auth.setup.ts         # Auth setup project
│       └── sidenav.spec.ts       # Test specs
```

---

## Authentication Strategy

We use Playwright's **setup project** pattern: authenticate once, reuse session
for all tests.

### How It Works

1. `auth.setup.ts` runs first, logs in, saves cookies/localStorage to
   `.auth/user.json`
2. All other tests load this state automatically
3. No repeated login UI interactions = fast tests

### Setup Project (`auth.setup.ts`)

```typescript
import { test as setup, expect } from "@playwright/test";

const AUTH_FILE = "playwright/.auth/user.json";

setup("authenticate", async ({ page }) => {
  const username = process.env.TEST_USER || "admin";
  const password = process.env.TEST_PASS || "adminADMIN!";

  await page.goto("/");
  await page.getByLabel("Username").fill(username);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Login" }).click();

  // Wait for authenticated state
  await expect(page.getByRole("button", { name: /menu/i })).toBeVisible();

  await page.context().storageState({ path: AUTH_FILE });
});
```

### Config (`playwright.config.ts`)

```typescript
projects: [
  { name: 'setup', testMatch: /.*\.setup\.ts/ },
  {
    name: 'chromium',
    use: { storageState: 'playwright/.auth/user.json' },
    dependencies: ['setup'],
  },
],
```

**Reference:** [Playwright Auth Docs](https://playwright.dev/docs/auth)

---

## Page Object Model

Encapsulate page interactions in reusable classes. This is the recommended
pattern for maintainability.

### Pattern

```typescript
// fixtures/sidenav.ts
import { Page, expect, Locator } from "@playwright/test";

export class Sidenav {
  readonly page: Page;
  readonly nav: Locator;
  readonly menuButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.nav = page.locator(".cds--side-nav");
    this.menuButton = page.locator('[data-cy="menuButton"]');
  }

  async expectExpanded() {
    await expect(this.nav).toHaveClass(/cds--side-nav--expanded/);
  }

  async expectCollapsed() {
    await expect(this.nav).not.toHaveClass(/cds--side-nav--expanded/);
  }

  async toggle() {
    await this.menuButton.click();
  }

  async clickMenu(text: string) {
    await this.nav.getByRole("link", { name: text }).click();
  }

  async expandMenu(text: string) {
    const button = this.nav.getByRole("button", { name: text, exact: true });
    const expanded = await button.getAttribute("aria-expanded");
    if (expanded !== "true") {
      await button.click();
    }
  }
}
```

### Usage in Tests

```typescript
// tests/sidenav.spec.ts
import { test, expect } from "@playwright/test";
import { Sidenav } from "../fixtures/sidenav";

test("storage page has expanded nav", async ({ page }) => {
  const sidenav = new Sidenav(page);
  await page.goto("/Storage/samples");
  await sidenav.expectExpanded();
});
```

**Reference:** [Playwright POM Docs](https://playwright.dev/docs/pom)

---

## Selector Strategy for Carbon Design System

### Priority Order (Most to Least Preferred)

| Priority | Selector Type | Example                                   | When to Use                            |
| -------- | ------------- | ----------------------------------------- | -------------------------------------- |
| 1        | Role + Name   | `getByRole('button', { name: 'Submit' })` | Always prefer for interactive elements |
| 2        | Label         | `getByLabel('Username')`                  | Form inputs                            |
| 3        | Test ID       | `locator('[data-cy="menuButton"]')`       | When semantic selectors don't work     |
| 4        | Text          | `getByText('Dashboard')`                  | Static text content                    |
| 5        | CSS Class     | `locator('.cds--side-nav')`               | Carbon structural elements only        |

### Carbon-Specific Patterns

```typescript
// Carbon SideNav
const sidenav = page.locator(".cds--side-nav");
const menuItem = sidenav.getByRole("link", { name: "Storage" });
const submenu = sidenav.getByRole("button", { name: "Storage", exact: true });

// Carbon Buttons
page.getByRole("button", { name: "Save" });

// Carbon Form Inputs
page.getByLabel("Patient Name");

// Carbon Dropdowns
page.getByRole("combobox", { name: "Select Status" });

// Carbon Tabs
page.getByRole("tab", { name: "Details" });

// Carbon Modal
page.getByRole("dialog");
```

### Use `exact: true` for Substring Conflicts

```typescript
// ❌ BAD: Matches "Storage", "Storage Management", "Cold Storage Monitoring"
page.getByRole("button", { name: "Storage" });

// ✅ GOOD: Matches only "Storage"
page.getByRole("button", { name: "Storage", exact: true });
```

**Reference:** [Playwright Locators](https://playwright.dev/docs/locators)

---

## Writing Tests

### Test Structure

```typescript
import { test, expect } from "@playwright/test";
import { Sidenav } from "../fixtures/sidenav";

test.describe("Feature Name", () => {
  test("specific behavior being tested", async ({ page }) => {
    // Arrange
    const sidenav = new Sidenav(page);
    await page.goto("/Storage/samples");

    // Act
    await sidenav.toggle();

    // Assert
    await sidenav.expectCollapsed();
  });
});
```

### Best Practices

| Do                                   | Don't                             |
| ------------------------------------ | --------------------------------- |
| One assertion focus per test         | Multiple unrelated assertions     |
| Use Page Objects for reuse           | Duplicate selectors across tests  |
| `await expect(x).toBeVisible()`      | `await page.waitForTimeout(1000)` |
| `{ exact: true }` for ambiguous text | Rely on `.first()`                |
| Test user workflows                  | Test implementation details       |
| Use semantic selectors               | Use fragile CSS selectors         |

### Waiting for Navigation

```typescript
// ❌ BAD: Race condition
await page.click("a");
await expect(page.locator(".content")).toBeVisible();

// ✅ GOOD: Wait for URL change
await page.click("a");
await expect(page).toHaveURL(/\/dashboard/);
```

---

## Configuration Reference

```typescript
// playwright.config.ts
import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./playwright/tests",

  // Parallelization
  fullyParallel: true,
  workers: process.env.CI ? 1 : undefined,

  // CI safeguards
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,

  // Timeouts
  timeout: 30_000, // Per-test timeout
  expect: { timeout: 5_000 }, // Assertion timeout

  // Reporting
  reporter: process.env.CI ? "github" : "html",

  use: {
    baseURL: process.env.BASE_URL || "https://localhost",
    ignoreHTTPSErrors: true,

    // Evidence on failure
    trace: "on-first-retry",
    screenshot: "only-on-failure",
    video: "off",
  },

  projects: [
    { name: "setup", testMatch: /.*\.setup\.ts/ },
    {
      name: "chromium",
      use: {
        ...devices["Desktop Chrome"],
        storageState: "playwright/.auth/user.json",
      },
      dependencies: ["setup"],
    },
  ],
});
```

---

## Environment Variables

| Variable    | Default             | Description                         |
| ----------- | ------------------- | ----------------------------------- |
| `BASE_URL`  | `https://localhost` | Target server URL                   |
| `TEST_USER` | `admin`             | Login username                      |
| `TEST_PASS` | `adminADMIN!`       | Login password                      |
| `CI`        | -                   | Enables CI mode (stricter settings) |

```bash
# Example: Run against staging
BASE_URL=https://staging.example.com npm run pw:test
```

---

## Debugging

### Interactive Mode

```bash
npm run pw:test:ui    # Full UI with time-travel debugging
npm run pw:test:headed  # See browser window
```

### Trace Viewer

On failure, traces are captured. Open with:

```bash
npx playwright show-trace test-results/*/trace.zip
```

### Screenshots

Auto-captured on failure in `test-results/`. Review to understand failure state.

### Console Output

```bash
npm run pw:test 2>&1 | tee /tmp/playwright.log
```

---

## Adding New Tests

### 1. Create Page Object (if needed)

```typescript
// fixtures/storage.ts
export class StoragePage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto("/Storage/samples");
  }

  async selectSample(id: string) {
    await this.page.getByRole("row", { name: id }).click();
  }
}
```

### 2. Write Test

```typescript
// tests/storage.spec.ts
import { test, expect } from "@playwright/test";
import { StoragePage } from "../fixtures/storage";

test("can select sample", async ({ page }) => {
  const storage = new StoragePage(page);
  await storage.goto();
  await storage.selectSample("SAMPLE-001");
  await expect(page.getByText("Sample Details")).toBeVisible();
});
```

### 3. Run

```bash
npx playwright test storage.spec.ts
```

---

## Anti-Patterns

| ❌ Avoid                        | ✅ Instead                          |
| ------------------------------- | ----------------------------------- |
| `page.waitForTimeout(ms)`       | Auto-retrying `expect()` assertions |
| `.first()` / `.nth(0)`          | More specific selectors             |
| Hardcoded credentials in tests  | Environment variables               |
| Testing CSS classes for state   | Role/ARIA attributes                |
| Long tests with many assertions | Focused single-concern tests        |
| Repeated login in each test     | Setup project with storageState     |
| Raw CSS selectors               | Semantic `getByRole`, `getByLabel`  |

---

## References

- [Playwright Best Practices](https://playwright.dev/docs/best-practices)
- [Page Object Model](https://playwright.dev/docs/pom)
- [Authentication](https://playwright.dev/docs/auth)
- [Locators](https://playwright.dev/docs/locators)
- [Assertions](https://playwright.dev/docs/test-assertions)
- [Carbon Design System](https://carbondesignsystem.com/)

---

**Last Updated:** 2025-12-22  
**Applies To:** `frontend/playwright/`
