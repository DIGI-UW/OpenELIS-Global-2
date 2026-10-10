import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1442: when the server could not be reached while a page loaded (a
 * Tomcat restart, a dropped site connection), the app tried the session
 * check ten times in ten seconds, then put up a blocking "System Error /
 * Failed to fetch" modal and never tried again, even once the server was
 * back. The app now keeps trying with growing gaps behind a non-blocking
 * notice and renders the page on the same URL as soon as the server answers.
 *
 * The outage is injected by failing requests in the browser, so the server
 * session survives and the case runs the same on any stack. Attempts come
 * after 1, 2, 4, 8 and 15 s, so the sixth is about 30 s into the outage.
 */

const SESSION = "**/api/OpenELIS-Global/session";
const ALL_API = "**/api/OpenELIS-Global/**";
const ORDER_ENTRY = "/order/clinical/enter";

test.describe("Server unreachable while a page loads (OGC-1442)", () => {
  test.beforeEach(() => test.setTimeout(180_000));

  test("an outage of about 30 s shows a non-blocking notice, never System Error, and the page renders by itself", async ({
    page,
  }) => {
    let attempts = 0;
    const down = (route) => {
      attempts += 1;
      return route.abort("connectionrefused");
    };
    await page.route(SESSION, down);

    await page.goto(ORDER_ENTRY, { waitUntil: "domcontentloaded" });

    const notice = page.getByTestId("server-reconnect");
    await expect(notice).toBeVisible({ timeout: NAV_TIMEOUT });
    await expect(notice).toContainText("Can't reach the server");
    await expect(page.getByRole("button", { name: "Try now" })).toBeVisible();

    await expect
      .poll(() => attempts, { timeout: 60_000, intervals: [1_000] })
      .toBeGreaterThanOrEqual(6);
    await expect(page.getByText("System Error")).toHaveCount(0);
    await expect(notice).toBeVisible();

    await page.unroute(SESSION, down);

    await expect(notice).toBeHidden({ timeout: 45_000 });
    await expect(page).toHaveURL(new RegExp(`${ORDER_ENTRY}$`));
    await expect(
      page.getByRole("button", { name: "Generate Lab Number" }),
    ).toBeVisible({ timeout: NAV_TIMEOUT });
  });

  test("Try now reconnects at once after the whole API dropped, and order entry gets its lists", async ({
    page,
  }) => {
    const down = (route) => route.abort("connectionrefused");
    await page.route(ALL_API, down);

    await page.goto(ORDER_ENTRY, { waitUntil: "domcontentloaded" });
    const notice = page.getByTestId("server-reconnect");
    await expect(notice).toBeVisible({ timeout: NAV_TIMEOUT });
    await expect(page.getByTestId("server-reconnect-countdown")).toContainText(
      /Trying again in \d+ s/,
    );

    await page.unroute(ALL_API, down);
    await page.getByRole("button", { name: "Try now" }).click();

    await expect(notice).toBeHidden({ timeout: NAV_TIMEOUT });
    await expect(
      page.getByRole("button", { name: "Generate Lab Number" }),
    ).toBeVisible({ timeout: NAV_TIMEOUT });
    await expect
      .poll(() => page.locator("select#sampleType-0 option").count(), {
        timeout: NAV_TIMEOUT,
      })
      .toBeGreaterThan(1);
  });

  test("a 401 from the session check is an answer: straight to login, no notice", async ({
    page,
  }) => {
    await page.route(SESSION, (route) =>
      route.fulfill({ status: 401, body: "" }),
    );

    await page.goto(ORDER_ENTRY, { waitUntil: "domcontentloaded" });

    await expect(page).toHaveURL(/\/login/, { timeout: NAV_TIMEOUT });
    await expect(page.getByTestId("server-reconnect")).toHaveCount(0);
  });
});
