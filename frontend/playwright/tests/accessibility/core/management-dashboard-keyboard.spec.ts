import { test, expect } from "../../../helpers/test-base";
import type { Locator, Page } from "@playwright/test";
import { LONG_TIMEOUT, NAV_TIMEOUT } from "../../../helpers/timeouts";

async function tabTo(page: Page, target: Locator, maxTabs = 150) {
  await expect(target).toBeVisible({ timeout: LONG_TIMEOUT });
  for (let index = 0; index < maxTabs; index += 1) {
    await page.keyboard.press("Tab");
    const reached = await target.evaluate(
      (element) =>
        element === document.activeElement ||
        element.contains(document.activeElement),
    );
    if (reached) {
      await expect(target).toBeFocused();
      return;
    }
  }
  throw new Error(
    `Keyboard focus did not reach ${await target.evaluate((element) => element.outerHTML.slice(0, 300))}`,
  );
}

test.describe("Management Overview keyboard-only workflow", () => {
  test("changes scope, refreshes and opens the CSV picker without a mouse", async ({
    page,
  }, testInfo) => {
    test.skip(
      testInfo.project.name.endsWith("-mobile"),
      "The phone layout swaps the scope tabs for a dropdown",
    );
    test.setTimeout(120_000);
    await page.addInitScript(() => {
      Object.keys(localStorage)
        .filter((key) => key.startsWith("mgmtDashboard.scope."))
        .forEach((key) => localStorage.removeItem(key));
    });
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
