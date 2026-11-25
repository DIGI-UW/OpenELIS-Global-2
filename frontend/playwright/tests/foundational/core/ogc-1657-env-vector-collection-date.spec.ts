import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { API_PREFIX } from "../../../helpers/admin-ui";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1657 — the environmental and vector Collection Date, and the
 * environmental compliance dashboard's date filters, follow the site date
 * format like every other date (OGC-1616). They were fixed to YYYY-MM-DD, so a
 * day-first site showed and read ISO dates there only.
 */

async function siteDayFirst(page: Page): Promise<boolean> {
  const response = await page.request.get(
    `${API_PREFIX}/rest/configuration-properties`,
  );
  const properties = await response.json();
  expect(
    properties?.DEFAULT_DATE_LOCALE,
    "the site's date locale",
  ).toBeTruthy();
  return properties.DEFAULT_DATE_LOCALE === "fr-FR";
}

const shown = (dayFirst: boolean, day: number, month: number, year: number) => {
  const dd = String(day).padStart(2, "0");
  const mm = String(month).padStart(2, "0");
  return dayFirst ? `${dd}/${mm}/${year}` : `${mm}/${dd}/${year}`;
};

for (const lane of ["environmental", "vector"]) {
  test(`${lane} Enter Order: the Collection Date is shown, typed and picked in the site format`, async ({
    page,
  }) => {
    const dayFirst = await siteDayFirst(page);
    await page.goto(`/order/${lane}/enter`, { waitUntil: "domcontentloaded" });
    const field = page.locator("#vec-collection-date");
    await expect(field).toBeVisible({ timeout: NAV_TIMEOUT });

    await expect(field).toHaveAttribute(
      "placeholder",
      dayFirst ? "dd/mm/yyyy" : "mm/dd/yyyy",
    );

    await field.fill(shown(dayFirst, 5, 3, 2026));
    await field.press("Enter");
    await expect(field).toHaveValue(shown(dayFirst, 5, 3, 2026), {
      timeout: UI_TIMEOUT,
    });

    await page.getByRole("heading", { level: 1 }).click();
    await field.click();
    const calendar = page.locator(".flatpickr-calendar.open");
    await expect(calendar).toBeVisible({ timeout: UI_TIMEOUT });
    await calendar.getByLabel("March 10, 2026", { exact: true }).click();
    await expect(field).toHaveValue(shown(dayFirst, 10, 3, 2026), {
      timeout: UI_TIMEOUT,
    });

    const requiredBy = page.locator("#requiredBy");
    await expect(requiredBy).toHaveAttribute(
      "placeholder",
      dayFirst ? "dd/mm/yyyy" : "mm/dd/yyyy",
    );
    await requiredBy.fill(shown(dayFirst, 30, 12, 2099));
    await requiredBy.press("Enter");
    await expect(requiredBy).toHaveValue(shown(dayFirst, 30, 12, 2099));
  });
}

test("environmental Enter Order: each sample's Collected date follows the lot date in the site format", async ({
  page,
}) => {
  const dayFirst = await siteDayFirst(page);
  await page.goto("/order/environmental/enter", {
    waitUntil: "domcontentloaded",
  });
  const lot = page.locator("#vec-collection-date");
  await expect(lot).toBeVisible({ timeout: NAV_TIMEOUT });

  await lot.fill(shown(dayFirst, 5, 3, 2026));
  await lot.press("Enter");

  const collected = page.locator("#collected-0");
  await expect(collected).toHaveAttribute(
    "placeholder",
    dayFirst ? "dd/mm/yyyy" : "mm/dd/yyyy",
  );
  await expect(collected).toHaveValue(shown(dayFirst, 5, 3, 2026), {
    timeout: UI_TIMEOUT,
  });
  await expect(page.locator("#receivedAtLab-0")).toHaveAttribute(
    "placeholder",
    dayFirst ? "dd/mm/yyyy" : "mm/dd/yyyy",
  );
});

test("environmental compliance dashboard: a start date typed in the site format filters from that day", async ({
  page,
}) => {
  const dayFirst = await siteDayFirst(page);
  await page.goto("/EnvironmentalDashboard", { waitUntil: "domcontentloaded" });
  const start = page.getByRole("textbox", { name: "Date Range", exact: true });
  await expect(start).toBeVisible({ timeout: NAV_TIMEOUT });
  await expect(start).toHaveAttribute(
    "placeholder",
    dayFirst ? "dd/mm/yyyy" : "mm/dd/yyyy",
  );

  const filtered = page.waitForRequest(
    (request) =>
      request.url().includes("/rest/compliance/dashboard/summary") &&
      request.url().includes("startDate=2026-03-05&"),
    { timeout: UI_TIMEOUT },
  );
  await start.fill(shown(dayFirst, 5, 3, 2026));
  await start.press("Enter");
  await filtered;
  await expect(start).toHaveValue(shown(dayFirst, 5, 3, 2026));
});
