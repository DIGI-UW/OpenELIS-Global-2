import { Page, expect } from "@playwright/test";
import type { DemoPresentation } from "./demo-presentation";
import { openAccessionResultsAndWaitForText } from "./results-ui";

/**
 * Accept all analyzer results on the staging page, verify they were saved,
 * and optionally navigate to AccessionResults to confirm the accepted results.
 *
 * Call this AFTER verifying results are visible on the AnalyzerResults page.
 *
 * Flow:
 *   1. Click "Save All Results" label (Carbon Checkbox — hidden input + visible label)
 *   2. Click Save button (data-testid="Save-btn")
 *   3. Wait for empty staging UI signal (data-testid="analyzer-results-empty")
 *   4. Verify staging page is now empty (results promoted to official result table)
 *   5. (Optional) Navigate to AccessionResults to verify accepted results appear
 *
 * DOM references (from AnalyserResults.js):
 *   - Accept All checkbox label: text "Save All Results" (line 385)
 *   - Save button: data-testid="Save-btn" (line 505)
 *   - POST to /rest/AnalyzerResults, reloads same page on success (line 134)
 *   - Empty state: data-testid="analyzer-results-empty"
 *
 * Note: OE auto-creates Sample/SampleItem/Analysis/Result records on accept,
 * even when no pre-existing order exists. So AccessionResults will show results
 * for any accession number — pre-existing orders are NOT required.
 *
 * @param accessionNumber If provided, navigates to AccessionResults after
 *   acceptance to verify the results appear with proper test names.
 */
export async function acceptAndVerifyResults(
  page: Page,
  presentation: DemoPresentation,
  stepOffset: number,
  accessionNumber?: string,
) {
  // ── Accept All ──────────────────────────────────────────────────
  await presentation.step(stepOffset + 1, "Accept All Results", 2000);

  // Carbon Checkbox renders a hidden <input> + visible <label>.
  // Click the label text, not the hidden input.
  const acceptAllLabel = page.getByText("Save All Results");
  await expect(acceptAllLabel).toBeVisible({ timeout: 5_000 });
  await acceptAllLabel.click();
  await presentation.pause(1_500);

  // ── Save ────────────────────────────────────────────────────────
  await presentation.step(stepOffset + 2, "Save Accepted Results", 2000);

  const saveButton = page.locator('[data-testid="Save-btn"]');
  await expect(saveButton).toBeVisible({ timeout: 5_000 });
  await expect(saveButton).toBeEnabled({ timeout: 5_000 });
  await saveButton.click();

  // ── Verify post-save state ───────────────────────────────────────
  const emptyState = page.locator('[data-testid="analyzer-results-empty"]');
  await expect(emptyState).toBeVisible({ timeout: 15_000 });
  await expect(page.locator('[data-testid="LabNo"]')).toHaveCount(0, {
    timeout: 15_000,
  });
  await presentation.pause(2_000);

  // ── Navigate to AccessionResults (if accession number provided) ──
  if (accessionNumber) {
    await presentation.step(stepOffset + 3, "View Accepted Results", 2000);
    await openAccessionResultsAndWaitForText(page, accessionNumber);
    await expect(page.getByText(accessionNumber).first()).toBeVisible({
      timeout: 15_000,
    });
    await presentation.pause(3_000);
  }
}
