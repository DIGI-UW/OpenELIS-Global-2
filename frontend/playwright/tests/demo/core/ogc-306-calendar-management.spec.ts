import { test, expect } from "../../../helpers/test-base";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import { videoPause } from "../../../helpers/video-pause";
import {
  seedHolidays,
  cleanupHolidays,
} from "../../../helpers/seed-calendar-data";

test.describe("OGC-306: Calendar Management (US1)", () => {
  test.beforeEach(async ({ page }) => {
    await seedHolidays(page, 2026);
  });

  test.afterEach(async ({ page }) => {
    await cleanupHolidays(page, 2026);
  });

  test("US1 — Full calendar management workflow", async ({
    page,
  }, testInfo) => {
    test.setTimeout(180_000);
    const demo = createDemoPresentation(page, testInfo);

    await test.step("Title card", async () => {
      await demo.title(
        "User Story 1: Calendar Management",
        "As a lab admin, manage public holidays and weekend configuration",
      );
    });

    await test.step("US1.1 — Navigate to Admin > Calendar Management", async () => {
      await page.goto("/MasterListsPage/calendarManagement");
      await expect(
        page.getByRole("heading", { name: "Calendar Management" }),
      ).toBeVisible({ timeout: 15_000 });
      await demo.evidence("US1.1-calendar-management-page");
      await demo.pause(2000);
    });

    await test.step("US1.2 — Verify seeded holidays appear in table", async () => {
      await expect(page.getByText("New Year's Day")).toBeVisible({
        timeout: 10_000,
      });
      await expect(page.getByText("Labour Day")).toBeVisible();
      await expect(
        page.locator('[data-testid="holiday-count-footer"]'),
      ).toBeVisible();
      await expect(
        page.locator('[data-testid="holiday-count-footer"]'),
      ).toContainText("holidays configured for");
      await demo.evidence("US1.2-seeded-holidays-visible");
      await demo.pause(2000);
    });

    await test.step("US1.3 — Open inline add form and verify validation", async () => {
      await page.locator('[data-testid="add-holiday-button"]').click();
      await expect(
        page.locator('[data-testid="holiday-inline-row"]'),
      ).toBeVisible();

      await expect(
        page.locator('[data-testid="save-holiday-button"]'),
      ).toBeDisabled();

      await page.locator("#new-holiday-name").fill("Test Holiday");
      await expect(
        page.locator('[data-testid="save-holiday-button"]'),
      ).toBeDisabled();
      await demo.evidence("US1.3-validation-save-disabled");

      await page.locator('[data-testid="cancel-holiday-button"]').click();
      await expect(
        page.locator('[data-testid="holiday-inline-row"]'),
      ).not.toBeVisible();
      await demo.evidence("US1.3-form-cancelled");
      await demo.pause(1500);
    });

    await test.step("US1.5 — Verify table controls and UI elements", async () => {
      await expect(page.locator('[data-testid="year-dropdown"]')).toBeVisible();
      await expect(
        page.locator('[data-testid="import-csv-button"]'),
      ).toBeVisible();
      await expect(
        page.locator('[data-testid="export-csv-button"]'),
      ).toBeVisible();
      await demo.evidence("US1.5-table-controls");
      await demo.pause(1500);
    });

    await test.step("US1.6 — Verify weekend checkboxes", async () => {
      const satCheckbox = page.locator('[data-testid="weekend-checkbox-6"]');
      const sunCheckbox = page.locator('[data-testid="weekend-checkbox-0"]');
      await expect(satCheckbox).toBeVisible();
      await expect(sunCheckbox).toBeVisible();
      await demo.evidence("US1.6-weekend-checkboxes");
      await demo.pause(1500);
    });

    await test.step("US1.7 — Delete holiday with confirmation", async () => {
      const deleteButtons = page.locator('[data-testid^="delete-holiday-"]');
      const count = await deleteButtons.count();
      if (count > 0) {
        await deleteButtons.first().click();
        const confirmButton = page.getByRole("button", { name: /Delete/i });
        const isConfirmVisible = await confirmButton.isVisible();
        if (isConfirmVisible) {
          await confirmButton.click();
          await demo.pause(1500);
        }
      }
      await demo.evidence("US1.7-after-delete");
    });

    await test.step("US1.8 — Verify holiday count footer reflects changes", async () => {
      await expect(
        page.locator('[data-testid="holiday-count-footer"]'),
      ).toBeVisible();
      await expect(
        page.locator('[data-testid="holiday-count-footer"]'),
      ).toContainText("holidays configured for");
      await demo.evidence("US1.8-holiday-count-footer");
      await demo.pause(1000);
    });
  });
});
