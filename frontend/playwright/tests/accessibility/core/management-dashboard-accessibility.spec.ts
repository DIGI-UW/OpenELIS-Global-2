import { test, expect } from "../../../helpers/test-base";
import { expectNoWcag21AaViolations } from "../../../helpers/accessibility";
import { LONG_TIMEOUT, NAV_TIMEOUT } from "../../../helpers/timeouts";

test.describe("Management Overview WCAG 2.1 AA qualification", () => {
  test("dashboard, CSV section picker and composite weights", async ({
    page,
  }, testInfo) => {
    test.setTimeout(120_000);
    if (!testInfo.project.name.endsWith("-mobile")) {
      await page.setViewportSize({ width: 1440, height: 1000 });
    }

    await page.goto("/dashboards/management", { timeout: NAV_TIMEOUT });
    await expect(page.getByTestId("mgmt-subtitle")).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
    await expect(page.getByTestId("mgmt-kpis")).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
    await expect(page.getByTestId("mgmt-export")).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
    await expect(
      page
        .getByTestId("mgmt-dashboard")
        .locator(".cds--skeleton__text, .cds--skeleton, .cds--loading"),
    ).toHaveCount(0, { timeout: LONG_TIMEOUT });
    await expectNoWcag21AaViolations(page, testInfo, "management-overview");

    await page.getByTestId("mgmt-export-csv").click();
    const picker = page.getByRole("dialog", { name: "Download CSV" });
    await expect(picker).toBeVisible({ timeout: LONG_TIMEOUT });
    await expectNoWcag21AaViolations(
      page,
      testInfo,
      "management-overview-csv-picker",
    );
    await picker.getByRole("button", { name: "Cancel" }).click();
    await expect(picker).toBeHidden();

    await page.goto("/qa/qi/config", { timeout: NAV_TIMEOUT });
    await expect(page.getByTestId("composite-weights")).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
    await expectNoWcag21AaViolations(
      page,
      testInfo,
      "management-overview-composite-weights",
    );
  });
});
