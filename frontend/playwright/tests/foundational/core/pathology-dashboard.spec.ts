import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Pathology Dashboard bench stages.
 *
 * The dashboard used to print the stored status constant straight onto the
 * screen, so the Stage column and the stage filter read MICROTOMY rather than
 * Microtomy, and the filter built its own "in progress" set (everything that
 * was not complete) which disagreed with the grouping the count tiles showed.
 * The filter, the Stage column and the tiles now read one ordered bench-stage
 * list, and every stage is shown in the user's language.
 *
 * What this proves on a deployed stack:
 *   - the four count tiles render, each with a whole-number count,
 *   - the stage filter offers exactly the eleven bench stages, localized and
 *     in bench order, behind the three fixed options: a disabled placeholder
 *     and two groupings, and
 *   - narrowing to one stage and widening back to All both hold.
 *
 * The per-deployment switches that decide which of the optional stages a
 * laboratory runs are configuration rows on an admin page, so they are proved
 * in pathology-stage-switches.spec.ts rather than here.
 *
 * No case data is seeded. Every assertion is about the filter, the tiles and
 * the table header, which render the same way on an empty dashboard as on a
 * busy one, so the spec passes against a fresh stack and a loaded one alike.
 */

/** The eleven bench stages, in the order the bench works them. */
const BENCH_STAGE_LABELS = [
  "Accessioned",
  "Grossing",
  "Decalcification",
  "Processing",
  "Embedding",
  "Microtomy",
  "Staining",
  "Coverslipping & QC",
  "Ready for Pathologist",
  "Under Pathologist Review",
  "Completed",
];

/**
 * The filter opens with three fixed options: a disabled placeholder and two
 * groupings. Every stage follows. Asserting the whole array at once proves
 * the count, the order and the localization in a single assertion.
 */
const STAGE_FILTER_OPTIONS = [
  "Status",
  "All",
  "In Progress",
  ...BENCH_STAGE_LABELS,
];

/** The Complete tile appends the week it counts, so it is matched loosely. */
const TILE_TITLES: Array<string | RegExp> = [
  "Cases in Progress",
  "Awaiting Pathology Review",
  "Additional Pathology Requests",
  /^Complete\(Week /,
];

test.describe("Pathology Dashboard bench stages", () => {
  test("dashboard loads with the four count tiles", async ({ page }) => {
    await page.goto("/PathologyDashboard", { waitUntil: "domcontentloaded" });

    await expect(
      page.getByRole("heading", { name: "Pathology", exact: true }),
    ).toBeVisible({ timeout: NAV_TIMEOUT });

    const tiles = page.locator(".dashboard-tile");
    await expect(tiles).toHaveCount(TILE_TITLES.length);

    for (const title of TILE_TITLES) {
      const tileTitle =
        typeof title === "string"
          ? page.getByRole("heading", { name: title, exact: true })
          : page.getByRole("heading", { name: title });
      const tile = tiles.filter({ has: tileTitle });
      await expect(tile).toHaveCount(1);

      // The count carries no role of its own, so it is read by the class the
      // tile gives it; the title above is what identifies which tile this is.
      await expect(tile.locator(".tile-value")).toHaveText(/^\d+$/, {
        timeout: UI_TIMEOUT,
      });
    }
  });

  test("stage filter lists the eleven bench stages, localized and in bench order", async ({
    page,
  }) => {
    await page.goto("/PathologyDashboard", { waitUntil: "domcontentloaded" });

    // The filter renders without a visible label, so the select is addressed
    // by its element type and id, which the dashboard sets explicitly.
    const stageFilter = page.locator("select#statusFilter");
    await expect(stageFilter).toBeVisible({ timeout: NAV_TIMEOUT });

    await expect(stageFilter.locator("option")).toHaveText(
      STAGE_FILTER_OPTIONS,
      { timeout: UI_TIMEOUT },
    );

    // Inversion: a stored constant reaching the screen is the defect this
    // replaced, and an exact match is case sensitive, so "Microtomy" does not
    // satisfy these.
    await expect(
      stageFilter.getByText("READY_PATHOLOGIST", { exact: true }),
    ).toHaveCount(0);
    await expect(
      stageFilter.getByText("MICROTOMY", { exact: true }),
    ).toHaveCount(0);

    // The dashboard opens on the in-progress grouping.
    await expect(stageFilter).toHaveValue("IN_PROGRESS");
  });

  test("selecting a single stage narrows the filter", async ({ page }) => {
    await page.goto("/PathologyDashboard", { waitUntil: "domcontentloaded" });

    const stageFilter = page.locator("select#statusFilter");
    // The opening selection is only settled once the stage list has arrived,
    // so wait for it before changing the selection.
    await expect(stageFilter).toHaveValue("IN_PROGRESS", {
      timeout: NAV_TIMEOUT,
    });

    await stageFilter.selectOption("MICROTOMY");
    await expect(stageFilter).toHaveValue("MICROTOMY");

    // The table keeps its Stage column whatever the filter holds. The header's
    // accessible name also carries the sort instruction the table adds for
    // screen readers, hence the partial match. Row counts are deliberately not
    // asserted: the dashboard may legitimately hold no cases.
    const stageColumn = page.getByRole("columnheader", { name: /Stage/ });
    await expect(stageColumn).toBeVisible({ timeout: UI_TIMEOUT });

    await stageFilter.selectOption("All");
    await expect(stageFilter).toHaveValue("All");
    await expect(stageColumn).toBeVisible();
  });
});
