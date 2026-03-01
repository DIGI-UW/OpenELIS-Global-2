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

  await page.goto("login", { waitUntil: "domcontentloaded" });
  const usernameInput = page
    .locator(
      'input[name="loginName"], input[name="username"], input[aria-label="Username"], input[id*="user" i], input[type="text"]',
    )
    .first();
  const passwordInput = page
    .locator(
      'input[name="password"], input[aria-label="Password"], input[id*="password" i], input[type="password"]',
    )
    .first();
  // OE can render the login shell before form inputs are hydrated right after restarts.
  // Retry a few times to avoid flaky setup failures during harness warm-up.
  let formReady = false;
  for (let attempt = 1; attempt <= 6; attempt++) {
    if (
      (await usernameInput.count()) > 0 &&
      (await passwordInput.count()) > 0 &&
      (await usernameInput.first().isVisible()) &&
      (await passwordInput.first().isVisible())
    ) {
      formReady = true;
      break;
    }
    await page.waitForTimeout(5_000);
    await page.goto("login", { waitUntil: "domcontentloaded" });
  }
  expect(formReady, "Login form inputs should be visible before auth setup").toBeTruthy();
  await usernameInput.fill(username);
  await passwordInput.fill(password);

  await Promise.all([
    page.waitForURL((url) => !url.pathname.endsWith("/login"), {
      timeout: 15_000,
    }),
    page.getByRole("button", { name: /login/i }).first().click(),
  ]);

  // Verify authenticated state by reaching analyzer list route and ensuring
  // we're not bounced back to /login after async auth checks complete.
  await page.goto("analyzers", { waitUntil: "networkidle" });
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/, { timeout: 15_000 });
  await expect(page.locator('[data-testid="analyzers-list"]')).toBeVisible({
    timeout: 45_000,
  });

  await page.context().storageState({ path: AUTH_FILE });
});
