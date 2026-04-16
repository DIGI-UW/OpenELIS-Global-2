import { test, expect } from "../../../helpers/test-base";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Track A / Site 4 — Result Entry → expandable result row
 *
 * The StorageLocationSelector at this site lives inside the expanded detail
 * of a result row at /result. Reaching that detail requires executing a
 * search with criteria that depend on environment-specific data — out of
 * scope for a regression smoke.
 *
 * What this spec verifies:
 *   - The /result route loads (catches accidental route removal)
 *   - The result-search shell renders (catches accidental unmount)
 *
 * What this spec deliberately does NOT verify:
 *   - The StorageLocationSelector behavior — covered in depth by
 *     storage-assign-dashboard.spec.ts. Same shared component across all
 *     four sites; Site 1 catches component-level regressions.
 *
 * Future work: a follow-up spec under demo/core/ can perform a search,
 * expand a row, and exercise the storage-quick-find flow once seed-data
 * infrastructure exists for creating a queryable result via REST.
 */
test.describe("Result Entry — search shell entry point", () => {
  test("/result loads with the search-type chooser visible", async ({
    page,
  }) => {
    await page.goto("/result", {
      waitUntil: "domcontentloaded",
      timeout: LONG_TIMEOUT,
    });

    await expect(page).toHaveURL(/\/result/, { timeout: LONG_TIMEOUT });

    // The Result Search page renders a "Search By" chooser as the primary
    // interaction. This is the stable landmark before any data is loaded.
    await expect(
      page.getByRole("combobox", { name: /search by/i }).first(),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
  });
});
