import { Page, expect } from "@playwright/test";
import type { DemoPresentation } from "./demo-presentation";
import {
  accessionTextRegExp,
  locatorForAccessionNumber,
  openAccessionResultsAndWaitForText,
} from "./results-ui";
import {
  SHORT_TIMEOUT,
  UI_TIMEOUT,
  LONG_TIMEOUT,
  NAV_TIMEOUT,
} from "./timeouts";

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
  await expect(acceptAllCheckbox).toBeAttached({ timeout: SHORT_TIMEOUT });
  // Carbon checkbox: click the visible label instead of forcing the hidden input
  await page.locator('label[for="saveallresults"]').click();
  await presentation.pause(1_500);

  // ── Save ────────────────────────────────────────────────────────
  await presentation.step(stepOffset + 2, "Save Accepted Results", 2000);

  const saveButton = page.locator('[data-testid="Save-btn"]');
  // Scope to this accession: other lanes/QC rows can share the AnalyzerResults page.
  const stagedRows = page
    .locator('[data-testid="LabNo"]')
    .filter({ hasText: accessionTextRegExp(stagedAccession.trim()) });
  const stagedCountBeforeSave = await stagedRows.count();
  await expect(saveButton).toBeVisible({ timeout: SHORT_TIMEOUT });
  await expect(saveButton).toBeEnabled({ timeout: SHORT_TIMEOUT });

  await saveButton.click();

  const saveInProgress = page.locator(
    '[data-testid="analyzer-results-save-in-progress"]',
  );
  await Promise.any([
    expect(saveButton).toBeDisabled({ timeout: SHORT_TIMEOUT }),
    saveInProgress.waitFor({ state: "attached", timeout: SHORT_TIMEOUT }),
  ]).catch(() => {
    // Some runs complete quickly and can skip observable transition states.
  });

  // Success path issues full page reload (AnalyserResults.js).
  await page.waitForURL(/AnalyzerResults[?]type=/, { timeout: NAV_TIMEOUT });

  if (stagedCountBeforeSave > 0) {
    await expect
      .poll(async () => stagedRows.count(), {
        timeout: LONG_TIMEOUT,
      })
      .toBe(0);
  }

  await expect(saveInProgress).toBeHidden({ timeout: LONG_TIMEOUT });
  await expect(saveButton).toBeEnabled({ timeout: LONG_TIMEOUT });

  // ── Verify in OE results view, not on the staging page ───────────
  await presentation.step(stepOffset + 3, "View Accepted Results", 2000);
  await openAccessionResultsAndWaitForText(
    page,
    stagedAccession,
    stagedAccession,
    {
      timeoutMs: NAV_TIMEOUT,
      perAttemptTimeoutMs: UI_TIMEOUT,
    },
  );
  await expect(locatorForAccessionNumber(page, stagedAccession)).toBeVisible({
    timeout: UI_TIMEOUT,
  });
  await presentation.pause(3_000);
}
