import { test, expect } from "../../../helpers/test-base";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";
import { AnalyzerFormPage } from "../../../fixtures/analyzer-form";
import {
  ensureAnalyzerByName,
  GENEXPERT_DEFAULT_ANALYZER,
} from "../../../helpers/ensure-analyzer";
import {
  QUICK_TIMEOUT,
  SHORT_TIMEOUT,
  LONG_TIMEOUT,
} from "../../../helpers/timeouts";

test.describe("Analyzer Plugin Config", () => {
  test("profile selection prefills implemented analyzer fields", async ({
    page,
  }) => {
    const list = new AnalyzerListPage(page);
    const form = new AnalyzerFormPage(page);

    await list.goto();
    await list.expectLoaded();

    // Open the inline setup panel and select a profile. When running in
    // parallel with other tests that also hit /analyzers, the panel can
    // close from session interference. Retry the full open→select flow if
    // the profile dropdown isn't reachable.
    let selectedProfile = false;
    for (let attempt = 1; attempt <= 3; attempt++) {
      await list.clickAdd();
      await form.expectOpen();

      try {
        await expect(form.defaultConfigDropdown).toBeVisible({
          timeout: SHORT_TIMEOUT,
        });
      } catch {
        // Modal may have closed — retry
        if (
          (await form.modal.isVisible()) &&
          (await form.cancelButton.isVisible())
        ) {
          await form.cancelButton.click();
        }
        await expect(form.modal).not.toBeVisible({ timeout: QUICK_TIMEOUT });
        continue;
      }

      try {
        await form.selectProfile("GeneXpert");
        selectedProfile = true;
      } catch {
        // Selection failed (e.g. options not rendered) — retry
      }
      if (selectedProfile) break;

      // Close form and retry
      if (await form.modal.isVisible()) {
        if (await form.cancelButton.isVisible())
          await form.cancelButton.click();
        await expect(form.modal).not.toBeVisible({ timeout: QUICK_TIMEOUT });
      }
    }
    expect(
      selectedProfile,
      "GeneXpert profile option should be selectable",
    ).toBeTruthy();

    // Selecting the profile should prefill the summary with its resolved
    // protocol and analyzer-type category — the inline flow has no separate
    // identifier-pattern/analyzer-type fields to inspect directly.
    await expect(form.profileSummary).toBeVisible();
    await expect(form.profileSummary).toContainText(/GeneXpert/i);
    await expect(form.profileSummary).toContainText("ASTM");
    await expect(form.profileSummary).toContainText(/MOLECULAR/i);
    await form.cancelButton.click();
    await expect(form.modal).not.toBeVisible();
  });

  test("mappings page shows plugin-config snapshot and pending-codes panel", async ({
    page,
  }) => {
    const analyzerId = await ensureAnalyzerByName(
      page.request,
      (a) => a.name?.includes("GeneXpert") && !a.name?.includes("E2E"),
      GENEXPERT_DEFAULT_ANALYZER,
    );

    const list = new AnalyzerListPage(page);

    await list.goto();
    await list.expectLoaded();
    await list.openOverflowMenu(analyzerId);
    await list.clickAction(analyzerId, "mappings");

    await expect(page).toHaveURL(
      new RegExp(`/analyzers/${analyzerId}/mappings`),
    );

    // The raw plugin-config JSON snapshot is an advanced/debug view gated
    // behind `?advanced=1` — reload with it to exercise the panel.
    await page.goto(`/analyzers/${analyzerId}/mappings?advanced=1`, {
      waitUntil: "domcontentloaded",
    });
    await expect(page.locator('[data-testid="field-mapping"]')).toBeVisible({
      timeout: LONG_TIMEOUT,
    });

    const snapshotTile = page.locator('[data-testid="plugin-config-snapshot"]');
    await expect(snapshotTile).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(snapshotTile).toContainText("{");

    await expect(
      page.locator('[data-testid="pending-codes-panel"]'),
    ).toBeVisible();
  });
});
