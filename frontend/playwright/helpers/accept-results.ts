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
 *   3. Navigate to AccessionResults for the staged accession
 *   4. Verify the accepted results appear in the OE results view
 *
 * DOM references (from AnalyserResults.js):
 *   - Accept All checkbox label: text "Save All Results" (line 385)
 *   - Save button: data-testid="Save-btn" (line 505)
 *   - Staged accession number: data-testid="LabNo"
 *   - POST to /rest/AnalyzerResults, reloads same page on success (line 134)
 *
 * Note: OE auto-creates Sample/SampleItem/Analysis/Result records on accept,
 * even when no pre-existing order exists. So AccessionResults will show results
 * for any accession number — pre-existing orders are NOT required.
 *
 * @param accessionNumber Optional explicit accession. If omitted, the helper
 *   captures the first staged accession from the current page before saving.
 */
export async function acceptAndVerifyResults(
  page: Page,
  presentation: DemoPresentation,
  stepOffset: number,
  accessionNumber?: string,
) {
  const stagedAccession =
    accessionNumber ??
    (await page.locator('[data-testid="LabNo"]').first().textContent())?.trim();

  if (!stagedAccession) {
    throw new Error("Could not determine staged accession number before save.");
  }

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

  // ── Verify in OE results view, not on the staging page ───────────
  await presentation.step(stepOffset + 3, "View Accepted Results", 2000);
  await openAccessionResultsAndWaitForText(page, stagedAccession);
  await expect(page.getByText(stagedAccession).first()).toBeVisible({
    timeout: 15_000,
  });
  await presentation.pause(3_000);
}
