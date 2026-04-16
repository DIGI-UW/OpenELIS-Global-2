import { test, expect } from "../../../helpers/test-base";
import { LONG_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";
import {
  StorageLocationSelector,
  uniqueLocationName,
} from "../../../fixtures/storage-location-selector";

/**
 * Track A / Site 2 — Order workflow → Step 3 "Label & Store"
 *
 * Resumes an existing in-progress order via OrderContext's labNumber URL
 * param, then exercises the StorageLocationSelector embedded inline in
 * OrderLabel.jsx (legacy autocomplete mode).
 *
 * Seed lookup: /rest/home-dashboard/ORDERS_IN_PROGRESS returns
 * { paging, displayItems: [{ labNumber, ... }] }. Verified shape against
 * testing.openelis-global.org. If the test environment has no orders in
 * progress, the test fails with an actionable diagnostic — not silently
 * skipped. The fix is to seed the environment.
 */

async function findInProgressLabNumber(
  request: import("@playwright/test").APIRequestContext,
): Promise<string | null> {
  const response = await request.get("/rest/home-dashboard/ORDERS_IN_PROGRESS");
  if (!response.ok()) return null;
  const data = await response.json();
  return data?.displayItems?.[0]?.labNumber ?? null;
}

test.describe("Order workflow — Label & Store storage assignment", () => {
  test("assigns existing location on an in-progress order", async ({
    page,
    request,
  }) => {
    const labNumber = await findInProgressLabNumber(request);
    expect(
      labNumber,
      "test environment must have at least one in-progress order — see ORDERS_IN_PROGRESS endpoint",
    ).not.toBeNull();

    await page.goto(
      `/order/label?labNumber=${encodeURIComponent(labNumber!)}`,
      { waitUntil: "domcontentloaded", timeout: LONG_TIMEOUT },
    );

    const selector = new StorageLocationSelector(page);
    await selector.expectVisible();

    await selector.openSearchDropdown();
    await selector.selectLocationByText(/Shelf1/);

    // Inline auto-save renders the selection back into the form.
    await expect(page.getByText(/Shelf1/).first()).toBeVisible({
      timeout: UI_TIMEOUT,
    });
  });

  test("creates a new location inline on an in-progress order", async ({
    page,
    request,
  }) => {
    const labNumber = await findInProgressLabNumber(request);
    expect(
      labNumber,
      "test environment must have at least one in-progress order",
    ).not.toBeNull();

    await page.goto(
      `/order/label?labNumber=${encodeURIComponent(labNumber!)}`,
      { waitUntil: "domcontentloaded", timeout: LONG_TIMEOUT },
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

    await expect(page.getByText(newShelf).first()).toBeVisible({
      timeout: UI_TIMEOUT,
    });
  });
});
