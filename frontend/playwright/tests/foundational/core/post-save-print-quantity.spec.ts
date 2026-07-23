import { test, expect } from "../../../helpers/test-base";
import type { APIRequestContext } from "@playwright/test";

/**
 * LabelMakerServlet quantity-cap contract.
 *
 * Locks in the per-request quantity ceiling
 * (numMaxRequestLabelQuantity, default DEFAULT_MAX_REQUEST_QUANTITY=100)
 * at the wire level. Without this cap, an authenticated caller can submit
 * ?override=true&quantity=999999999 and BarcodeLabelMaker's PDF render loop
 * will hang the JVM.
 *
 * Validation logic itself is unit-tested in LabelMakerServletTest;
 * this spec exercises the same behaviour through the actual servlet
 * dispatch (auth filter, doGet, validate, error response) so a future
 * refactor can't silently relax the cap end-to-end.
 *
 * Seed lookup: pulls a real accession from
 * /rest/home-dashboard/ORDERS_IN_PROGRESS so the request reaches the
 * quantity check rather than failing earlier on accession-format
 * validation. Pattern mirrors storage-assign-order-label.spec.ts.
 */

async function findInProgressLabNumber(
  request: APIRequestContext,
): Promise<string> {
  const response = await request.get(
    "/api/OpenELIS-Global/rest/home-dashboard/ORDERS_IN_PROGRESS",
  );
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

test.describe("LabelMakerServlet quantity bounds", () => {
  test("rejects quantity above the per-request cap (HTTP 400)", async ({
    page,
    request,
  }) => {
    const labNumber = await findInProgressLabNumber(request);

    // 100000 is well above the 100 default and any reasonable admin override.
    // override=true is included to confirm the cap holds even when the
    // per-label safety override is in effect (the cap's only job).
    const response = await page.request.get(
      `/api/OpenELIS-Global/LabelMakerServlet?labNo=${encodeURIComponent(labNumber)}` +
        `&type=order&quantity=100000&override=true`,
    );

    expect(response.status()).toBe(400);
  });
});
