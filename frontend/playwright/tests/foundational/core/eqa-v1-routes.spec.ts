import { test, expect } from "../../../helpers/test-base";
import { UI_TIMEOUT, NAV_TIMEOUT } from "../../../helpers/timeouts";

/**
 * The first-generation EQA screens still on the menu (OGC-613).
 *
 * These predate the V2 provider and participant lanes and are not where new
 * work happens, but they are still routed, still reachable from the menu, and
 * still the only place some V1 data can be seen. A route that quietly stops
 * resolving — a lazy import renamed, a guard tightened, a path typo during a
 * menu change — presents as a blank page rather than an error, and nothing
 * else in the suite would notice.
 *
 * So this is a deliberately shallow spec: one landmark per route, asserting
 * the page mounted rather than exercising anything. Depth belongs with the V2
 * lanes, which is where it has been spent.
 */

const ROUTES: [string, string][] = [
  ["/qa/eqa/management", "Program Administration"],
  ["/qa/eqa/results", "EQA Results & Analysis"],
  ["/qa/eqa/participants", "Participants"],
  ["/qa/eqa/distribution", "EQA Distribution"],
  // The create wizard has no heading of its own until a later step, so its
  // first step label is the landmark.
  ["/qa/eqa/distribution/create", "Program & Details"],
];

test.describe("EQA first-generation routes", () => {
  for (const [route, landmark] of ROUTES) {
    test(`${route} mounts`, async ({ page }) => {
      test.setTimeout(120_000);
      await page.goto(route, { timeout: NAV_TIMEOUT });
      await expect(page).toHaveURL(new RegExp(route.replace(/\//g, "\\/")));
      await expect(page.getByText(landmark).first()).toBeVisible({
        timeout: UI_TIMEOUT,
      });
    });
  }
});
