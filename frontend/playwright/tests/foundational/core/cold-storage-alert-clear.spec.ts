import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT } from "../../../helpers/timeouts";
import {
  seedFreezerWithOpenAlert,
  cleanupFreezerAlert,
} from "../../../helpers/seed-freezer-alert";

const FREEZER_NAME = "E2E-CLR-FRZ";

test.describe("Cold Storage — clear an active alert", () => {
  test.afterEach(() => {
    cleanupFreezerAlert(FREEZER_NAME);
  });

  test("an active alert exposes a Clear action that removes it from the list", async ({
    page,
  }) => {
    seedFreezerWithOpenAlert(FREEZER_NAME);

    await page.goto("/FreezerMonitoring", { waitUntil: "domcontentloaded" });

    const alertRow = page
      .getByRole("row")
      .filter({ hasText: FREEZER_NAME })
      .filter({ has: page.getByRole("button", { name: "Clear" }) });
    await expect(alertRow).toBeVisible({ timeout: NAV_TIMEOUT });

    await alertRow.getByRole("button", { name: "Clear" }).click();

    await expect(alertRow).toHaveCount(0, { timeout: NAV_TIMEOUT });

    await page.reload({ waitUntil: "domcontentloaded" });
    await expect(alertRow).toHaveCount(0, { timeout: NAV_TIMEOUT });
  });
});
