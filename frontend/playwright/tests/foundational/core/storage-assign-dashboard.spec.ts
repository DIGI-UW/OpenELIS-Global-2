import { test, expect, Locator } from "../../../helpers/test-base";
import { LONG_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Phase 5a (RED) — Sample Items page + dedicated Manage Location page.
 *
 * User story:
 *   Admin navigates to the new /Storage/sample-items route (replacing the
 *   old /Storage/samples Tab). On an unassigned row, "Manage Location"
 *   navigates to /Storage/sample-items/{id}/manage-location — a dedicated
 *   LocationPickerPage with breadcrumb + page header. The admin picks or
 *   creates a location, clicks Save, and is navigated back to the list.
 *
 * Architecture the spec asserts:
 *   - SampleItemsPage at /Storage/sample-items (not a Tab in StorageDashboard)
 *   - ManageLocationPage at /Storage/sample-items/:id/manage-location
 *     composing the new LocationPickerPage (SearchField + CreateForm)
 *   - No modal — page navigation replaces LocationManagementModal
 *
 * RED expectation (Phase 5a):
 *   The /Storage/sample-items route does not exist yet — App.js still has
 *   the /Storage/:tab catch-all pointing at StorageDashboard. This spec
 *   therefore fails at beforeEach's heading assertion, or at the
 *   navigation to /manage-location. It goes GREEN in Phase 5b when the
 *   page extraction lands.
 */

/**
 * Find a sample-item row whose "Storage location" cell is empty (i.e. no
 * hierarchy path containing ">"). The new SampleItemsPage should render a
 * <table> with one row per unassigned or assigned sample; assigned rows
 * show a path like "Room > Device > Shelf".
 */
async function findUnassignedRow(page: any): Promise<Locator> {
  const rows = page.locator("table tbody tr");
  const total = await rows.count();
  for (let i = 0; i < total; i++) {
    const row = rows.nth(i);
    const text = (await row.textContent()) ?? "";
    if (text.includes(">")) continue;
    return row;
  }
  throw new Error(
    `No unassigned sample item found in ${total} rows — seed at least one ` +
      `SampleItem without a storage assignment so this regression spec can run.`,
  );
}

test.describe("Sample Items page — Manage Location (dedicated page)", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/Storage/sample-items", { waitUntil: "domcontentloaded" });
    await expect(page).toHaveURL(/\/Storage\/sample-items/, {
      timeout: LONG_TIMEOUT,
    });
    // The new page has an h1 "Sample Items" (not a Carbon Tab panel) and
    // a breadcrumb above it. Assert the page header landed before
    // interacting with the table.
    await expect(
      page.getByRole("heading", { level: 1, name: /sample items/i }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(page.locator("table tbody tr").first()).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
  });

  test("navigates to manage-location page and assigns via search", async ({
    page,
  }) => {
    const row = await findUnassignedRow(page);
    await row.getByRole("button", { name: /sample actions/i }).click();
    await page
      .getByRole("menuitem", { name: /manage location/i })
      .click({ timeout: UI_TIMEOUT });

    // Navigation to the dedicated picker page, NOT a modal opening.
    await expect(page).toHaveURL(
      /\/Storage\/sample-items\/.+\/manage-location/,
      { timeout: LONG_TIMEOUT },
    );
    await expect(
      page.getByRole("heading", {
        level: 1,
        name: /assign storage location/i,
      }),
    ).toBeVisible({ timeout: UI_TIMEOUT });

    // Use the SearchField to type 2+ chars and pick a pre-seeded location.
    // The default test fixtures include a "Main Laboratory" room.
    const search = page.locator("#storage-location-picker-search-input");
    await search.fill("Main");
    const option = page.getByRole("option", { name: /main/i }).first();
    await expect(option).toBeVisible({ timeout: UI_TIMEOUT });
    await option.click();

    await page
      .getByRole("button", { name: /^save$/i })
      .click({ timeout: UI_TIMEOUT });

    // Page navigates back to /Storage/sample-items on save.
    await expect(page).toHaveURL(/\/Storage\/sample-items(\?.*)?$/, {
      timeout: LONG_TIMEOUT,
    });
    // The row the user came from now shows a hierarchical path containing ">".
    await expect(page.getByText(/>/).first()).toBeVisible({
      timeout: UI_TIMEOUT,
    });
  });

  test("creates a new storage location inline via cascade and assigns it", async ({
    page,
  }) => {
    const row = await findUnassignedRow(page);
    await row.getByRole("button", { name: /sample actions/i }).click();
    await page
      .getByRole("menuitem", { name: /manage location/i })
      .click({ timeout: UI_TIMEOUT });

    await expect(page).toHaveURL(
      /\/Storage\/sample-items\/.+\/manage-location/,
      { timeout: LONG_TIMEOUT },
    );

    // Toggle from search to cascading-create.
    await page
      .getByRole("button", { name: /create new location/i })
      .click({ timeout: UI_TIMEOUT });

    // Room dropdown: open, select "Main Laboratory" (seeded).
    const roomTrigger = page
      .locator("#location-picker-room button.cds--list-box__field")
      .first();
    await roomTrigger.click();
    await page
      .getByRole("option", { name: /main laboratory/i })
      .first()
      .click({ timeout: UI_TIMEOUT });

    // Device dropdown: seeded device under that room.
    const deviceTrigger = page
      .locator("#location-picker-device button.cds--list-box__field")
      .first();
    await expect(deviceTrigger).toBeEnabled({ timeout: UI_TIMEOUT });
    await deviceTrigger.click();
    await page
      .getByRole("option", { name: /freezer/i })
      .first()
      .click({ timeout: UI_TIMEOUT });

    // Inline-create a new Shelf under the selected Room > Device.
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
    await expect(page.getByText(new RegExp(uniqueShelf))).toBeVisible({
      timeout: UI_TIMEOUT,
    });
  });
});
