import { test as setup, expect } from "@playwright/test";

const AUTH_FILE = "playwright/.auth/user.json";

setup("authenticate", async ({ page }) => {
  const username = process.env.TEST_USER || "admin";
  const password = process.env.TEST_PASS || "adminADMIN!";

  await page.goto("/");
  await page.getByLabel("Username").fill(username);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Login" }).click();

  // Wait for authenticated state (sidenav menu button visible)
  await expect(page.locator("#sidenav-menu-button")).toBeVisible();

  await page.context().storageState({ path: AUTH_FILE });
});
