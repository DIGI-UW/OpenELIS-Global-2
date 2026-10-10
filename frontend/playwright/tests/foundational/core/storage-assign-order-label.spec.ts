import { test, expect } from "../../../helpers/test-base";
import type { APIRequestContext } from "@playwright/test";
import { LONG_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Order workflow — storage on Prepare Samples (OGC-1266 M4).
 *
 * Storage is assigned on the Prepare Samples step of a clinical order; the
 * separate Label & Store step is gone and its address opens Prepare Samples.
 * The picker renders for an order that has a requested sample.
 *
 * Seed lookup: /rest/home-dashboard/ORDERS_IN_PROGRESS. If that endpoint
 * returns no orders the test fails with an actionable diagnostic rather than
 * silently passing.
 */

async function findInProgressLabNumber(
  request: APIRequestContext,
): Promise<string> {
  const response = await request.get(
    "/api/OpenELIS-Global/rest/home-dashboard/ORDERS_IN_PROGRESS",
  );
  // Explicit status assertion (not response.ok() — see
  // .specify/guides/playwright-best-practices.md). This is a seed-lookup
  // precondition; UI assertions below are the real pass/fail.
  expect(
    response.status(),
    "ORDERS_IN_PROGRESS endpoint must be reachable and authenticated " +
      "before running this spec",
  ).toBeLessThan(400);

  const data = await response.json();
  const lab = data?.displayItems?.[0]?.labNumber as string | undefined;
  expect(
    lab,
    "test environment must expose at least one in-progress order; " +
      "seed one before running this spec",
  ).toBeDefined();

  return lab as string;
}

test.describe("Order workflow — Prepare Samples storage picker", () => {
  test("renders the storage picker on Prepare Samples", async ({
    page,
    request,
  }) => {
    const labNumber = await test.step("fetch an in-progress order", async () =>
      findInProgressLabNumber(request));

    await test.step("deep-link into the order at Prepare Samples", async () => {
      await page.goto(
        `/order/clinical/collect?order=${encodeURIComponent(labNumber)}`,
        { waitUntil: "domcontentloaded", timeout: LONG_TIMEOUT },
      );
      await expect(
        page.getByRole("heading", { level: 2, name: "Prepare Samples" }),
      ).toBeVisible({ timeout: UI_TIMEOUT });
    });

    await test.step("storage picker is rendered inside the step", async () => {
      const storage = page.getByTestId("prepare-storage-section");
      await expect(storage).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(
        storage.locator("#storage-location-picker-search-input"),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
    });
  });

  test("the retired Label & Store address opens Prepare Samples", async ({
    page,
    request,
  }) => {
    const labNumber = await findInProgressLabNumber(request);

    await page.goto(
      `/order/clinical/label?labNumber=${encodeURIComponent(labNumber)}`,
      { waitUntil: "domcontentloaded", timeout: LONG_TIMEOUT },
    );

    await expect(page).toHaveURL(/\/order\/clinical\/collect\?labNumber=/, {
      timeout: UI_TIMEOUT,
    });
    await expect(
      page.getByRole("heading", { level: 2, name: "Prepare Samples" }),
    ).toBeVisible({ timeout: UI_TIMEOUT });
  });
});
