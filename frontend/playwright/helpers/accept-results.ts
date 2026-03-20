import { Page, expect } from "@playwright/test";
import type { DemoPresentation } from "./demo-presentation";
import {
  locatorForAccessionNumber,
  openAccessionResultsAndWaitForText,
} from "./results-ui";

/**
 * Accept all analyzer results on the staging page, verify they were saved,
 * and optionally navigate to AccessionResults to confirm the accepted results.
 *
 * Call this AFTER verifying results are visible on the AnalyzerResults page.
 *
 * Flow:
 *   1. Check "Save All Results" via stable checkbox id
 *   2. Click Save button (data-testid="Save-btn")
 *   3. Navigate to AccessionResults for the staged accession
 *   4. Verify the accepted results appear in the OE results view
 *
 * DOM references (from AnalyserResults.js):
 *   - Accept All checkbox input: id="saveallresults"
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

  const acceptAllCheckbox = page.locator("#saveallresults");
  await expect(acceptAllCheckbox).toBeAttached({ timeout: 5_000 });
  await acceptAllCheckbox.check({ force: true });
  await presentation.pause(1_500);

  // ── Save ────────────────────────────────────────────────────────
  await presentation.step(stepOffset + 2, "Save Accepted Results", 2000);

  const saveButton = page.locator('[data-testid="Save-btn"]');
  const stagedRows = page.locator('[data-testid="LabNo"]');
  const stagedCountBeforeSave = await stagedRows.count();
  await expect(saveButton).toBeVisible({ timeout: 5_000 });
  await expect(saveButton).toBeEnabled({ timeout: 5_000 });
  await saveButton.click();

  const saveInProgress = page.locator(
    '[data-testid="analyzer-results-save-in-progress"]',
  );
  await Promise.any([
    expect(saveButton).toBeDisabled({ timeout: 5_000 }),
    saveInProgress.waitFor({ state: "attached", timeout: 5_000 }),
  ]).catch(() => {
    // Some runs complete quickly and can skip observable transition states.
  });

  if (stagedCountBeforeSave > 0) {
    await expect
      .poll(async () => stagedRows.count(), {
        timeout: 30_000,
      })
      .toBeLessThan(stagedCountBeforeSave);
  }

  await expect(saveInProgress).toBeHidden({ timeout: 30_000 });
  await expect(saveButton).toBeEnabled({ timeout: 20_000 });

  // ── Verify in OE results view, not on the staging page ───────────
  await presentation.step(stepOffset + 3, "View Accepted Results", 2000);
  await openAccessionResultsAndWaitForText(
    page,
    stagedAccession,
    stagedAccession,
    {
      timeoutMs: 60_000,
      perAttemptTimeoutMs: 15_000,
    },
  );
  await expect(locatorForAccessionNumber(page, stagedAccession)).toBeVisible({
    timeout: 15_000,
  });
  await presentation.pause(3_000);
}
