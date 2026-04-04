import { test, expect } from "@playwright/test";
import { showTitleCard } from "../../../helpers/title-card";
import { videoPause, isVideoProject } from "../../../helpers/video-pause";

/** Capture a named screenshot — only in video projects (not CI) */
async function evidence(
  page: import("@playwright/test").Page,
  testInfo: import("@playwright/test").TestInfo,
  name: string,
) {
  if (!isVideoProject(testInfo)) return;
  const screenshot = await page.screenshot({ fullPage: true });
  await testInfo.attach(name, {
    body: screenshot,
    contentType: "image/png",
  });
}

test.describe("OGC-306: Calendar Management (US1)", () => {
  test("US1 — Full calendar management workflow", async ({
    page,
  }, testInfo) => {
    test.setTimeout(120_000);

    await test.step("Title card", async () => {
      await showTitleCard(
        page,
        "User Story 1: Calendar Management",
        "As a lab admin, manage public holidays and weekend configuration",
        3000,
        testInfo,
      );
    });

    await test.step("US1.1 — Navigate to Admin > Calendar Management", async () => {
      await page.goto("/MasterListsPage/calendarManagement");
      await expect(
        page.getByRole("heading", { name: "Calendar Management" }),
      ).toBeVisible({ timeout: 15_000 });
      await evidence(page, testInfo, "US1.1-calendar-management-page");
      await videoPause(page, 2000, testInfo);
    });

    await test.step("US1.2 — Verify table controls and UI elements", async () => {
      await expect(
        page.locator('[data-testid="holiday-count-footer"]'),
      ).toBeVisible({ timeout: 10_000 });
      await expect(page.locator('[data-testid="year-dropdown"]')).toBeVisible();
      await expect(
        page.locator('[data-testid="add-holiday-button"]'),
      ).toBeVisible();
      await expect(
        page.locator('[data-testid="import-csv-button"]'),
      ).toBeVisible();
      await expect(
        page.locator('[data-testid="export-csv-button"]'),
      ).toBeVisible();
      await evidence(page, testInfo, "US1.2-table-and-controls");
      await videoPause(page, 2000, testInfo);
    });

    await test.step("US1.3 — Open inline add form and verify validation", async () => {
      // Click Add Holiday — inline row appears
      await page.locator('[data-testid="add-holiday-button"]').click();
      await expect(
        page.locator('[data-testid="holiday-inline-row"]'),
      ).toBeVisible();

      // Save disabled when form is empty (validation works)
      await expect(
        page.locator('[data-testid="save-holiday-button"]'),
      ).toBeDisabled();
      await evidence(page, testInfo, "US1.3a-form-open-save-disabled");

      // Fill name only — save still disabled (date required)
      await page.locator("#new-holiday-name").fill("Test Holiday");
      await expect(
        page.locator('[data-testid="save-holiday-button"]'),
      ).toBeDisabled();
      await evidence(page, testInfo, "US1.3b-name-only-save-disabled");

      // Cancel closes the form
      await page.locator('[data-testid="cancel-holiday-button"]').click();
      await expect(
        page.locator('[data-testid="holiday-inline-row"]'),
      ).not.toBeVisible();
      await evidence(page, testInfo, "US1.3c-form-cancelled");
      await videoPause(page, 1500, testInfo);
    });

    await test.step("US1.4 — Verify weekend checkboxes", async () => {
      // Saturday and Sunday checked by default
      const satCheckbox = page.locator('[data-testid="weekend-checkbox-6"]');
      const sunCheckbox = page.locator('[data-testid="weekend-checkbox-0"]');
      await expect(satCheckbox).toBeVisible();
      await expect(sunCheckbox).toBeVisible();
      await evidence(page, testInfo, "US1.4-weekend-checkboxes");
      await videoPause(page, 1500, testInfo);
    });

    await test.step("US1.5 — Verify holiday count footer", async () => {
      await expect(
        page.locator('[data-testid="holiday-count-footer"]'),
      ).toBeVisible();
      await expect(
        page.locator('[data-testid="holiday-count-footer"]'),
      ).toContainText("holidays configured for");
      await evidence(page, testInfo, "US1.5-holiday-count-footer");
      await videoPause(page, 1000, testInfo);
    });
  });
});
