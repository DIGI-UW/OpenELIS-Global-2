import { test as setup, expect } from "@playwright/test";

const AUTH_FILE = "playwright/.auth/user.json";

setup("authenticate", async ({ page }) => {
  const username = process.env.TEST_USER;
  const password = process.env.TEST_PASS;

  if (!username || !password) {
    throw new Error(
      "TEST_USER and TEST_PASS environment variables must be set for Playwright authentication setup.",
    );
  }

  await page.goto("/");
  await page.getByLabel("Username").fill(username);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Login" }).click();

  // Wait for authenticated state (sidenav menu button visible)
  await expect(page.locator("#sidenav-menu-button")).toBeVisible();

  await page.context().storageState({ path: AUTH_FILE });
});
