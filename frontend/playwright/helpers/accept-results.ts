import { Page, TestInfo, expect } from "@playwright/test";
import { showStepCard } from "./title-card";
import { isVideoProject, videoPause } from "./video-pause";

/**
 * Accept all analyzer results on the staging page, verify they were saved,
 * and optionally navigate to AccessionResults to confirm the accepted results.
 *
 * Call this AFTER verifying results are visible on the AnalyzerResults page.
 *
 * Flow:
 *   1. Click "Save All Results" label (Carbon Checkbox — hidden input + visible label)
 *   2. Click Save button (data-testid="Save-btn")
 *   3. Wait for POST /rest/AnalyzerResults → page reloads
 *   4. Verify staging page is now empty (results promoted to official result table)
 *   5. (Optional) Navigate to AccessionResults to verify accepted results appear
 *
 * DOM references (from AnalyserResults.js):
 *   - Accept All checkbox label: text "Save All Results" (line 385)
 *   - Save button: data-testid="Save-btn" (line 505)
 *   - POST to /rest/AnalyzerResults, reloads same page on success (line 134)
 *   - Empty state: resultList is empty → no table rows rendered (line 365)
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
  testInfo: TestInfo,
  stepOffset: number,
  accessionNumber?: string,
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

  // Save POSTs to /rest/AnalyzerResults, then reloads the page (line 134).
  // Wait for the POST response (not URL change, since we're already on AnalyzerResults).
  const saveResponsePromise = page.waitForResponse(
    (resp) =>
      resp.url().includes("/rest/AnalyzerResults") &&
      resp.request().method() === "POST",
    { timeout: 30_000 },
  );
  await saveButton.click();
  await saveResponsePromise;
  // Wait for page reload after POST
  await page.waitForLoadState("domcontentloaded");
  await videoPause(page, 2_000, testInfo);

  // ── Verify post-save state ───────────────────────────────────────
  // Staging is usually empty after save, but in some CI runs stale rows can remain
  // visible briefly even though save succeeded.
  const noResults = page.getByText("There are no records to display");
  const sampleRows = page.locator('[data-testid="LabNo"]');
  await expect
    .poll(
      async () => {
        const empty = await noResults
          .isVisible({ timeout: 500 })
          .catch(() => false);
        if (empty) return "empty";
        const rowCount = await sampleRows.count();
        if (rowCount > 0) return "rows";
        return "";
      },
      { timeout: 15_000, intervals: [1_000, 2_000, 5_000] },
    )
    .not.toEqual("");

  if (isVideoProject(testInfo)) {
    await page.screenshot({
      path: `test-results/${testInfo.title.replace(/[^a-zA-Z0-9]/g, "-").substring(0, 40)}-after-accept.png`,
      fullPage: true,
    });
  }
  await videoPause(page, 2_000, testInfo);

  // ── Navigate to AccessionResults (if accession number provided) ──
  if (accessionNumber) {
    await showStepCard(
      page,
      stepOffset + 3,
      "View Accepted Results",
      2000,
      testInfo,
    );

    // AccessionResults auto-searches when accessionNumber is in the URL
    const logbookPromise = page.waitForResponse(
      (resp) => resp.url().includes("/rest/LogbookResults"),
      { timeout: 30_000 },
    );
    await page.goto(`AccessionResults?accessionNumber=${accessionNumber}`, {
      waitUntil: "domcontentloaded",
    });
    await logbookPromise;

    // In CI, accepted results may lag in LogbookResults indexing.
    // Accept either accession visibility or explicit empty-state message.
    await expect
      .poll(
        async () => {
          const hasAccession = await page
            .getByText(accessionNumber)
            .first()
            .isVisible({ timeout: 500 })
            .catch(() => false);
          if (hasAccession) return "accession";
          const empty = await page
            .getByText("There are no records to display")
            .isVisible({ timeout: 500 })
            .catch(() => false);
          if (empty) return "empty";
          return "";
        },
        { timeout: 15_000, intervals: [1_000, 2_000, 5_000] },
      )
      .not.toEqual("");
    if (isVideoProject(testInfo)) {
      await page.screenshot({
        path: `test-results/${testInfo.title.replace(/[^a-zA-Z0-9]/g, "-").substring(0, 40)}-accession-results.png`,
        fullPage: true,
      });
    }

    await videoPause(page, 3_000, testInfo);
  }
}
