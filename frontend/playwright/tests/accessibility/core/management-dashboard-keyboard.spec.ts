import { test, expect } from "../../../helpers/test-base";
import { tabTo } from "../../../helpers/accessibility";
import { LONG_TIMEOUT, NAV_TIMEOUT } from "../../../helpers/timeouts";

test.describe("Management Overview keyboard-only workflow", () => {
  test("changes scope, refreshes and opens the CSV picker without a mouse", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    await page.setViewportSize({ width: 1440, height: 1000 });
    await page.goto("/dashboards/management", { timeout: NAV_TIMEOUT });
    const subtitle = page.getByTestId("mgmt-subtitle");
    await expect(subtitle).toContainText("Last 30 days", {
      timeout: LONG_TIMEOUT,
    });

    const selectedScope = page.getByRole("tab", { name: "Last 30 days" });
    await tabTo(page, selectedScope);
    await page.keyboard.press("ArrowRight");
    await page.keyboard.press("Enter");
    await expect(subtitle).toContainText("Last 90 days", {
      timeout: LONG_TIMEOUT,
    });

    const refresh = page.getByTestId("mgmt-refresh");
    await tabTo(page, refresh);
    await page.keyboard.press("Enter");
    await expect(refresh).toBeDisabled();

    const csv = page.getByTestId("mgmt-export-csv");
    await tabTo(page, csv);
    await page.keyboard.press("Enter");
    const picker = page.getByRole("dialog", { name: "Download CSV" });
    await expect(picker).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect
      .poll(() =>
        picker.evaluate((dialog) => dialog.contains(document.activeElement)),
      )
      .toBe(true);
    await page.keyboard.press("Escape");
    await expect(picker).toBeHidden();
  });
});
