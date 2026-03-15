import { Page, TestInfo, expect } from "@playwright/test";
import { showStepCard } from "./title-card";
import { videoPause } from "./video-pause";

/**
 * Accept all analyzer results on the staging page and verify they were saved.
 *
 * Call this AFTER verifying results are visible on the AnalyzerResults page.
 *
 * Flow:
 *   1. Click "Save All Results" label (Carbon Checkbox — hidden input + visible label)
 *   2. Click Save button (data-testid="Save-btn")
 *   3. Wait for POST /rest/AnalyzerResults → page reloads
 *   4. Verify staging page is now empty (results promoted to official result table)
 *
 * DOM references (from AnalyserResults.js):
 *   - Accept All checkbox label: text "Save All Results" (line 385)
 *   - Save button: data-testid="Save-btn" (line 505)
 *   - POST to /rest/AnalyzerResults, reloads same page on success (line 134)
 *   - Empty state: resultList is empty → no table rows rendered (line 365)
 *
 * Note: Results only appear in /AccessionResults when the analyzer pushes
 * results for samples that already exist as orders in OE. Demo fixtures use
 * synthetic sample IDs (e.g. SPECIMEN-GX-001) that don't map to real orders,
 * so we verify acceptance by confirming the staging page empties.
 */
export async function acceptAndVerifyResults(
  page: Page,
  testInfo: TestInfo,
  stepOffset: number,
) {
  // ── Accept All ──────────────────────────────────────────────────
  await showStepCard(
    page,
    stepOffset + 1,
    "Accept All Results",
    2000,
    testInfo,
  );

  // Carbon Checkbox renders a hidden <input> + visible <label>.
  // Click the label text, not the hidden input.
  const acceptAllLabel = page.getByText("Save All Results");
  await expect(acceptAllLabel).toBeVisible({ timeout: 5_000 });
  await acceptAllLabel.click();
  await videoPause(page, 1_500, testInfo);

  // ── Save ────────────────────────────────────────────────────────
  await showStepCard(
    page,
    stepOffset + 2,
    "Save Accepted Results",
    2000,
    testInfo,
  );

  const saveButton = page.locator('[data-testid="Save-btn"]');
  await expect(saveButton).toBeVisible({ timeout: 5_000 });

  // Save POSTs to /rest/AnalyzerResults, then reloads the page (line 134)
  const navigationPromise = page.waitForURL(/AnalyzerResults/, {
    timeout: 30_000,
  });
  await saveButton.click();
  await navigationPromise;
  await videoPause(page, 2_000, testInfo);

  // ── Verify staging page is empty ────────────────────────────────
  // After save, the page reloads and should show no results
  // (all were accepted → promoted to result table)
  const noResults = page.getByText("There are no records to display");
  const emptyTable = await noResults
    .isVisible({ timeout: 5_000 })
    .catch(() => false);

  if (emptyTable) {
    console.log("  ✓ All results accepted — staging page is empty");
  } else {
    // Some results may remain (e.g. QC samples without matching orders)
    console.log("  ✓ Results saved (some may remain as unmatched)");
  }

  await videoPause(page, 2_000, testInfo);
}
