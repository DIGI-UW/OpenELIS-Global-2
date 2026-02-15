import { addParityAnnotations } from "../fixtures/parity-metadata";
import { test, expect } from "../fixtures/e2e-base";

test.describe("Playwright harness smoke", () => {
  test("authenticated shell is available on dashboard", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-024",
      riskTier: "P0",
      domain: "core",
      notes: "M3 harness baseline for login/session shell availability",
    });

    await gotoAndWait("/Dashboard");
    await ensureAuthenticatedShell();
    await expect(page).toHaveURL(/\/Dashboard/i);
  });

  test("storage samples route is reachable with authenticated session", async ({
    page,
    gotoAndWait,
    ensureAuthenticatedShell,
  }, testInfo) => {
    addParityAnnotations(testInfo, {
      legacyScenarioId: "LEG-CYP-033",
      riskTier: "P0",
      domain: "storage",
      notes: "M3 harness baseline for shared route navigation",
    });

    await gotoAndWait("/Storage/samples");
    await ensureAuthenticatedShell();
    await expect(page).toHaveURL(/\/Storage\/samples/);
  });
});
