import { test as setup, expect } from "@playwright/test";

const AUTH_FILE = "playwright/.auth/user.json";

setup("authenticate", async ({ page }, testInfo) => {
  testInfo.setTimeout(120_000);
  const username = process.env.TEST_USER;
  const password = process.env.TEST_PASS;

  if (!username || !password) {
    throw new Error(
      "Playwright auth setup requires TEST_USER and TEST_PASS environment variables to be set.",
    );
  }

  await page.goto("login", { waitUntil: "domcontentloaded" });

  const usernameInput = page.getByLabel("Username");
  const passwordInput = page.getByLabel("Password");
  await expect(usernameInput).toBeVisible({ timeout: 45_000 });
  await expect(passwordInput).toBeVisible({ timeout: 5_000 });

  if (page.url().includes("/login")) {
    await usernameInput.fill(username);
    await passwordInput.fill(password);
    await page.getByRole("button", { name: "Login" }).click();
    await expect(page).not.toHaveURL(/\/login(?:\?|$)/, { timeout: 15_000 });
  }

  await page.goto("analyzers", { waitUntil: "domcontentloaded" });
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/, { timeout: 15_000 });
  await expect(page.locator('[data-testid="analyzers-list"]')).toBeVisible({
    timeout: 45_000,
  });

  await page.context().storageState({ path: AUTH_FILE });
});
