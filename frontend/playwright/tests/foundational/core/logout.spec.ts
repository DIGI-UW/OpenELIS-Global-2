import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT } from "../../../helpers/timeouts";

// Logging out ends the server session, so this spec logs in on its own
// instead of ending the shared storageState session other specs reuse.
test.use({ storageState: { cookies: [], origins: [] } });

test.describe("Session logout", () => {
  test("Logout in the user menu ends the session", async ({ page }) => {
    test.setTimeout(NAV_TIMEOUT * 2);
    const username = process.env.TEST_USER || "admin";
    const password = process.env.TEST_PASS || "adminADMIN!";
    const usernameInput = page.locator("#loginName");
    const sideNav = page.getByRole("navigation", { name: "Side navigation" });

    await page.goto("/login", { waitUntil: "domcontentloaded" });
    await expect(usernameInput).toBeVisible({ timeout: NAV_TIMEOUT });
    await usernameInput.fill(username);
    await page.locator("#password").fill(password);
    await page.locator('[data-cy="loginButton"]').click();
    await expect(sideNav).toBeVisible({ timeout: NAV_TIMEOUT });

    const signedIn = await page.request.get("/api/OpenELIS-Global/session");
    expect((await signedIn.json()).authenticated).toBe(true);

    await page.getByRole("button", { name: "User", exact: true }).click();
    await page
      .locator(".headerPanel")
      .getByText("Logout", { exact: true })
      .click();

    await expect(usernameInput).toBeVisible({ timeout: NAV_TIMEOUT });
    await expect(sideNav).toBeHidden();
    const signedOut = await page.request.get("/api/OpenELIS-Global/session");
    expect((await signedOut.json()).authenticated).toBe(false);
  });
});
