import { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { csrfToken } from "../../../helpers/api-session";
import {
  AMYLASE_TEST,
  API,
  RBC_TEST,
  SERUM,
  WHOLE_BLOOD,
  orderTests,
} from "../../../helpers/results-nce-ui";
import { LONG_TIMEOUT, NAV_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1588 — Pending Tests held only the 500 oldest tests and searched inside
 * them, a batch accepted tests from two lab units, and refusals spoke in
 * database ids and parser errors. The list is paged and searched by the
 * server, a batch holds one lab unit's tests, and refusals name what the user
 * sees.
 */

const PENDING = `${API}/rest/batch-workplans/pending-tests/page`;

async function pendingAnalysisId(page: Page, accession: string) {
  const response = await page.request.get(
    `${PENDING}?page=1&pageSize=20&search=${encodeURIComponent(accession)}`,
  );
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.total, `pending rows for ${accession}`).toBe(1);
  return body.items[0].analysisId as string;
}

async function selectByLabNumber(page: Page, accession: string) {
  const main = page.getByRole("main");
  const searched = page.waitForResponse(
    (response) =>
      response.url().includes("/pending-tests/page") &&
      response.url().includes(`search=${accession}`),
  );
  await main.getByLabel("Search", { exact: true }).fill(accession);
  await searched;
  const checkbox = main.getByLabel(`Select ${accession}`);
  await expect(checkbox).toBeAttached({ timeout: LONG_TIMEOUT });
  await main
    .locator(`label[for="${await checkbox.getAttribute("id")}"]`)
    .click();
}

test.describe("OGC-1588 batch workplan pending tests and lab units", () => {
  test("the server finds any pending test, and a batch keeps to one lab unit", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const chemistry = await orderTests(page, SERUM, [AMYLASE_TEST]);
    const hematology = await orderTests(page, WHOLE_BLOOD, [RBC_TEST]);

    await test.step("API: one page, its total and every filter's values", async () => {
      const response = await page.request.get(`${PENDING}?page=1&pageSize=1`);
      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.items).toHaveLength(1);
      expect(body.total).toBeGreaterThanOrEqual(2);
      expect(Array.isArray(body.units)).toBe(true);
      expect(
        (await page.request.get(`${PENDING}?page=1&pageSize=1000`)).status(),
        "a page size past the limit is refused",
      ).toBe(400);
    });

    const chemistryId = await pendingAnalysisId(page, chemistry);
    const hematologyId = await pendingAnalysisId(page, hematology);
    const headers = { "X-CSRF-Token": await csrfToken(page) };

    await test.step("API: refusals name what the user sees", async () => {
      const mixed = await page.request.post(
        `${API}/rest/batch-workplans/batches`,
        { headers, data: { analysisIds: [chemistryId, hematologyId] } },
      );
      expect(mixed.status()).toBe(400);
      expect((await mixed.json()).message).toContain(
        "A batch holds the tests of one lab unit",
      );
      const notANumber = await page.request.post(
        `${API}/rest/batch-workplans/batches`,
        { headers, data: { analysisIds: ["abc"] } },
      );
      expect(notANumber.status()).toBe(400);
      expect((await notANumber.json()).message).toBe(
        "One or more analyses were not found",
      );
    });

    await test.step("UI: search reaches each order, and a two-unit batch is refused", async () => {
      await page.goto("/Workplan", { waitUntil: "domcontentloaded" });
      await expect(
        page.getByRole("main").getByRole("heading", { name: "Batch Workplan" }),
      ).toBeVisible({ timeout: NAV_TIMEOUT });
      await selectByLabNumber(page, chemistry);
      await selectByLabNumber(page, hematology);
      const created = page.waitForResponse(
        (response) =>
          response.url().endsWith("/rest/batch-workplans/batches") &&
          response.request().method() === "POST",
      );
      await page
        .getByRole("main")
        .getByRole("button", { name: "Create batch (2)" })
        .click();
      expect((await created).status()).toBe(400);
      await expect(
        page.getByText(
          /A batch holds the tests of one lab unit; these come from/,
        ),
      ).toBeVisible();
    });
  });
});
