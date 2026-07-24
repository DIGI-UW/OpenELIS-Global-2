import { expect } from "@playwright/test";
import { test as setup } from "../helpers/test-base";
import { NAV_TIMEOUT } from "../helpers/timeouts";

const AUTH_FILE = "playwright/.auth/harness-ui-user.json";

setup(
  "authenticate through the visible login form",
  async ({ page }, testInfo) => {
    testInfo.setTimeout(NAV_TIMEOUT);

    const username = process.env.TEST_USER || "admin";
    const password = process.env.TEST_PASS || "adminADMIN!";

    await page.context().clearCookies();
    await page.goto("/login", { waitUntil: "domcontentloaded" });

    const usernameInput = page.locator("#loginName");
    const passwordInput = page.locator("#password");
    const loginButton = page.locator("[data-cy='loginButton']");

    await expect(usernameInput).toBeVisible({ timeout: NAV_TIMEOUT });
    await expect(passwordInput).toBeVisible();
    await usernameInput.fill(username);
    await passwordInput.fill(password);
    await expect(loginButton).toBeEnabled();
    await loginButton.click();

    await expect(page).not.toHaveURL(/\/login(?:\?|$)/, {
      timeout: NAV_TIMEOUT,
    });
    await page.context().storageState({ path: AUTH_FILE });
  },
);
