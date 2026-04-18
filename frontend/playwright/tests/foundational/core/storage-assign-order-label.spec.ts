import { test, expect } from "../../../helpers/test-base";
import type { APIRequestContext } from "@playwright/test";
import { LONG_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Order workflow — Label & Store storage picker smoke.
 *
 * Precondition: the deep-linked order must already be at Step 3
 * (Label & Store). If it's earlier, OrderContext redirects away and
 * the picker won't render — that's a real precondition failure, not
 * a skip condition.
 *
 * Seed lookup: /rest/home-dashboard/ORDERS_IN_PROGRESS. If that
 * endpoint returns no orders the test fails with an actionable
 * diagnostic rather than silently passing.
 */

async function findInProgressLabNumber(
  request: APIRequestContext,
): Promise<string> {
  const response = await request.get(
    "/api/OpenELIS-Global/rest/home-dashboard/ORDERS_IN_PROGRESS",
  );
  expect(
    response.ok(),
    `ORDERS_IN_PROGRESS endpoint returned ${response.status()} — ` +
      "API must be reachable and authenticated before running this spec",
  ).toBe(true);

  const data = await response.json();
  const lab = data?.displayItems?.[0]?.labNumber as string | undefined;
  expect(
    lab,
    "test environment must expose at least one in-progress order; " +
      "seed one before running this spec",
  ).toBeDefined();

  return lab as string;
}

test.describe("Order workflow — Label & Store storage picker", () => {
  test("renders the new LocationPickerInline when the order is at Step 3", async ({
    page,
    request,
  }) => {
    const labNumber = await test.step("fetch an in-progress order", async () =>
      findInProgressLabNumber(request));

    await test.step("deep-link into the order at Step 3", async () => {
      await page.goto(
        `/order/label?labNumber=${encodeURIComponent(labNumber)}`,
        { waitUntil: "domcontentloaded", timeout: LONG_TIMEOUT },
      );
    });

    await test.step("assert Step 3 heading (fail loudly if earlier)", async () => {
      // OrderContext routes back to Step 1 if prerequisites aren't met, so
      // asserting the Label & Store heading doubles as the precondition check.
      await expect(
        page.getByRole("heading", { level: 2, name: /label.*store/i }),
        `Order ${labNumber} must be at Step 3 (Label & Store)`,
      ).toBeVisible({ timeout: UI_TIMEOUT });
    });

    await test.step("storage picker is rendered", async () => {
      await expect(
        page.locator("#storage-location-picker-search-input"),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
    });
  });
});
