import { test, expect } from "../../../helpers/test-base";
import { LONG_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Order workflow — Label & Store storage picker smoke.
 *
 * Precondition: the deep-linked order must already be at Step 3
 * (Label & Store). If it's earlier, OrderContext redirects away and
 * the picker won't render; in that case this test runs `test.skip()`
 * with a clear precondition message.
 *
 * Seed lookup: /rest/home-dashboard/ORDERS_IN_PROGRESS. If that
 * endpoint returns no orders, the test fails with an actionable
 * diagnostic rather than silently passing.
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

test.describe("Order workflow — Label & Store storage picker", () => {
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
    // ("Enter Order"). Skip the picker assertion with a clear reason.
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
