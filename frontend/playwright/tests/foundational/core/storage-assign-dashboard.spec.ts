import { test, expect } from "../../../helpers/test-base";
import type { Page } from "@playwright/test";
import { StorageManagement } from "../../../fixtures/storage-management";
import { LONG_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Sample Items tab + dedicated Manage Location page.
 *
 * User story: admin navigates to /Storage/sample-items, clicks Manage
 * Location on a row → LocationPickerPage at
 * /Storage/sample-items/{id}/manage-location. They pick or create a
 * location, click Save, and are navigated back.
 *
 * Sample Items is a tab of the Storage Management container now, so the
 * container owns the breadcrumb and heading and the tab's own <h1> is gone.
 * Manage Location remains a standalone route with its own heading.
 *
 * Fixture precondition: the environment must contain at least one
 * sample item, and a device named "Freezer Unit 1" under a room
 * named "Main Laboratory". If either is missing these tests fail
 * loudly so CI surfaces the data/flow mismatch rather than silently
 * skipping.
 *
 * Selector strategy follows .specify/guides/playwright-best-practices.md:
 *   - getByRole with a strict accessible name (no blind .first())
 *   - option lookups scoped to their listbox
 *   - data-testid retained only where the project anchors stable hooks
 */

async function openManageLocationFromRow(page: Page, rowIndex = 0) {
  const storage = new StorageManagement(page);
  await page.goto("/Storage/sample-items", { waitUntil: "domcontentloaded" });
  await storage.expectContainer();
  await storage.expectTabSelected("Sample Items");

  // Auto-retrying wait: `toBeVisible` polls until the table-row XHR
  // completes and at least one row hydrates. `locator.count()` is a
  // one-shot snapshot (non-retrying) so it must only run AFTER the
  // DOM has stabilized — otherwise it flakes under cold-runner
  // hydration delays.
  const rows = page.locator("table tbody tr");
  await expect(
    rows.first(),
    "Expected at least one sample row to open Manage Location — " +
      "seed sample items before exercising this flow.",
  ).toBeVisible({ timeout: LONG_TIMEOUT });
  const rowCount = await rows.count();

  const row = rows.nth(Math.min(rowIndex, rowCount - 1));
  await row.locator('[data-testid="sample-actions-overflow-menu"]').click();
  await page.getByRole("menuitem", { name: /manage location/i }).click();
  // Spec 001 §240-241 + §1647-1649: picker renders "Assign Storage Location"
  // for unassigned samples and "Move Sample" for pre-assigned ones. The
  // subsequent UX (search → pick → save → navigate back) is identical.
  await expect(
    page.getByRole("heading", {
      level: 1,
      name: /(assign storage location|move item)/i,
    }),
  ).toBeVisible({ timeout: LONG_TIMEOUT });
}

test.describe("Sample Items page — Manage Location (dedicated page)", () => {
  test("lists sample items on its tab with the container chrome", async ({
    page,
  }) => {
    const storage = new StorageManagement(page);
    await page.goto("/Storage/sample-items", { waitUntil: "domcontentloaded" });
    await expect(page).toHaveURL(/\/Storage\/sample-items/, {
      timeout: LONG_TIMEOUT,
    });

    // Breadcrumb + heading now come from the container, not the tab.
    await storage.expectContainer();
    await storage.expectTabSelected("Sample Items");

    const rows = page.locator("table tbody tr");
    await expect(rows.first()).toBeVisible({ timeout: LONG_TIMEOUT });
  });

  test("assigns a device-level location via search on the dedicated page", async ({
    page,
  }) => {
    await openManageLocationFromRow(page, 0);

    await test.step("search for Freezer Unit 1 and select the device result", async () => {
      // Backend rejects room-level assignments (DB check constraint), so
      // the test selects a device-level option.
      await page
        .locator("#storage-location-picker-search-input")
        .fill("Freezer");

      // The search returns the device AND its descendants (shelves, racks,
      // plates), whose breadcrumb labels all contain "Freezer Unit 1"; anchor to
      // the leaf so only the device-level option matches.
      const option = page.getByRole("option", {
        name: /freezer unit 1$/i,
      });
      await expect(option).toBeVisible({ timeout: UI_TIMEOUT });
      await option.click();
    });

    await test.step("save and verify navigation back", async () => {
      await page
        .getByRole("button", { name: /^save$/i })
        .click({ timeout: UI_TIMEOUT });

      await expect(page).toHaveURL(/\/Storage\/sample-items(\?.*)?$/, {
        timeout: LONG_TIMEOUT,
      });
      const storage = new StorageManagement(page);
      await storage.expectContainer();
      await storage.expectTabSelected("Sample Items");
    });
  });

  test("creates a new shelf inline via cascade and assigns it", async ({
    page,
  }) => {
    await openManageLocationFromRow(page, 1);

    // The level cascade renders alongside search; there is no toggle to open.
    await test.step("choose room → device", async () => {
      const roomScope = page.locator("#location-picker-room");
      await roomScope.locator("button.cds--list-box__field").click();
      await roomScope
        .getByRole("option", { name: /main laboratory/i })
        .click({ timeout: UI_TIMEOUT });

      const deviceScope = page.locator("#location-picker-device");
      const deviceTrigger = deviceScope.locator("button.cds--list-box__field");
      await expect(deviceTrigger).toBeEnabled({ timeout: UI_TIMEOUT });
      await deviceTrigger.click();
      await deviceScope
        .getByRole("option", { name: /freezer unit 1/i })
        .click({ timeout: UI_TIMEOUT });
    });

    await test.step("inline-create a unique shelf under the selected device", async () => {
      const shelfRow = page
        .locator("#location-picker-shelf")
        .locator("xpath=ancestor::*[contains(@class,'create-row')][1]");
      await shelfRow
        .getByRole("button", { name: /add new/i })
        .click({ timeout: UI_TIMEOUT });

      const uniqueShelf = `PWShelf-${Date.now().toString(36)}`;
      await page
        .locator("#location-picker-inline-create-name")
        .fill(uniqueShelf);
      await page
        .getByRole("button", { name: /^create$/i })
        .click({ timeout: UI_TIMEOUT });
    });

    await test.step("save and verify navigation back", async () => {
      await page
        .getByRole("button", { name: /^save$/i })
        .click({ timeout: UI_TIMEOUT });

      await expect(page).toHaveURL(/\/Storage\/sample-items(\?.*)?$/, {
        timeout: LONG_TIMEOUT,
      });
    });
  });
});
