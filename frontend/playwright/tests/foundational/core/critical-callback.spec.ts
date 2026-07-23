import { test, expect, Page } from "../../../helpers/test-base";
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
 * Critical Callback Compliance (C.4 / OGC-714 + OGC-715) — the full loop:
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
  const result = await page.evaluate(
    async ({ prefix, on }) => {
      const csrf = localStorage.getItem("CSRF") || "";
      const res = await fetch(`${prefix}/rest/qi-config/indicator/CALLBACK`, {
        method: "PUT",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-Token": csrf,
        },
        body: JSON.stringify({
          indicatorKey: "CALLBACK",
          enabled: on,
          target: 100,
          action: 95,
          direction: "HIGHER_BETTER",
          overrides: [],
        }),
      });
      return res.status;
    },
    { prefix: API_PREFIX, on: enabled },
  );
  expect(result, "qi-config PUT should succeed for admin").toBe(204);
}

test.describe.serial("Critical Callback Compliance (OGC-714/715)", () => {
  let band: CriticalBandSeed;
  let accessionNumber: string;

  test.beforeAll(() => {
    band = seedCriticalBand(ORDERED_TEST_ID, 10, 90);
  });

  test.afterAll(async ({ browser }) => {
    band?.restore();
    // put the indicator back to its shipped opt-out default
    const ctx = await browser.newContext({
      storageState: "playwright/.auth/user.json",
    });
    const page = await ctx.newPage();
    await page.goto("/", { waitUntil: "domcontentloaded", timeout: 15_000 });
    await putCallbackConfig(page, false);
    await ctx.close();
  });

  test("critical result → callback log → tile + detail", async ({ page }) => {
    await test.step("Enable the CALLBACK indicator", async () => {
      await page.goto("/", { waitUntil: "domcontentloaded" });
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
      await page.goto("/result?type=order&doRange=false", {
        waitUntil: "domcontentloaded",
      });
      const main = page.getByRole("main");
      const searchInput = main.getByPlaceholder(/accession/i);
      await expect(searchInput).toBeVisible({ timeout: NAV_TIMEOUT });
      await searchInput.fill(accessionNumber);
      await main.getByRole("button", { name: /search/i }).click();

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
      const recipient = modal.locator("#callback-recipient-name");
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
      await page.goto("/result?type=order&doRange=false", {
        waitUntil: "domcontentloaded",
      });
      const main = page.getByRole("main");
      const searchInput = main.getByPlaceholder(/accession/i);
      await expect(searchInput).toBeVisible({ timeout: NAV_TIMEOUT });
      await searchInput.fill(accessionNumber);
      await main.getByRole("button", { name: /search/i }).click();

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
      await expect(tile).toContainText("Target: 100%");
      await expect(tile).toContainText(
        /of \d+ critical results acknowledged within target/,
      );
    });

    await test.step("Detail page lists the result with its status tag", async () => {
      await page.goto("/qa/qi/callback", { waitUntil: "domcontentloaded" });
      const row = page.locator("table tbody tr", { hasText: accessionNumber });
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
