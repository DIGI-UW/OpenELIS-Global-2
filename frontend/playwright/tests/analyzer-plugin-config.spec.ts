import { test, expect } from "@playwright/test";
import { AnalyzerListPage } from "../fixtures/analyzer-list";
import { AnalyzerFormPage } from "../fixtures/analyzer-form";

test.describe("Analyzer Plugin Config", () => {
  test("profile selection prefills implemented analyzer fields", async ({ page }) => {
    const list = new AnalyzerListPage(page);
    const form = new AnalyzerFormPage(page);

    await list.goto();
    await list.expectLoaded();
    await list.clickAdd();
    await form.expectOpen();

    // Generic profile defaults are only available when a generic plugin is selected.
    // Plugin options load async, so retry opening/selecting briefly.
    let selectedPlugin = false;
    for (let attempt = 1; attempt <= 4; attempt++) {
      await form.pluginTypeDropdown.click();
      const genericAstmOption = page
        .getByRole("option", { name: /Generic ASTM/i })
        .first();
      if (await genericAstmOption.isVisible().catch(() => false)) {
        await genericAstmOption.click();
        selectedPlugin = true;
        break;
      }
      await page.keyboard.press("Escape");
      await page.waitForTimeout(1_000);
    }
    expect(selectedPlugin, "Generic ASTM plugin option should be selectable").toBeTruthy();

    await expect(form.defaultConfigDropdown).toBeVisible();
    await form.defaultConfigDropdown.click();
    const geneXpertProfile = page
      .getByRole("option", { name: /GeneXpert.*ASTM/i })
      .first();
    await expect(geneXpertProfile).toBeVisible({ timeout: 10_000 });
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
    const analyzerId = "2013";
    const list = new AnalyzerListPage(page);

    await list.goto();
    await list.expectLoaded();
    await list.openOverflowMenu(analyzerId);
    await list.clickAction(analyzerId, "mappings");

    await expect(page).toHaveURL(new RegExp(`/analyzers/${analyzerId}/mappings`));

    const snapshotTile = page.locator('[data-testid="plugin-config-snapshot"]');
    await expect(snapshotTile).toBeVisible({ timeout: 15_000 });
    await expect(snapshotTile).toContainText("{");

    await expect(page.locator('[data-testid="pending-codes-panel"]')).toBeVisible();
  });
});
