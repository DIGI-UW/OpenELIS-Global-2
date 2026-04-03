import { test, expect } from "@playwright/test";
import { showTitleCard } from "../../../helpers/title-card";
import { videoPause } from "../../../helpers/video-pause";

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
      // Navigate directly to the Calendar Management page
      await page.goto("/MasterListsPage/calendarManagement");
      await expect(
        page.getByRole("heading", { name: "Calendar Management" }),
      ).toBeVisible({ timeout: 15_000 });
      await videoPause(page, 2000, testInfo);
    });

    await test.step("US1.2 — Verify holiday table renders", async () => {
      // Table should be visible (may be empty or have seeded data)
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
      await videoPause(page, 1500, testInfo);
    });

    await test.step("US1.3 — Add a holiday inline", async () => {
      await page.locator('[data-testid="add-holiday-button"]').click();
      await expect(
        page.locator('[data-testid="holiday-inline-row"]'),
      ).toBeVisible();

      // Fill in date and name
      await page.locator("#new-holiday-date").fill("2026-07-04");
      await page.locator("#new-holiday-name").fill("Liberation Day");

      // Save button should be enabled now
      const saveBtn = page.locator('[data-testid="save-holiday-button"]');
      await expect(saveBtn).toBeEnabled();

      await saveBtn.click();
      await videoPause(page, 2000, testInfo);

      // Holiday should appear in table
      await expect(page.getByText("Liberation Day")).toBeVisible({
        timeout: 10_000,
      });
    });

    await test.step("US1.4 — Verify weekend checkboxes are visible", async () => {
      // Saturday and Sunday should be checked by default
      const satCheckbox = page.locator('[data-testid="weekend-checkbox-6"]');
      const sunCheckbox = page.locator('[data-testid="weekend-checkbox-0"]');
      await expect(satCheckbox).toBeVisible();
      await expect(sunCheckbox).toBeVisible();
      await videoPause(page, 1500, testInfo);
    });

    await test.step("US1.5 — Verify holiday count footer", async () => {
      await expect(
        page.locator('[data-testid="holiday-count-footer"]'),
      ).toBeVisible();
      await expect(
        page.locator('[data-testid="holiday-count-footer"]'),
      ).toContainText("holidays configured for");
      await videoPause(page, 1000, testInfo);
    });
  });
});
