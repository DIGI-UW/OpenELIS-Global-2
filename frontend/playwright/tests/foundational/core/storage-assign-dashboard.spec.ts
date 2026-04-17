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
 * The spec uses known fixture IDs (10051 / 10072) instead of scanning
 * for an unassigned row — the list endpoint's hierarchy resolver can
 * return `location=''` for some assigned rows, making a text heuristic
 * unreliable. Ground truth for unassigned items:
 *   SELECT si.id FROM sample_item si
 *   LEFT JOIN sample_storage_assignment ssa
 *     ON ssa.sample_item_id = si.id
 *   WHERE ssa.id IS NULL;
 *
 * The DB check constraint `chk_location_type_valid` rejects
 * `location_type = 'room'`, so the spec picks a device-level result
 * ("Freezer Unit 1").
 */

// Each test consumes one unassigned sample_item per run — after the
// test asserts success, the DB state permanently flips that item to
// "assigned". Running the spec twice against the same DB without
// `load-test-fixtures.sh --reset` in between is expected to fail.
// Using distinct IDs per test so parallel execution doesn't collide.
const ASSIGN_SEARCH_SAMPLE_ITEM_ID = "10051";
const ASSIGN_CASCADE_SAMPLE_ITEM_ID = "10072";

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
    await page.goto(
      `/Storage/sample-items/${ASSIGN_SEARCH_SAMPLE_ITEM_ID}/manage-location`,
      { waitUntil: "domcontentloaded" },
    );
    await expect(
      page.getByRole("heading", { level: 1, name: /assign storage location/i }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });

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
    await page.goto(
      `/Storage/sample-items/${ASSIGN_CASCADE_SAMPLE_ITEM_ID}/manage-location`,
      { waitUntil: "domcontentloaded" },
    );
    await expect(
      page.getByRole("heading", { level: 1, name: /assign storage location/i }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });

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
