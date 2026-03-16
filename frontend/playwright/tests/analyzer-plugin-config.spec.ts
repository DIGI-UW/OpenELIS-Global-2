import { test, expect } from "@playwright/test";
import { AnalyzerListPage } from "../fixtures/analyzer-list";
import { AnalyzerFormPage } from "../fixtures/analyzer-form";

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
        await expect(form.pluginTypeDropdown).toBeVisible({ timeout: 5_000 });
      } catch {
        // Modal may have closed — retry
        if (await form.modal.isVisible()) {
          await form.cancelButton.click().catch(() => {});
        }
        await page.waitForTimeout(1_000);
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
        if (await genericAstmOption.isVisible().catch(() => false)) {
          await genericAstmOption.click();
          selectedPlugin = true;
          break;
        }
        await page.keyboard.press("Escape");
        await page.waitForTimeout(1_000);
      }
      if (selectedPlugin) break;

      // Close form and retry
      if (await form.modal.isVisible()) {
        await form.cancelButton.click().catch(() => {});
        await expect(form.modal)
          .not.toBeVisible({ timeout: 2_000 })
          .catch(() => {});
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
    // Find or create a GeneXpert analyzer for testing
    const listResp = await page.request.get(
      "/api/OpenELIS-Global/rest/analyzer/analyzers",
    );
    const data = await listResp.json();
    const existing = (data.analyzers ?? []).find(
      (a: any) => a.name?.includes("GeneXpert") && !a.name?.includes("E2E"),
    );

    let analyzerId: string;
    if (existing) {
      analyzerId = String(existing.id);
    } else {
      const createResp = await page.request.post(
        "/api/OpenELIS-Global/rest/analyzer/analyzers",
        {
          data: {
            name: "Cepheid GeneXpert (ASTM Mode)",
            analyzerType: "MOLECULAR",
            pluginTypeId: "generic-astm",
            ipAddress: "172.21.1.100",
            port: 9600,
            protocolVersion: "ASTM_LIS2_A2",
            identifierPattern: "GENEXPERT|CEPHEID",
            status: "ACTIVE",
            defaultConfigId: "astm/genexpert-astm",
          },
        },
      );
      const created = await createResp.json();
      analyzerId = String(created.id);
    }

    const list = new AnalyzerListPage(page);

    await list.goto();
    await list.expectLoaded();
    await list.openOverflowMenu(analyzerId);
    await list.clickAction(analyzerId, "mappings");

    await expect(page).toHaveURL(
      new RegExp(`/analyzers/${analyzerId}/mappings`),
    );

    const snapshotTile = page.locator('[data-testid="plugin-config-snapshot"]');
    await expect(snapshotTile).toBeVisible({ timeout: 15_000 });
    await expect(snapshotTile).toContainText("{");

    await expect(
      page.locator('[data-testid="pending-codes-panel"]'),
    ).toBeVisible();
  });
});
