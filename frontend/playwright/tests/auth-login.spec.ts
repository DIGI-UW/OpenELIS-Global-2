import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.use({ storageState: { cookies: [], origins: [] } });

test.describe("Auth login parity migration", () => {
  test("rejects invalid credentials", async ({ page }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-024",
      riskTier: "P0",
      domain: "core",
      notes: "M4a invalid-login parity check",
    });

    await page.goto("/");
    await page.getByLabel("Username").fill("invalid-user");
    await page.getByLabel("Password").fill("invalid-password");
    await page.getByRole("button", { name: "Login" }).click();

    await expect(
      page.getByText("Username or Password are incorrect"),
    ).toBeVisible();
  });

  test("logs in with configured credentials", async ({ page }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-024",
      riskTier: "P0",
      domain: "core",
      notes: "M4a successful-login parity check",
    });

    const username = process.env.TEST_USER || "admin";
    const password = process.env.TEST_PASS || "adminADMIN!";

    await page.goto("/");
    await page.getByLabel("Username").fill(username);
    await page.getByLabel("Password").fill(password);
    await page.getByRole("button", { name: "Login" }).click();

    await expect(page.locator("#sidenav-menu-button")).toBeVisible();
    await expect(page.locator("#mainHeader")).toBeVisible();
  });

  test("retains authenticated shell after refresh", async ({
    page,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-024",
      riskTier: "P0",
      domain: "core",
      notes: "M4a session persistence parity check",
    });

    const username = process.env.TEST_USER || "admin";
    const password = process.env.TEST_PASS || "adminADMIN!";

    await page.goto("/");
    await page.getByLabel("Username").fill(username);
    await page.getByLabel("Password").fill(password);
    await page.getByRole("button", { name: "Login" }).click();
    await expect(page.locator("#sidenav-menu-button")).toBeVisible();

    await page.reload();
    await expect(page.locator("#sidenav-menu-button")).toBeVisible();
    await expect(page.locator("#mainHeader")).toBeVisible();
  });
});
