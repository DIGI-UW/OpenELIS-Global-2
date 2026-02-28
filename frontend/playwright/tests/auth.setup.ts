import { test as setup, expect } from "@playwright/test";

const AUTH_FILE = "playwright/.auth/user.json";

setup("authenticate", async ({ page }, testInfo) => {
  testInfo.setTimeout(120_000);
  const username = process.env.TEST_USER;
  const password = process.env.TEST_PASS;

  if (!username || !password) {
    throw new Error(
      "TEST_USER and TEST_PASS environment variables must be set for Playwright authentication setup.",
    );
  }

  await page.goto("");
  await page
    .locator('input[name="loginName"], input[name="username"], input[type="text"]')
    .first()
    .fill(username);
  await page
    .locator(
      'input[name="password"], input[type="password"], input[id*="password" i]',
    )
    .first()
    .fill(password);
  const loginButton = page.getByRole("button", { name: /login/i });
  if ((await loginButton.count()) > 0) {
    await loginButton.first().click();
  } else {
    const submitButton = page.getByRole("button", { name: /submit/i });
    if ((await submitButton.count()) > 0) {
      await submitButton.first().click();
    } else {
      await page.locator('input[type="submit"]').first().click();
    }
  }

  // Verify authenticated state by reaching analyzer list route.
  await page.goto("analyzers");
  await expect(page).toHaveURL(/analyzers/);

  await page.context().storageState({ path: AUTH_FILE });
});
