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
import { isSettingOn, setSetting } from "../../../fixtures/esig-admin";
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

// The needs-callback banner, the Log-callback button and the modal live in the
// legacy SearchResultForm; the unified worklist has no callback capture yet, so
// this loop is only reachable with the unified route off. The route ships on by
// default, so this spec turns it off for its own run and puts it back, the way
// ogc-1121-critical-result-flag reaches the same screen. Drop this once the
// banner and modal are ported to the unified page.
const UNIFIED_ROUTE_SETTING = "resultsEntryUnifiedRoute";

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

/** Search legacy Results Entry for one accession; answers with its main region. */
async function searchResultsEntry(
  page: Page,
  accessionNumber: string,
): Promise<Locator> {
  await page.goto("/result?type=order&doRange=false", {
    waitUntil: "domcontentloaded",
  });
  const main = page.getByRole("main");
  const searchInput = main.getByPlaceholder(/accession/i);
  await expect(searchInput).toBeVisible({ timeout: NAV_TIMEOUT });
  await searchInput.fill(accessionNumber);
  await main.getByRole("button", { name: /search/i }).click();
  return main;
}

test.describe.serial("Critical Callback Compliance (OGC-714/715)", () => {
  let band: CriticalBandSeed;
  let accessionNumber: string;
  let unifiedWasOn = false;

  test.beforeAll(async ({ browser }) => {
    band = seedCriticalBand(ORDERED_TEST_ID, 10, 90);
    await withAuthedPage(browser, async (page) => {
      unifiedWasOn = await isSettingOn(page, UNIFIED_ROUTE_SETTING);
      if (unifiedWasOn) {
        await setSetting(page, UNIFIED_ROUTE_SETTING, false);
      }
    });
  });

  test.afterAll(async ({ browser }) => {
    band?.restore();
    await withAuthedPage(browser, async (page) => {
      // put the indicator back to its shipped opt-out default
      await putCallbackConfig(page, false);
      if (unifiedWasOn) {
        await setSetting(page, UNIFIED_ROUTE_SETTING, true);
      }
    });
  });

  test("critical result → callback log → tile + detail", async ({ page }) => {
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

    await test.step("Results Entry flags the saved critical", async () => {
      await searchResultsEntry(page, accessionNumber);

      await expect(page.getByTestId("callback-banner")).toBeVisible({
        timeout: NAV_TIMEOUT,
      });
      await expect(page.getByTestId("log-callback-button")).toBeVisible({
        timeout: UI_TIMEOUT,
      });
    });

    await test.step("Log the callback via the modal", async () => {
      await page.getByTestId("log-callback-button").click();
      const modal = page.getByRole("dialog");
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

      // banner recomputes from the logged map — the page's criticals are covered
      await expect(page.getByTestId("callback-banner")).toBeHidden({
        timeout: UI_TIMEOUT,
      });
    });

    await test.step("Banner stays cleared on reload (durable read side)", async () => {
      const main = await searchResultsEntry(page, accessionNumber);

      // the row renders (Save button present) but no banner: the durable
      // logged-results check knows this critical was already called
      await expect(main.getByRole("button", { name: "Save" })).toBeVisible({
        timeout: NAV_TIMEOUT,
      });
      await expect(page.getByTestId("callback-banner")).toBeHidden();
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
