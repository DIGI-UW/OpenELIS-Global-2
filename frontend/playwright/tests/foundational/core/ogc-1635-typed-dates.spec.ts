import { test, expect, Page } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1635 — a date typed into a date filter is the date the page uses. Typing
 * a date and clicking Search (or anywhere else) used to leave the date in the
 * field while the search went out without it. A text that is not a date, or a
 * date the field does not allow, stays in the field with a message, and is not
 * used.
 */

const dayFirst = async (page: Page, inputId: string) =>
  (await page.locator(`#${inputId}`).getAttribute("placeholder"))?.startsWith(
    "dd",
  ) ?? false;

const asTyped = (day: number, month: number, year: number, df: boolean) => {
  const dd = String(day).padStart(2, "0");
  const mm = String(month).padStart(2, "0");
  return df ? `${dd}/${mm}/${year}` : `${mm}/${dd}/${year}`;
};

const typeInto = async (page: Page, inputId: string, text: string) => {
  const input = page.locator(`#${inputId}`);
  await input.click();
  await input.press("ControlOrMeta+a");
  await page.keyboard.type(text);
};

test.describe("OGC-1635 typed dates", () => {
  test("the Compliance Report searches with dates typed into both fields", async ({
    page,
  }) => {
    await page.goto("/LaporanHasil", { waitUntil: "domcontentloaded" });
    await expect(page.locator("#filter-date-from")).toBeVisible({
      timeout: NAV_TIMEOUT,
    });
    const df = await dayFirst(page, "filter-date-from");
    await typeInto(page, "filter-date-from", asTyped(11, 11, 2026, df));
    await typeInto(page, "filter-date-to", asTyped(12, 11, 2026, df));

    const search = page.waitForRequest((request) =>
      request.url().includes("/rest/complianceReport?"),
    );
    await page
      .getByTestId("content-wrapper")
      .getByRole("button", { name: "Search", exact: true })
      .click();
    const url = (await search).url();

    expect(url).toContain("dateFrom=2026-11-11");
    expect(url).toContain("dateTo=2026-11-12");
    await expect(page.locator("#filter-date-from")).toHaveValue(
      asTyped(11, 11, 2026, df),
    );
  });

  test("a typed text that is not a date is marked and left out of the search", async ({
    page,
  }) => {
    await page.goto("/LaporanHasil", { waitUntil: "domcontentloaded" });
    await expect(page.locator("#filter-date-from")).toBeVisible({
      timeout: NAV_TIMEOUT,
    });
    await typeInto(page, "filter-date-to", "45/45/2026");

    const search = page.waitForRequest((request) =>
      request.url().includes("/rest/complianceReport?"),
    );
    await page
      .getByTestId("content-wrapper")
      .getByRole("button", { name: "Search", exact: true })
      .click();
    const url = (await search).url();

    expect(url).not.toContain("dateTo=");
    await expect(page.locator("#filter-date-to")).toHaveValue("45/45/2026");
    await expect(page.getByText(/Enter a valid date as/)).toBeVisible({
      timeout: UI_TIMEOUT,
    });
  });

  test("the order dashboard filters by dates typed into From and To", async ({
    page,
  }) => {
    await page.goto("/order/clinical", { waitUntil: "domcontentloaded" });
    await expect(page.locator("#date-start")).toBeVisible({
      timeout: NAV_TIMEOUT,
    });
    const df = await dayFirst(page, "date-start");
    await typeInto(page, "date-start", asTyped(1, 11, 2026, df));
    const dashboard = page.waitForRequest(
      (request) =>
        request.url().includes("/rest/order/dashboard") &&
        request.url().includes("endDate=2026-11-15"),
    );
    await typeInto(page, "date-end", asTyped(15, 11, 2026, df));
    await page.getByRole("heading").first().click();

    expect((await dashboard).url()).toContain("startDate=2026-11-01");
  });

  test("the Report NCE form refuses a typed future date of event and says why", async ({
    page,
  }) => {
    await page.goto("/ReportNonConformingEvent", {
      waitUntil: "domcontentloaded",
    });
    await expect(page.locator("#date-of-event")).toBeVisible({
      timeout: NAV_TIMEOUT,
    });
    const df = await dayFirst(page, "date-of-event");
    await typeInto(page, "date-of-event", asTyped(31, 12, 2999, df));
    await page
      .getByRole("heading", { name: "Report Non-Conformity Event" })
      .click();

    await expect(
      page.getByText("The date cannot be in the future."),
    ).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(page.locator("#date-of-event")).toHaveValue(
      asTyped(31, 12, 2999, df),
    );
  });
});
