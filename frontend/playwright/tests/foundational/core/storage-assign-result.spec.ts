import { test, expect } from "../../../helpers/test-base";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Track A / Site 4 — Result Entry → expandable result row
 *
 * The StorageLocationSelector at this site lives inside the expanded detail
 * of a result row. Reaching that detail requires loading a worklist with
 * criteria that depend on environment-specific data — out of scope for a
 * regression smoke.
 *
 * What this spec verifies:
 *   - The legacy /result route still resolves: the unified worklist is the
 *     default (resultsEntryUnifiedRoute on), so /result forwards to /Results
 *     (catches accidental route removal)
 *   - The worklist shell renders (catches accidental unmount)
 *
 * What this spec deliberately does NOT verify:
 *   - The StorageLocationSelector behavior — covered in depth by
 *     storage-assign-dashboard.spec.ts. Same shared component across all
 *     four sites; Site 1 catches component-level regressions.
 */
test.describe("Result Entry — worklist entry point", () => {
  test("/result forwards to the unified worklist with its toolbar visible", async ({
    page,
  }) => {
    await page.goto("/result", {
      waitUntil: "domcontentloaded",
      timeout: LONG_TIMEOUT,
    });

    await expect(page).toHaveURL(/\/Results/, { timeout: LONG_TIMEOUT });

    const main = page.getByRole("main");
    await expect(
      main.getByRole("heading", { name: /result/i }).first(),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(main.getByLabel(/lab unit/i)).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
    await expect(
      main.getByRole("button", { name: /^load results$/i }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
  });
});
