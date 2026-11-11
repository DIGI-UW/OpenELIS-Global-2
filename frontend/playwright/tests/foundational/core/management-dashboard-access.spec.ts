import { test, expect } from "../../../helpers/test-base";
import type { Page } from "@playwright/test";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";

const PAGE = "/dashboards/management";
const API = "/api/OpenELIS-Global/rest/dashboards/management";
const MENU_LINK = 'a[id="menu_reports_management_overview_nav"]';

const openReports = async (page: Page) => {
  await page.goto("/", { timeout: NAV_TIMEOUT });
  const nav = page.getByRole("navigation", { name: "Side navigation" });
  const reports = nav.locator('button[id="menu_reports"]');
  await expect(reports).toBeVisible({ timeout: NAV_TIMEOUT });
  if ((await reports.getAttribute("aria-expanded")) !== "true") {
    await reports.click();
  }
  return {
    nav,
    firstReport: nav
      .locator('li:has(> button[id="menu_reports"]) > ul > li')
      .first(),
  };
};

test.describe("Management Overview access", () => {
  test("an administrator finds it first under Reports and sees every section with data", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    await page.clock.install();
    const firstReport = (await openReports(page)).firstReport.locator("a");
    await expect(firstReport).toHaveAttribute(
      "id",
      "menu_reports_management_overview_nav",
    );
    await expect(firstReport).toHaveText("Management Overview");
    await firstReport.click();
    await expect(page).toHaveURL(PAGE);

    const dashboard = page.getByTestId("mgmt-dashboard");
    await expect(
      dashboard.getByRole("heading", { name: "Management Overview", level: 2 }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(page.getByTestId("mgmt-subtitle")).toContainText(
      "Last 30 days",
    );

    const { features } = await (
      await page.request.get(`${API}/summary?scope=30d`)
    ).json();
    const { programs } = await (
      await page.request.get(`${API}/surveillance?scope=30d`)
    ).json();
    const sections: [string, boolean][] = [
      ["mgmt-kpis", true],
      ["mgmt-workload", true],
      ["mgmt-equipment", features.analyzers],
      ["mgmt-antibiogram", features.microbiology],
      ["mgmt-stock", features.inventory],
      ["mgmt-quality", true],
      ["mgmt-surveillance", programs.length > 0],
      ["mgmt-export", true],
    ];
    for (const [testId, shown] of sections) {
      await expect(page.getByTestId(testId)).toHaveCount(shown ? 1 : 0, {
        timeout: LONG_TIMEOUT,
      });
    }
    await expect(
      dashboard.locator(
        ".cds--actionable-notification, .cds--skeleton__text, .cds--skeleton, .cds--loading",
      ),
    ).toHaveCount(0, { timeout: LONG_TIMEOUT });

    await test.step("Changing the scope refreshes the Updated stamp", async () => {
      const updated = page.getByTestId("mgmt-updated");
      await page.clock.fastForward("02:05");
      await expect(updated).toHaveText("Updated 2m ago", {
        timeout: UI_TIMEOUT,
      });
      await page.getByRole("tab", { name: "Last 7 days" }).click();
      await expect(page.getByTestId("mgmt-subtitle")).toContainText(
        "Last 7 days",
        { timeout: LONG_TIMEOUT },
      );
      await expect(updated).toHaveText("Updated 0m ago");
    });

    await test.step("Export PDF downloads the snapshot", async () => {
      const download = page.waitForEvent("download", {
        timeout: LONG_TIMEOUT,
      });
      await page.getByTestId("mgmt-export-pdf").click();
      const file = await download;
      expect(file.suggestedFilename()).toMatch(
        /^management-overview-7d-\d{4}-\d{2}-\d{2}\.pdf$/,
      );
      expect(await file.failure()).toBeNull();
    });
  });

  test.describe("without the grant", () => {
    test.use({ storageState: "playwright/.auth/participant.json" });

    test("the menu item is absent, the page is denied and the API refuses", async ({
      page,
    }) => {
      const { nav, firstReport } = await openReports(page);
      await expect(firstReport).toBeVisible();
      await expect(nav.locator(MENU_LINK)).toHaveCount(0);

      await page.goto(PAGE, { timeout: NAV_TIMEOUT });
      await expect(page.getByText("Access Denied")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await expect(page.getByTestId("mgmt-dashboard")).toHaveCount(0);

      const summary = await page.request.get(`${API}/summary?scope=30d`);
      expect(summary.status()).toBe(403);
    });
  });
});
