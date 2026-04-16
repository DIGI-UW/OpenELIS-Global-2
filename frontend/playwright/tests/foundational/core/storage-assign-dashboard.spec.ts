import { test, expect } from "../../../helpers/test-base";
import { LONG_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";
import {
  LocationManagementModal,
  openManageLocationForUnassignedSample,
  uniqueLocationName,
} from "../../../fixtures/storage-location-selector";

/**
 * Track A / Site 1 — Storage Dashboard → Sample Items tab
 *
 * User story:
 *   Admin opens /Storage/samples, finds an unassigned sample item, clicks
 *   the overflow menu → "Manage Location", and either (a) picks an existing
 *   location from the tree/search or (b) creates a new one inline.
 *
 * Why this spec exists:
 *   The equivalent Cypress coverage at frontend/cypress/e2e/storageAssignment.cy.js
 *   has .skip() on every meaningful dropdown test, so CI was green while the
 *   feature was broken. This spec actually exercises the dropdown.
 *
 * Seed expectation:
 *   At least one SampleItem without a storage assignment in the test DB.
 *   At least one active Room → Device → Shelf hierarchy ("Room1 > Fridge1 > Shelf1"
 *   on testing.openelis-global.org).
 */
test.describe("Storage Dashboard — Sample Item location assignment", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/Storage/samples", { waitUntil: "domcontentloaded" });
    await expect(page).toHaveURL(/\/Storage\/samples/, {
      timeout: LONG_TIMEOUT,
    });
    // Wait for the samples table to render before any row interactions.
    await expect(page.locator("table").first()).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
  });

  test("assigns an existing storage location via tree/search", async ({
    page,
  }) => {
    const modal = await openManageLocationForUnassignedSample(page);

    // User clicks into the search input → tree view opens.
    await modal.selector.openSearchDropdown();

    // Pick a location that's known to exist on the test server (Room1 →
    // Fridge1 → Shelf1 is the default seeded hierarchy). The tree view
    // renders text labels, so match by visible text rather than id.
    await modal.selector.selectLocationByText(/Shelf/i);

    await modal.confirm();

    // Modal closes on success; the row the user came from now shows a
    // hierarchical path containing ">".
    await modal.expectClosed();
    await expect(page.getByText(/assigned successfully/i).first()).toBeVisible({
      timeout: UI_TIMEOUT,
    });
  });

  test("creates a new storage location inline and assigns it", async ({
    page,
  }) => {
    const modal = await openManageLocationForUnassignedSample(page);

    await modal.selector.clickAddLocation();

    // Cascading comboboxes: reuse an existing Room/Device, type a new Shelf.
    // FR-033a requires minimum 2 levels (room + device). Names match the
    // seeded "Main Laboratory > Freezer Unit 1" hierarchy on
    // testing.openelis-global.org; the shelf is unique-per-run so create
    // never collides.
    const newShelf = uniqueLocationName("PWShelf");
    await modal.selector.fillCascadingCreate({
      room: "Main Laboratory",
      device: "Freezer Unit 1",
      shelf: newShelf,
    });

    await modal.selector.confirmInlineCreate();
    await modal.confirm();

    await modal.expectClosed();
    await expect(page.getByText(/assigned successfully/i).first()).toBeVisible({
      timeout: UI_TIMEOUT,
    });
  });
});
