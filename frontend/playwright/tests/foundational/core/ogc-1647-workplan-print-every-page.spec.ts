import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1647 — a workplan the server splits into pages printed only the page on
 * screen and "Total Tests" counted that page. Print now takes every page, with
 * the tests removed on any page left out, and the total is the whole list.
 * The two pages are served by route so the spec does not need 100 orders.
 */

const rows = (count: number, first: number) =>
  Array.from({ length: count }, (_, i) => ({
    analysisId: String(first + i),
    accessionNumber: `DEV01009900000000${first + i}`,
    testName: "Mock test",
    receivedDate: "01/09/2026",
  }));

test("Print takes every page of the workplan and Total Tests counts it", async ({
  page,
}) => {
  await page.route(/\/rest\/WorkPlanByTest\?/, (route) => {
    const second =
      new URL(route.request().url()).searchParams.get("page") === "2";
    return route.fulfill({
      json: {
        workplanTests: second ? rows(2, 13) : rows(3, 10),
        paging: {
          currentPage: second ? "2" : "1",
          totalPages: "2",
          totalResults: "5",
        },
      },
    });
  });
  let printed: { workplanTests: Record<string, unknown>[] } | undefined;
  await page.route("**/rest/PrintWorkplanReport", (route) => {
    printed = JSON.parse(route.request().postData() || "{}");
    return route.fulfill({
      status: 200,
      contentType: "application/pdf",
      body: "%PDF-1.4\n%%EOF\n",
    });
  });

  await page.goto("/WorkPlanByTest", { waitUntil: "domcontentloaded" });
  const testType = page.locator("#workplan-test");
  await expect(testType).toBeVisible({ timeout: NAV_TIMEOUT });
  await testType.click();
  await page.getByRole("listbox").getByRole("option").first().click();

  await expect(page.getByText(/Total Tests\s*=\s*5/)).toBeVisible({
    timeout: UI_TIMEOUT,
  });
  await page.locator("#includedCheck_1").check();

  await page.locator("#print-top").click();
  await expect
    .poll(() => printed?.workplanTests?.length, { timeout: UI_TIMEOUT })
    .toBe(5);
  expect(
    printed!.workplanTests.map((row) => [
      row.accessionNumber,
      row.notIncludedInWorkplan,
    ]),
  ).toEqual([
    ["DEV0100990000000010", false],
    ["DEV0100990000000011", true],
    ["DEV0100990000000012", false],
    ["DEV0100990000000013", false],
    ["DEV0100990000000014", false],
  ]);
});
