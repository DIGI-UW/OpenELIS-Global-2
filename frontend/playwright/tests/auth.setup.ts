import { test as setup, expect } from "../helpers/test-base";
import { NAV_TIMEOUT } from "../helpers/timeouts";

const AUTH_FILE = "playwright/.auth/user.json";

/**
 * Shared authentication setup for all Playwright projects.
 *
 * Flow:
 *   1. Open the application and wait for the visible login form
 *   2. Enter credentials and submit through the same form a user sees
 *   3. Verify authenticated application chrome
 *   4. Save browser storage state for downstream tests
 */
setup("authenticate", async ({ page }, testInfo) => {
  testInfo.setTimeout(NAV_TIMEOUT);

  // Defaults match .env.example, frontend/playwright/helpers/verify-login.sh,
  // and projects/analyzer-harness/seed-analyzers.sh: admin / adminADMIN!.
  // A .env file or explicit exports still take precedence.
  const username = process.env.TEST_USER || "admin";
  const password = process.env.TEST_PASS || "adminADMIN!";

  const usernameInput = page.locator("#loginName");
  const passwordInput = page.locator("#password");
  const loginButton = page.locator('[data-cy="loginButton"]');

  await page.goto("/login", { waitUntil: "domcontentloaded" });
  await expect(usernameInput).toBeVisible({ timeout: NAV_TIMEOUT });
  await expect(passwordInput).toBeVisible();

  await usernameInput.fill(username);
  await passwordInput.fill(password);
  await loginButton.click();

  await expect(page).toHaveURL(/\/$/, { timeout: NAV_TIMEOUT });
  await expect(
    page.getByRole("navigation", { name: "Side navigation" }),
  ).toBeVisible({ timeout: NAV_TIMEOUT });
  await expect(usernameInput).toBeHidden();

  await page.context().storageState({ path: AUTH_FILE });
});
