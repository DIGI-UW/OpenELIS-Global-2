import { test, expect } from "@playwright/test";
import { AnalyzerListPage } from "../fixtures/analyzer-list";
import { AnalyzerFormPage } from "../fixtures/analyzer-form";

/**
 * Analyzer Form E2E Tests — GeneXpert ASTM Create/Edit Workflow
 *
 * Tests the full lifecycle of creating and editing a GeneXpert ASTM analyzer
 * using the Generic ASTM plugin with default config loading from templates.
 *
 * Prerequisites:
 *   - Harness running with analyzer fixtures loaded
 *   - Generic ASTM plugin type registered in analyzer_type table
 *   - Default configs available at /data/analyzer-defaults/astm/
 */
test.describe("Analyzer Form - GeneXpert ASTM Create/Edit", () => {
  test.describe.configure({ mode: "serial" });

  const uniqueSuffix = Date.now();
  const analyzerName = `TEST-GeneXpert-${uniqueSuffix}`;
  let createdAnalyzerId: string;

  test.afterAll(async ({ request }) => {
    // Cleanup: delete the test analyzer if it was created
    if (createdAnalyzerId) {
      try {
        await request.post(
          `/rest/analyzer/analyzers/${createdAnalyzerId}/delete`,
        );
      } catch {
        // Best effort cleanup
      }
    }
  });

  test("creates a GeneXpert ASTM analyzer with default config", async ({
    page,
  }) => {
    const list = new AnalyzerListPage(page);
    const form = new AnalyzerFormPage(page);

    // Navigate to analyzer list and open add form
    await list.goto();
    await list.expectLoaded();
    await list.clickAdd();
    await form.expectOpen();

    // Fill instance identity
    await form.fillName(analyzerName);

    // Select plugin type: Generic ASTM
    await form.selectPluginType("Generic ASTM");

    // Load default config: GeneXpert
    await expect(form.defaultConfigDropdown).toBeVisible({ timeout: 5000 });
    await form.selectDefaultConfig("Cepheid GeneXpert");

    // Verify config-level fields were auto-populated
    // identifierPattern should be set from the config template
    const identifierPattern = await form.getIdentifierPattern();
    expect(identifierPattern).toContain("GENEXPERT");

    // Name should NOT be overwritten by config (instance field)
    const name = await form.getName();
    expect(name).toBe(analyzerName);

    // Fill instance-specific connection details
    await form.fillIpAddress("192.168.1.100");
    await form.fillPort("1200");

    // Save
    await form.save();
    await form.expectSuccessNotification();

    // Wait for modal to close and list to refresh
    await expect(form.modal).not.toBeVisible({ timeout: 5000 });

    // Verify analyzer appears in the list
    await list.goto();
    await list.expectLoaded();
    await list.search(analyzerName);

    // Find the row and extract ID for cleanup
    const rows = page.locator("tbody tr");
    await expect(rows).toHaveCount(1, { timeout: 5000 });

    // Store ID for cleanup (from data-testid pattern)
    const row = rows.first();
    const testid = await row.getAttribute("data-testid");
    if (testid && testid.startsWith("analyzer-row-")) {
      createdAnalyzerId = testid.replace("analyzer-row-", "");
    }
  });

  test("edits an existing GeneXpert analyzer", async ({ page }) => {
    test.skip(!createdAnalyzerId, "Requires analyzer created in previous test");

    const list = new AnalyzerListPage(page);
    const form = new AnalyzerFormPage(page);

    // Navigate and find the analyzer
    await list.goto();
    await list.expectLoaded();
    await list.search(analyzerName);

    // Open edit via overflow menu
    await list.openOverflowMenu(createdAnalyzerId);
    await list.clickAction(createdAnalyzerId, "edit");
    await form.expectOpen();

    // Verify fields are populated from the saved analyzer
    const name = await form.getName();
    expect(name).toBe(analyzerName);

    const port = await form.getPort();
    expect(port).toBe("1200");

    // Change port
    await form.fillPort("1300");

    // Save
    await form.save();
    await form.expectSuccessNotification();

    // Wait for modal to close
    await expect(form.modal).not.toBeVisible({ timeout: 5000 });

    // Reopen edit and verify port updated
    await list.goto();
    await list.expectLoaded();
    await list.search(analyzerName);
    await list.openOverflowMenu(createdAnalyzerId);
    await list.clickAction(createdAnalyzerId, "edit");
    await form.expectOpen();

    const updatedPort = await form.getPort();
    expect(updatedPort).toBe("1300");

    await form.cancelButton.click();
  });

  test("loads default config in edit mode", async ({ page }) => {
    test.skip(!createdAnalyzerId, "Requires analyzer created in previous test");

    const list = new AnalyzerListPage(page);
    const form = new AnalyzerFormPage(page);

    // Navigate and open edit
    await list.goto();
    await list.expectLoaded();
    await list.search(analyzerName);
    await list.openOverflowMenu(createdAnalyzerId);
    await list.clickAction(createdAnalyzerId, "edit");
    await form.expectOpen();

    // Verify "Load Default Config" dropdown is visible in edit mode
    await expect(form.defaultConfigDropdown).toBeVisible({ timeout: 5000 });

    // Load a config and verify it updates config-level fields
    await form.selectDefaultConfig("Cepheid GeneXpert");

    const identifierPattern = await form.getIdentifierPattern();
    expect(identifierPattern).toContain("GENEXPERT");

    await form.cancelButton.click();
  });

  test("form validation rejects missing required fields", async ({ page }) => {
    const list = new AnalyzerListPage(page);
    const form = new AnalyzerFormPage(page);

    // Navigate and open add form
    await list.goto();
    await list.expectLoaded();
    await list.clickAdd();
    await form.expectOpen();

    // Try to save with empty form
    await form.save();

    // Validation errors should appear (name is required)
    const nameError = page.locator(
      '[data-testid="analyzer-form-name-input"] .cds--form-requirement',
    );
    await expect(nameError).toBeVisible({ timeout: 3000 });

    await form.cancelButton.click();
  });
});
