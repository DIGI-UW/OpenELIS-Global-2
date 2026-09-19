import { test, expect } from "../../../helpers/test-base";
import type { Locator, Page } from "@playwright/test";
import { StorageManagement } from "../../../fixtures/storage-management";
import { LONG_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Sample Items tab — Manage Location in the shared picker modal.
 *
 * User story: admin navigates to /Storage/sample-items, clicks Manage
 * Location on a row → the LocationPickerModal opens over the listing
 * (the same modal the results and inventory surfaces use; there is no
 * dedicated page any more). They pick or create a location, confirm,
 * and the row in the listing behind the modal shows the new path.
 *
 * Fixture precondition: the environment must contain at least one
 * sample item and a device named "Freezer Unit 1". If either is
 * missing these tests fail loudly so CI surfaces the data/flow
 * mismatch rather than silently skipping.
 *
 * Selector strategy follows .specify/guides/playwright-best-practices.md:
 *   - getByRole with a strict accessible name (no blind .first())
 *   - option lookups scoped to their listbox
 *   - data-testid retained only where the project anchors stable hooks
 */

/** The picker modal as mounted for sample items, and its dialog. */
function picker(page: Page): Locator {
  return page.locator('[data-occupant-type="SAMPLE_ITEM"]');
}

function pickerDialog(page: Page): Locator {
  return picker(page).getByRole("dialog");
}

/** The listing row carrying this sample-item id in its first cell. */
function rowById(page: Page, id: string): Locator {
  return page
    .locator("tbody tr")
    .filter({ has: page.getByRole("cell", { name: id, exact: true }) });
}

/**
 * Opens Manage Location on a listing row and returns that row's id so
 * the same row can be found again after the post-save refetch. Disposed
 * rows are skipped: assigning one is refused with a 400, which is a
 * precondition failure rather than the behaviour under test. `rowIndex`
 * counts the assignable rows, not the rendered ones.
 */
async function openPickerFromRow(page: Page, rowIndex = 0): Promise<string> {
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

  const assignable: number[] = [];
  for (let i = 0; i < rowCount; i += 1) {
    const rowText = (await rows.nth(i).textContent()) ?? "";
    if (!/Disposed/i.test(rowText)) assignable.push(i);
  }
  if (assignable.length === 0) {
    throw new Error(
      "No assignable sample row on the listing — every row is disposed. " +
        "Reset or extend fixtures to include an active sample item.",
    );
  }

  const row = rows.nth(assignable[Math.min(rowIndex, assignable.length - 1)]);
  const rowId = (
    (await row.getByRole("cell").first().textContent()) ?? ""
  ).trim();
  await reopenPicker(page, row);
  return rowId;
}

async function reopenPicker(page: Page, row: Locator) {
  await row.locator('[data-testid="sample-actions-overflow-menu"]').click();
  await page.getByRole("menuitem", { name: /manage location/i }).click();
  // Spec 001 §240-241 + §1647-1649: the picker titles itself "Assign
  // Storage Location" for an unassigned sample and "Move Item" for a
  // pre-assigned one. The rest of the flow is identical.
  await expect(
    pickerDialog(page).getByRole("heading", {
      name: /(assign storage location|move item)/i,
    }),
  ).toBeVisible({ timeout: LONG_TIMEOUT });
  // The picker is a modal on the listing, not a route of its own.
  await expect(page).toHaveURL(/\/Storage\/sample-items(\?.*)?$/);
}

/**
 * Selects the one device the fixture data guarantees. Searching rather
 * than walking the room dropdown keeps the room's name out of the test:
 * the search result carries its ancestors, which is also what fills the
 * level cascade below.
 */
async function selectFreezerUnit1(page: Page) {
  await page.locator("#storage-location-picker-search-input").fill("Freezer");

  // The search returns the device AND its descendants (shelves, racks,
  // plates), whose breadcrumb labels all contain "Freezer Unit 1"; anchor to
  // the leaf so only the device-level option matches.
  const option = page.getByRole("option", { name: /freezer unit 1$/i });
  await expect(option).toBeVisible({ timeout: UI_TIMEOUT });
  await option.click();

  // The cascade adopts the searched device, which is what enables the
  // levels below it.
  await expect(page.locator("#location-picker-device")).toContainText(
    "Freezer Unit 1",
  );
}

async function createShelfUnderSelectedDevice(page: Page): Promise<string> {
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

  // Creating it also selects it, and the POST is in flight when the
  // button is released — wait for the cascade to adopt the new shelf
  // before anything confirms the picker.
  await expect(page.locator("#location-picker-shelf")).toContainText(
    uniqueShelf,
    { timeout: UI_TIMEOUT },
  );
  return uniqueShelf;
}

async function confirmPicker(page: Page) {
  await pickerDialog(page)
    .getByRole("button", { name: /^confirm$/i })
    .click({ timeout: UI_TIMEOUT });
  await expect(picker(page)).toBeHidden({ timeout: LONG_TIMEOUT });
}

test.describe("Sample Items page — Manage Location (picker modal)", () => {
  test("lists sample items on its tab with the container chrome", async ({
    page,
  }) => {
    const storage = new StorageManagement(page);
    await page.goto("/Storage/sample-items", { waitUntil: "domcontentloaded" });
    await expect(page).toHaveURL(/\/Storage\/sample-items/, {
      timeout: LONG_TIMEOUT,
    });

    // Breadcrumb + heading come from the container, not the tab.
    await storage.expectContainer();
    await storage.expectTabSelected("Sample Items");

    const rows = page.locator("table tbody tr");
    await expect(rows.first()).toBeVisible({ timeout: LONG_TIMEOUT });
  });

  test("assigns a device-level location via search and the row shows it", async ({
    page,
  }) => {
    const rowId = await openPickerFromRow(page, 0);

    await test.step("search for Freezer Unit 1 and select the device result", async () => {
      // Backend rejects room-level assignments (DB check constraint), so
      // the test selects a device-level option.
      await selectFreezerUnit1(page);
      await fillReasonIfMovement(page, "PW device assignment");
    });

    await test.step("confirm, and the refetched listing shows the new path", async () => {
      await confirmPicker(page);

      const storage = new StorageManagement(page);
      await storage.expectContainer();
      await storage.expectTabSelected("Sample Items");
      await expect(rowById(page, rowId)).toContainText(/freezer unit 1/i, {
        timeout: LONG_TIMEOUT,
      });
    });
  });

  test("creates a new shelf inline under the device and assigns it", async ({
    page,
  }) => {
    const rowId = await openPickerFromRow(page, 1);
    let uniqueShelf = "";

    // The level cascade renders alongside search; there is no toggle to open.
    await test.step("select the device, then inline-create a shelf under it", async () => {
      await selectFreezerUnit1(page);
      uniqueShelf = await createShelfUnderSelectedDevice(page);
    });

    await test.step("a pre-assigned row asks for a reason, so supply one", async () => {
      await fillReasonIfMovement(page, "PW inline shelf assignment");
    });

    await test.step("confirm, and the refetched listing shows the new shelf", async () => {
      await confirmPicker(page);
      await expect(rowById(page, rowId)).toContainText(uniqueShelf, {
        timeout: LONG_TIMEOUT,
      });
    });
  });

  test("moves an assigned sample and records the reason it was given", async ({
    page,
  }) => {
    const rowId = await openPickerFromRow(page, 2);

    await test.step("put the row somewhere first, so the next open is a move", async () => {
      // A freshly-created shelf is somewhere the row cannot already be,
      // whatever state the environment left it in.
      await selectFreezerUnit1(page);
      const startingShelf = await createShelfUnderSelectedDevice(page);
      await fillReasonIfMovement(page, "PW move precondition");
      await confirmPicker(page);
      await expect(rowById(page, rowId)).toContainText(startingShelf, {
        timeout: LONG_TIMEOUT,
      });
    });

    const reason = `PW move ${Date.now().toString(36)}`;
    let uniqueShelf = "";

    await test.step("reopening the assigned row is a move: current location and reason", async () => {
      await reopenPicker(page, rowById(page, rowId));
      const dialog = pickerDialog(page);
      await expect(
        dialog.getByRole("heading", { name: /move item/i }),
      ).toBeVisible();
      await expect(dialog).toContainText(/freezer unit 1/i);

      const reasonField = dialog.locator(
        "#storage-location-picker-modal-reason",
      );
      await expect(reasonField).toBeVisible();
      await reasonField.fill(reason);

      await selectFreezerUnit1(page);
      uniqueShelf = await createShelfUnderSelectedDevice(page);
    });

    await test.step("confirm, and the listing shows the moved-to shelf", async () => {
      await confirmPicker(page);
      await expect(rowById(page, rowId)).toContainText(uniqueShelf, {
        timeout: LONG_TIMEOUT,
      });
    });

    await test.step("the movement audit carries the reason the move was given", async () => {
      await rowById(page, rowId)
        .locator('[data-testid="sample-actions-overflow-menu"]')
        .click();
      await page.getByRole("menuitem", { name: /view audit/i }).click();
      const audit = page.locator('[data-testid="view-audit-modal"]');
      await expect(audit).toContainText(reason, { timeout: LONG_TIMEOUT });
    });
  });
});

/**
 * A sample that already sits somewhere is moved, not assigned, and the
 * picker then asks why. Rows arrive in whatever state the environment
 * left them, so the reason field is filled only when it is there.
 */
async function fillReasonIfMovement(page: Page, reason: string) {
  const reasonField = pickerDialog(page).locator(
    "#storage-location-picker-modal-reason",
  );
  if ((await reasonField.count()) > 0) {
    await reasonField.fill(reason);
  }
}
