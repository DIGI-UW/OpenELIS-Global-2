import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1222: a Program row deleted straight from the database stayed in the
 * cached program list, so /rest/user-programs answered 500, the fetch helper
 * handed that error body to the page as data, and the next render called
 * find() on it and unmounted every order entry lane, leaving a blank page.
 *
 * The endpoint failure is injected rather than provoked by deleting a row, so
 * the case is reproduced exactly on any database and leaves nothing behind.
 */

const LANES = [
  { name: "clinical", path: "/order/clinical/enter" },
  { name: "environmental", path: "/order/environmental/enter" },
  { name: "vector", path: "/order/vector/enter" },
];

test.describe("Order entry survives a failing program list (OGC-1222)", () => {
  test.beforeEach(() => test.setTimeout(180_000));

  for (const lane of LANES) {
    test(`the ${lane.name} lane still renders when /rest/user-programs fails`, async ({
      page,
    }) => {
      await page.route("**/rest/user-programs", (route) =>
        route.fulfill({
          status: 500,
          contentType: "application/json",
          body: JSON.stringify({
            timestamp: Date.now(),
            status: 500,
            error: "Internal Server Error",
          }),
        }),
      );

      await page.goto(lane.path, { waitUntil: "domcontentloaded" });

      // The page used to come back blank: React unmounted the tree, leaving an
      // empty root. Any rendered content at all is the regression guard.
      const root = page.locator("#root");
      await expect(root).toBeVisible({ timeout: NAV_TIMEOUT });
      await expect(root).not.toBeEmpty({ timeout: NAV_TIMEOUT });
      await expect(page.locator("header, nav").first()).toBeVisible({
        timeout: NAV_TIMEOUT,
      });
    });
  }
});
