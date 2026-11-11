import { Locator, Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { csrfToken, withAuthedPage } from "../../../helpers/api-session";
import {
  createSampleOrder,
  enterResults,
  validateResults,
} from "../../../helpers/seed-tat-data";
import {
  seedCriticalBand,
  CriticalBandSeed,
} from "../../../helpers/seed-callback-data";
import {
  NAV_TIMEOUT,
  UI_TIMEOUT,
  LONG_TIMEOUT,
} from "../../../helpers/timeouts";

/**
 * Critical Callback Compliance (OGC-714 + OGC-715) — the full loop:
 * psql-seed a critical band on the ordered test (ResultLimit has no REST
 * write path) → order → save a critical result → needs-callback banner +
 * Log-callback button in Results Entry → modal logs the call → banner
 * clears and STAYS cleared on reload (durable read side) → validate/release
 * → CALLBACK indicator enabled via /rest/qi-config → dashboard tile shows
 * compliance → detail page lists the result with its status tag.
 *
 * Run: cd frontend && npm run pw:test:core-foundational
 */

const API_PREFIX = "/api/OpenELIS-Global";
// createSampleOrder's captured payload orders exactly test id 13 (sampleXML).
const ORDERED_TEST_ID = 13;
const CRITICAL_VALUE = "95"; // at/beyond the seeded high bound (10–90 band)
const RECIPIENT = `E2E Dr. Callback ${Date.now().toString(36)}`;

/** Toggle the CALLBACK indicator via the OGC-709 manage endpoint. */
async function putCallbackConfig(page: Page, enabled: boolean): Promise<void> {
  const res = await page.request.put(
    `${API_PREFIX}/rest/qi-config/indicator/CALLBACK`,
    {
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": await csrfToken(page),
      },
      data: {
        indicatorKey: "CALLBACK",
        enabled,
        target: 100,
        action: 95,
        direction: "HIGHER_BETTER",
        overrides: [],
      },
    },
  );
  expect(res.status(), "qi-config PUT should succeed for admin").toBe(204);
}

/**
 * Opens Results Entry on one accession and expands its row: the callback
 * action lives in the expanded panel of the saved critical result.
 */
async function openResultsEntryRow(
  page: Page,
  accessionNumber: string,
): Promise<Locator> {
  await page.goto(
    `/Results?accessionNumber=${encodeURIComponent(accessionNumber)}`,
    { waitUntil: "domcontentloaded" },
  );
  const row = page
    .getByRole("main")
    .getByRole("row")
    .filter({ hasText: accessionNumber })
    .first();
  await expect(row).toBeVisible({ timeout: NAV_TIMEOUT });
  await row.getByRole("button", { name: /expand row/i }).click();
  return page.getByRole("main");
}

test.describe.serial("Critical Callback Compliance (OGC-714/715)", () => {
  let band: CriticalBandSeed;
  let accessionNumber: string;

  test.beforeAll(() => {
    band = seedCriticalBand(ORDERED_TEST_ID, 10, 90);
  });

  test.afterAll(async ({ browser }) => {
    band?.restore();
    await withAuthedPage(browser, async (page) => {
      // put the indicator back to its shipped opt-out default
      await putCallbackConfig(page, false);
    });
  });

  test("critical result → callback log → tile + detail", async ({ page }) => {
    test.setTimeout(180_000);
    await test.step("Enable the CALLBACK indicator", async () => {
      await putCallbackConfig(page, true);
    });

    await test.step("Create an order and save a critical result", async () => {
      accessionNumber = await createSampleOrder(page, {
        labNo: "",
        receivedDate: "",
        receivedTime: "",
      });
      expect(accessionNumber, "sample order must be created").toBeTruthy();
      await enterResults(page, accessionNumber, CRITICAL_VALUE);
    });

    await test.step("Results Entry offers to log the callback for the saved critical", async () => {
      const main = await openResultsEntryRow(page, accessionNumber);

      await expect(main.getByTestId("unified-log-callback-button")).toBeVisible(
        { timeout: UI_TIMEOUT },
      );
      await expect(main.getByTestId("unified-callback-logged")).toHaveCount(0);
    });

    await test.step("Log the callback via the modal", async () => {
      await page.getByTestId("unified-log-callback-button").click();
      const modal = page.getByTestId("callback-modal");
      await expect(modal).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(modal).toContainText(accessionNumber);

      // toHaveValue guards the regression this spec once caught: the banner's
      // default alertdialog role stole focus on every render, emptying this
      // controlled input (fixed by role="status" on the banner).
      const recipient = modal.getByTestId("callback-recipient-name");
      await recipient.fill(RECIPIENT);
      await expect(recipient).toHaveValue(RECIPIENT);
      // status Select defaults to CONFIRMED — keep it
      await modal.getByRole("button", { name: /save/i }).click();
      await expect(modal).toBeHidden({ timeout: LONG_TIMEOUT });

      // the row now says the call was logged
      await expect(page.getByTestId("unified-callback-logged")).toBeVisible({
        timeout: UI_TIMEOUT,
      });
    });

    await test.step("The logged call survives a reload (durable read side)", async () => {
      const main = await openResultsEntryRow(page, accessionNumber);

      // the durable logged-results check knows this critical was already called
      await expect(main.getByTestId("unified-callback-logged")).toBeVisible({
        timeout: NAV_TIMEOUT,
      });
    });

    await test.step("Validate/release the result", async () => {
      await validateResults(page, accessionNumber);
    });

    await test.step("Dashboard shows the callback tile with compliance", async () => {
      await page.goto("/qa/qi/dashboard", { waitUntil: "domcontentloaded" });
      const tile = page.getByTestId("qi-tile-callback");
      await expect(tile).toBeVisible({ timeout: NAV_TIMEOUT });
      // released just now with a pre-release CONFIRMED call → counted compliant;
      // other window data may exist, so assert a computed percentage, not 100%
      await expect(tile.locator(".qi-tile__value")).toHaveText(/\d+\.\d{2}%/, {
        timeout: UI_TIMEOUT,
      });
      await expect(tile).toContainText("Target ≥ 100%");
      await expect(tile).toContainText(
        /of \d+ critical results acknowledged within target/,
      );
    });

    await test.step("Detail page lists the result with its status tag", async () => {
      await page.goto("/qa/qi/callback", { waitUntil: "domcontentloaded" });
      const row = page.getByRole("row", { name: accessionNumber });
      await expect(row).toBeVisible({ timeout: NAV_TIMEOUT });
      await expect(row).toContainText("Confirmed with read-back");
      await expect(row).toContainText(RECIPIENT);

      // design-aligned aggregate: the pre-release CONFIRMED call lands in
      // the histogram's 0–5 bucket
      await expect(
        page.getByText("Time-to-acknowledge distribution"),
      ).toBeVisible();
      const histogram = page.getByTestId("callback-distribution");
      await expect(
        histogram.locator(".qi-barlist__row", { hasText: "0–5 min" }),
      ).toBeVisible();
    });
  });
});
