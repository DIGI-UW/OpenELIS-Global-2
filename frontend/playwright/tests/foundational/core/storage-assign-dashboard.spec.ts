import { test, expect } from "../../../helpers/test-base";
import { LONG_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Sample Items page + dedicated Manage Location page.
 *
 * User story: admin navigates to /Storage/sample-items, clicks Manage
 * Location on an unassigned row → LocationPickerPage at
 * /Storage/sample-items/{id}/manage-location. They pick or create a
 * location, click Save, and are navigated back.
 *
 * This spec no longer hardcodes fixture IDs. It starts from the current
 * table state, opens Manage Location from row actions, and validates
 * outcomes through visible UI state.
 */

async function openManageLocationFromRow(page, rowIndex = 0) {
  await page.goto("/Storage/sample-items", { waitUntil: "domcontentloaded" });
  await expect(
    page.getByRole("heading", { level: 1, name: /sample items/i }),
  ).toBeVisible({ timeout: LONG_TIMEOUT });

  const rows = page.locator("table tbody tr");
  const rowCount = await rows.count();
  expect(
    rowCount,
    "Expected at least one sample row to open Manage Location",
  ).toBeGreaterThan(0);

  const row = rows.nth(Math.min(rowIndex, rowCount - 1));
  await row.locator('[data-testid="sample-actions-overflow-menu"]').click();
  await page.getByRole("menuitem", { name: /manage location/i }).click();
  await expect(
    page.getByRole("heading", { level: 1, name: /assign storage location/i }),
  ).toBeVisible({ timeout: LONG_TIMEOUT });
}

test.describe("Sample Items page — Manage Location (dedicated page)", () => {
  test("lists sample items with header and table rendered", async ({
    page,
  }) => {
    await page.goto("/Storage/sample-items", { waitUntil: "domcontentloaded" });
    await expect(page).toHaveURL(/\/Storage\/sample-items/, {
      timeout: LONG_TIMEOUT,
    });
    await expect(
      page.getByRole("heading", { level: 1, name: /sample items/i }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(page.locator("table tbody tr").first()).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
    // Breadcrumb with Storage > Sample Items
    const nav = page.getByRole("navigation", { name: /breadcrumb/i });
    await expect(nav).toBeVisible();
    await expect(nav.getByRole("link", { name: /^storage$/i })).toBeVisible();
  });

  test("assigns a device-level location via search on the dedicated page", async ({
    page,
  }) => {
    await openManageLocationFromRow(page, 0);

    // Pick a device (backend rejects room-level — DB check constraint).
    const search = page.locator("#storage-location-picker-search-input");
    await search.fill("Freezer");
    const option = page
      .getByRole("option", { name: /freezer unit 1/i })
      .first();
    await expect(option).toBeVisible({ timeout: UI_TIMEOUT });
    await option.click();

    await page
      .getByRole("button", { name: /^save$/i })
      .click({ timeout: UI_TIMEOUT });

    await expect(page).toHaveURL(/\/Storage\/sample-items(\?.*)?$/, {
      timeout: LONG_TIMEOUT,
    });
    // List page re-rendered — the header is back.
    await expect(
      page.getByRole("heading", { level: 1, name: /sample items/i }),
    ).toBeVisible({ timeout: UI_TIMEOUT });
  });

  test("creates a new shelf inline via cascade and assigns it", async ({
    page,
  }) => {
    await openManageLocationFromRow(page, 1);

    await page
      .getByRole("button", { name: /create new location/i })
      .click({ timeout: UI_TIMEOUT });

    // Room → Main Laboratory
    const roomTrigger = page
      .locator("#location-picker-room button.cds--list-box__field")
      .first();
    await roomTrigger.click();
    await page
      .getByRole("option", { name: /main laboratory/i })
      .first()
      .click({ timeout: UI_TIMEOUT });

    // Device → Freezer Unit 1
    const deviceTrigger = page
      .locator("#location-picker-device button.cds--list-box__field")
      .first();
    await expect(deviceTrigger).toBeEnabled({ timeout: UI_TIMEOUT });
    await deviceTrigger.click();
    await page
      .getByRole("option", { name: /freezer unit 1/i })
      .first()
      .click({ timeout: UI_TIMEOUT });

    // Inline-create a unique Shelf under the selected Device.
    const shelfRow = page
      .locator("#location-picker-shelf")
      .locator("xpath=ancestor::*[contains(@class,'create-row')][1]");
    await shelfRow
      .getByRole("button", { name: /add new/i })
      .click({ timeout: UI_TIMEOUT });

    const uniqueShelf = `PWShelf-${Date.now().toString(36)}`;
    await page.locator("#location-picker-inline-create-name").fill(uniqueShelf);
    await page
      .getByRole("button", { name: /^create$/i })
      .click({ timeout: UI_TIMEOUT });

    await page
      .getByRole("button", { name: /^save$/i })
      .click({ timeout: UI_TIMEOUT });

    await expect(page).toHaveURL(/\/Storage\/sample-items(\?.*)?$/, {
      timeout: LONG_TIMEOUT,
    });
  });
});
