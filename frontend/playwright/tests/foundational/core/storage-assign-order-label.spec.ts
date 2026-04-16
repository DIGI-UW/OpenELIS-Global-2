import { test, expect } from "../../../helpers/test-base";
import { LONG_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Phase 8 — Site 2 (Order Label & Store).
 *
 * OrderLabel.jsx now uses LocationPickerInline (the new picker's
 * inline shell), replacing the legacy StorageLocationSelector
 * mode="autocomplete" path. This was the last caller of the legacy
 * mode= path — after Phase 12 the legacy components are deleted.
 *
 * Precondition for the picker-visibility assertion: the deep-linked
 * order must already be at Step 3 (Label & Store). If the order is
 * still at Step 1 (Enter Order) or Step 2, the router redirects away
 * and the picker won't render. That's pre-existing OrderContext
 * behavior, unrelated to the picker refactor. If the test
 * environment has no order at Step 3, this test runs `test.skip()`
 * with a clear precondition message — not a silent pass.
 *
 * Seed lookup: /rest/home-dashboard/ORDERS_IN_PROGRESS. If the
 * endpoint returns no orders at all, this spec fails with an
 * actionable diagnostic.
 */

async function findInProgressLabNumber(
  request: import("@playwright/test").APIRequestContext,
): Promise<string | null> {
  const response = await request.get(
    "/api/OpenELIS-Global/rest/home-dashboard/ORDERS_IN_PROGRESS",
  );
  if (!response.ok()) return null;
  const data = await response.json();
  return data?.displayItems?.[0]?.labNumber ?? null;
}

test.describe("Order workflow — Label & Store storage picker (Phase 8)", () => {
  test("renders the new LocationPickerInline when the order is at Step 3", async ({
    page,
    request,
  }) => {
    const labNumber = await findInProgressLabNumber(request);
    expect(
      labNumber,
      "test environment must have at least one in-progress order — " +
        "see /rest/home-dashboard/ORDERS_IN_PROGRESS endpoint",
    ).not.toBeNull();

    await page.goto(
      `/order/label?labNumber=${encodeURIComponent(labNumber!)}`,
      { waitUntil: "domcontentloaded", timeout: LONG_TIMEOUT },
    );

    // If the order isn't at Step 3, OrderContext routes us to Step 1
    // ("Enter Order"). That's a pre-existing behavior (filed as the
    // BUG-2 follow-up in the Phase 0 cherry-pick commit). Skip the
    // picker assertion with a clear reason.
    const stepHeading = await page
      .getByRole("heading", { level: 2 })
      .first()
      .textContent({ timeout: UI_TIMEOUT });
    test.skip(
      !/label.*store/i.test(stepHeading ?? ""),
      `Order ${labNumber} is at "${stepHeading?.trim()}" (not Step 3 ` +
        "Label & Store). Seed an order past Steps 1-2 to exercise the picker.",
    );

    // Order IS at Label & Store → the new picker must be present.
    await expect(
      page.locator("#storage-location-picker-search-input"),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
  });
});
