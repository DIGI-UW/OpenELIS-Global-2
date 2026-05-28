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
  await presentation.step(stepOffset + 1, "Accept All Results");

  const stagedRows = () =>
    page
      .getByRole("row")
      .filter({ hasText: accessionTextRegExp(stagedAccession.trim()) });

  let stagedCountBeforeSave = await stagedRows().count();
  if (stagedCountBeforeSave === 0) {
    const labNumberInput = page.getByRole("textbox", {
      name: /enter lab number/i,
    });
    await expect(labNumberInput).toBeVisible({ timeout: SHORT_TIMEOUT });
    await labNumberInput.fill(stagedAccession);
    await page.getByRole("button", { name: /search/i }).click();
    await expect(stagedRows().first()).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
    stagedCountBeforeSave = await stagedRows().count();
  }

  for (let i = 0; i < stagedCountBeforeSave; i++) {
    const acceptInput = stagedRows()
      .nth(i)
      .locator('input[id$=".isAccepted"]')
      .first();
    await expect(acceptInput).toBeAttached({ timeout: SHORT_TIMEOUT });
    if (!(await acceptInput.isChecked())) {
      const checkboxId = await acceptInput.getAttribute("id");
      if (!checkboxId) {
        throw new Error("Could not determine row acceptance checkbox id.");
      }
      await page.locator(`label[for="${checkboxId}"]`).click();
    }
  }
  await presentation.pause(1_500);

  // ── Save ────────────────────────────────────────────────────────
  await presentation.step(stepOffset + 2, "Save Accepted Results");

  const saveButton = page.locator('[data-testid="Save-btn"]');
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
  // After save, either more results remain (Save visible) or all were consumed
  // (empty state). Use locator.or() — NOT Promise.race, which leaves the losing
  // assertion retrying in the background.
  await page.waitForURL(/AnalyzerResults[?](id|type)=/, {
    timeout: NAV_TIMEOUT,
  });
  // Wait for the AnalyzerResults API response before asserting on UI —
  // the page navigates instantly but the component fetches data async.
  // On CI under load, this fetch can exceed 10s.
  await page
    .waitForResponse(
      (resp) =>
        resp.url().includes("/rest/AnalyzerResults") && resp.status() === 200,
      { timeout: LONG_TIMEOUT },
    )
    .catch((e) => {
      // TimeoutError = response arrived before we started listening (fast backend).
      // Any other error is unexpected — log it for diagnostics.
      if (!(e instanceof Error && e.message.includes("Timeout"))) {
        console.error(`[waitForResponse] unexpected: ${e}`);
      }
    });
  const emptyState = page.locator('[data-testid="analyzer-results-empty"]');
  await expect(saveButton.or(emptyState).first()).toBeVisible({
    timeout: LONG_TIMEOUT,
  });

  const saveStillVisible = await saveButton.isVisible();
  if (saveStillVisible) {
    if (stagedCountBeforeSave > 0) {
      await expect
        .poll(async () => stagedRows().count(), {
          timeout: LONG_TIMEOUT,
        })
        .toBe(0);
    }

    await expect(saveInProgress).toBeHidden({ timeout: LONG_TIMEOUT });
    await expect(saveButton).toBeEnabled({ timeout: LONG_TIMEOUT });
  }

  // ── Verify in OE results view, not on the staging page ───────────
  await presentation.step(
    stepOffset + 3,
    "Viewing accepted results in OpenELIS",
  );
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
  // Hold on AccessionResults so the viewer can see the final outcome
  await presentation.pause(5_000);
}
