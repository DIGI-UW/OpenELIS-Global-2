import { test, expect } from "../../../helpers/test-base";
import { UI_TIMEOUT, NAV_TIMEOUT } from "../../../helpers/timeouts";

/**
 * The first-generation EQA paths (OGC-613).
 *
 * Two of these screens survived the move to the V2 lanes and are still routed;
 * the rest were absorbed and now redirect to the V2 screen that replaced them,
 * so an operator following an old link or bookmark still lands somewhere that
 * answers the question they had. Either way the failure mode is silent — a
 * lazy import renamed, a guard tightened, a redirect dropped during a menu
 * change presents as a blank page rather than an error, and nothing else in
 * the suite would notice.
 *
 * So this is a deliberately shallow spec: one landmark per path, asserting the
 * page mounted rather than exercising anything. Depth belongs with the V2
 * lanes, which is where it has been spent.
 */

/**
 * Landmarks are matched inside the main content region, never across the whole
 * page: the side navigation already contains words like "Participants", so an
 * unscoped search would let a blank routed page pass on nav text — precisely
 * the regression these checks exist to catch.
 */
const MOUNTED: [string, string][] = [
  ["/qa/eqa/management", "Scheme Administration"],
  ["/qa/eqa/participants", "Participants"],
];

/**
 * Absorbed paths: the old path, the V2 path it now resolves to, and a landmark
 * proving the destination rendered rather than merely changing the URL.
 */
const REDIRECTED: [string, string, string][] = [
  ["/qa/eqa/results", "/qa/eqa/my-cycles", "My EQA Cycles"],
  [
    "/qa/eqa/distribution",
    "/qa/eqa/provider/schemes",
    "EQA schemes we provide",
  ],
  [
    "/qa/eqa/distribution/create",
    "/qa/eqa/provider/schemes",
    "EQA schemes we provide",
  ],
];

test.describe("EQA first-generation routes", () => {
  for (const [route, landmark] of MOUNTED) {
    test(`${route} mounts`, async ({ page }) => {
      test.setTimeout(120_000);
      await page.goto(route, { timeout: NAV_TIMEOUT });
      await expect(page).toHaveURL(new RegExp(route.replace(/\//g, "\\/")));
      await expect(
        page.getByRole("main").getByText(landmark, { exact: true }).first(),
      ).toBeVisible({ timeout: UI_TIMEOUT });
    });
  }

  for (const [route, destination, landmark] of REDIRECTED) {
    test(`${route} redirects to ${destination}`, async ({ page }) => {
      test.setTimeout(120_000);
      await page.goto(route, { timeout: NAV_TIMEOUT });
      await expect(page).toHaveURL(
        new RegExp(destination.replace(/\//g, "\\/")),
      );
      await expect(
        page.getByRole("main").getByText(landmark, { exact: true }).first(),
      ).toBeVisible({ timeout: UI_TIMEOUT });
    });
  }
});
