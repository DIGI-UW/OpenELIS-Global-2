import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1053 — a /Report link needs both `type` and `report`. One missing either
 * used to land on the Dashboard with nothing said, so a stale bookmark looked
 * like a session or permission problem. The page now stays, names what is
 * missing and offers the report lists; a complete link still opens the report.
 */

test.describe("OGC-1053 report link guard", () => {
  test("an incomplete report link explains itself instead of bouncing to the Dashboard", async ({
    page,
  }) => {
    await page.goto("/Report?type=patient", { waitUntil: "domcontentloaded" });
    const main = page.getByRole("main");

    await expect(main.getByText("This report link is incomplete")).toBeVisible({
      timeout: NAV_TIMEOUT,
    });
    await expect(main.getByText(/which report to open/)).toBeVisible();
    await expect(page).toHaveURL(/\/Report\?type=patient$/);

    await main.getByRole("link", { name: "Routine Reports" }).click();
    await expect(page).toHaveURL(/\/RoutineReports$/, { timeout: NAV_TIMEOUT });
  });

  test("a complete report link still opens the report", async ({ page }) => {
    await page.goto("/Report?type=patient&report=TBPatientReport", {
      waitUntil: "domcontentloaded",
    });
    const main = page.getByRole("main");

    await expect(
      main.getByRole("navigation", { name: /breadcrumb/i }),
    ).toContainText("Routine Reports", { timeout: NAV_TIMEOUT });
    await page.waitForLoadState("networkidle");
    await expect(page).toHaveURL(
      /\/Report\?type=patient&report=TBPatientReport$/,
    );
    await expect(main.getByText("This report link is incomplete")).toHaveCount(
      0,
      { timeout: UI_TIMEOUT },
    );
  });
});
