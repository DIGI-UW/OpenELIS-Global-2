import { test, expect } from "../../../helpers/test-base";
import { LONG_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";
import {
  StorageLocationSelector,
  uniqueLocationName,
} from "../../../fixtures/storage-location-selector";

/**
 * Track A / Site 2 — Order workflow → Step 3 "Label & Store"
 *
 * User story:
 *   Admin creates an order via /order/enter → collects the sample at
 *   /order/collect → arrives at /order/label where the StorageLocationSelector
 *   is embedded inline in the form (legacy autocomplete mode, not a modal).
 *
 * Why this spec matters:
 *   OrderLabel.jsx is the canonical "assign storage to a fresh sample during
 *   order entry" workflow. If the selector breaks here, users can't attach
 *   storage to a new sample — the core use case.
 *
 * Navigation strategy:
 *   OrderDashboard supports deep-linking via ?labNumber=XYZ which hydrates
 *   OrderContext with an existing order. We resume an in-progress order
 *   rather than creating one from scratch (which would require patient entry,
 *   sample type selection, etc. — out of scope for a focused regression test).
 *
 * Seed expectation:
 *   At least one in-progress order on the test server — enumerated via
 *   the `/rest/home-dashboard/home` or a similar "orders in progress" feed.
 *   If none exist, the test reports a skip with a clear diagnostic.
 */
test.describe("Order workflow — Label & Store storage assignment", () => {
  test("assigns existing location on an in-progress order", async ({
    page,
    request,
  }) => {
    // Find any in-progress order we can resume. The Home dashboard's
    // "orders in progress" card exposes this via the same REST endpoint
    // the UI uses — preferring that over hardcoded test data.
    const inProgress = await request
      .get("/rest/home-dashboard/ordersInProgress")
      .then((r) => (r.ok() ? r.json() : null))
      .catch(() => null);

    const labNumber =
      Array.isArray(inProgress) && inProgress.length > 0
        ? (inProgress[0].labNumber ??
          inProgress[0].accessionNumber ??
          inProgress[0].id)
        : null;

    test.skip(
      !labNumber,
      "No in-progress order on the test server — seed one to run this regression spec. " +
        "See the spec header for the required seed shape.",
    );

    // Resume the order directly at the Label & Store step. OrderContext
    // reads labNumber from the URL param and fetches /rest/order/search.
    await page.goto(
      `/order/label?labNumber=${encodeURIComponent(labNumber!)}`,
      {
        waitUntil: "domcontentloaded",
        timeout: LONG_TIMEOUT,
      },
    );

    // The Label & Store form renders only after OrderContext resolves.
    // Wait for the embedded selector to appear before interacting.
    const selector = new StorageLocationSelector(page);
    await selector.expectVisible();

    await selector.openSearchDropdown();
    await selector.selectLocationByText(/Shelf1/);

    // Label & Store auto-saves selections through OrderContext; there is
    // no modal to close. Confirm by asserting the selected location
    // renders somewhere in the form (hierarchical path or name).
    await expect(page.getByText(/Shelf1/).first()).toBeVisible({
      timeout: UI_TIMEOUT,
    });
  });

  test("creates a new location inline on an in-progress order", async ({
    page,
    request,
  }) => {
    const inProgress = await request
      .get("/rest/home-dashboard/ordersInProgress")
      .then((r) => (r.ok() ? r.json() : null))
      .catch(() => null);

    const labNumber =
      Array.isArray(inProgress) && inProgress.length > 0
        ? (inProgress[0].labNumber ??
          inProgress[0].accessionNumber ??
          inProgress[0].id)
        : null;

    test.skip(
      !labNumber,
      "No in-progress order on the test server — seed one to run this regression spec.",
    );

    await page.goto(
      `/order/label?labNumber=${encodeURIComponent(labNumber!)}`,
      {
        waitUntil: "domcontentloaded",
        timeout: LONG_TIMEOUT,
      },
    );

    const selector = new StorageLocationSelector(page);
    await selector.expectVisible();

    await selector.clickAddLocation();
    const newShelf = uniqueLocationName("OrderShelf");
    await selector.fillCascadingCreate({
      room: "Room1",
      device: "Fridge1",
      shelf: newShelf,
    });
    await selector.confirmInlineCreate();

    // Newly created shelf appears as the selected location.
    await expect(page.getByText(newShelf).first()).toBeVisible({
      timeout: UI_TIMEOUT,
    });
  });
});
