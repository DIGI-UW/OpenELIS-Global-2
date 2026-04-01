import { test, expect } from "@playwright/test";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";
import { AnalyzerFormPage } from "../../../fixtures/analyzer-form";
import {
  ensureAnalyzerByName,
  GENEXPERT_DEFAULT_ANALYZER,
} from "../../../helpers/ensure-analyzer";
import {
  QUICK_TIMEOUT,
  SHORT_TIMEOUT,
  UI_TIMEOUT,
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

    // Open form and select plugin. When running in parallel with other tests
    // that also hit /analyzers, the modal can close from session interference.
    // Retry the full open→select flow if the dropdown isn't reachable.
    let selectedPlugin = false;
    for (let attempt = 1; attempt <= 3; attempt++) {
      await list.clickAdd();
      await form.expectOpen();

      try {
        await expect(form.pluginTypeDropdown).toBeVisible({
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

      for (let sel = 1; sel <= 4; sel++) {
        // Carbon places data-testid on wrapper div; click inner trigger button
        const trigger = form.pluginTypeDropdown.locator(
          'button[role="combobox"], .cds--list-box__field',
        );
        await trigger.click();
        const genericAstmOption = page
          .getByRole("option", { name: /Generic ASTM/i })
          .first();
        let optionVisible = false;
        try {
          await expect(genericAstmOption).toBeVisible({
            timeout: QUICK_TIMEOUT,
          });
          optionVisible = true;
        } catch {
          // Option not rendered in this attempt — retry
        }
        if (optionVisible) {
          await genericAstmOption.click();
          selectedPlugin = true;
          break;
        }
        await page.keyboard.press("Escape");
        await expect(genericAstmOption).not.toBeVisible({
          timeout: QUICK_TIMEOUT,
        });
      }
      if (selectedPlugin) break;

      // Close form and retry
      if (await form.modal.isVisible()) {
        if (await form.cancelButton.isVisible())
          await form.cancelButton.click();
        await expect(form.modal).not.toBeVisible({ timeout: QUICK_TIMEOUT });
      }
    }
    expect(
      selectedPlugin,
      "Generic ASTM plugin option should be selectable",
    ).toBeTruthy();

    await expect(form.defaultConfigDropdown).toBeVisible();
    // Carbon: click inner trigger, not wrapper div
    const configTrigger = form.defaultConfigDropdown.locator(
      'button[role="combobox"], .cds--list-box__field',
    );
    await configTrigger.click();
    const geneXpertProfile = page
      .getByRole("option", { name: /GeneXpert.*ASTM/i })
      .first();
    await expect(geneXpertProfile).toBeVisible({ timeout: UI_TIMEOUT });
    await geneXpertProfile.click();

    // Selecting the profile should prefill key analyzer fields.
    await expect(form.identifierPatternInput).toHaveValue(/GENEXPERT/i);
    await expect(form.typeDropdown).toContainText(/Molecular/i);
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

    const snapshotTile = page.locator('[data-testid="plugin-config-snapshot"]');
    await expect(snapshotTile).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(snapshotTile).toContainText("{");

    await expect(
      page.locator('[data-testid="pending-codes-panel"]'),
    ).toBeVisible();
  });
});
