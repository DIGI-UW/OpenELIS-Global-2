import { test, expect } from "@playwright/test";
import { AnalyzerListPage } from "../fixtures/analyzer-list";
import { AnalyzerFormPage } from "../fixtures/analyzer-form";

/**
 * Analyzer Form E2E Tests
 *
 * Tests core form workflows: open/close, validation, and create/edit.
 * Designed to run in standard CI (no analyzer harness or default configs required).
 */
test.describe("Analyzer Form", () => {
  test.describe.configure({ mode: "serial" });

  const uniqueSuffix = Date.now();
  const analyzerName = `TEST-Analyzer-${uniqueSuffix}`;
  let createdAnalyzerId: string;

  test.afterAll(async ({ request }) => {
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

  test("opens add form and validates required fields", async ({ page }) => {
    const list = new AnalyzerListPage(page);
    const form = new AnalyzerFormPage(page);

    await list.goto();
    await list.expectLoaded();
    await list.clickAdd();
    await form.expectOpen();

    // Save with empty form — should show validation error
    await form.save();

    // Carbon TextInput shows invalidText and sets data-invalid on the input
    const nameInput = page.locator(
      '[data-testid="analyzer-form-name-input"] input',
    );
    await expect(nameInput).toHaveAttribute("data-invalid", "true");
    await expect(page.getByText("Analyzer name is required")).toBeVisible();

    await form.cancelButton.click();
    await expect(form.modal).not.toBeVisible();
  });

  test("creates analyzer with minimal fields", async ({ page }) => {
    const list = new AnalyzerListPage(page);
    const form = new AnalyzerFormPage(page);

    await list.goto();
    await list.expectLoaded();
    await list.clickAdd();
    await form.expectOpen();

    // Fill only required fields
    await form.fillName(analyzerName);
    await form.selectType("Molecular");

    // Save
    await form.save();
    await form.expectSuccessNotification();
    await expect(form.modal).not.toBeVisible();

    // Verify analyzer appears in the list
    await list.goto();
    await list.expectLoaded();
    await list.search(analyzerName);

    const rows = page.locator("tbody tr");
    await expect(rows).toHaveCount(1);

    // Store ID for edit test and cleanup
    const row = rows.first();
    const testid = await row.getAttribute("data-testid");
    if (testid && testid.startsWith("analyzer-row-")) {
      createdAnalyzerId = testid.replace("analyzer-row-", "");
    }
  });

  test("edits an existing analyzer", async ({ page }) => {
    test.skip(!createdAnalyzerId, "Requires analyzer created in previous test");

    const list = new AnalyzerListPage(page);
    const form = new AnalyzerFormPage(page);

    await list.goto();
    await list.expectLoaded();
    await list.search(analyzerName);

    // Open edit
    await list.openOverflowMenu(createdAnalyzerId);
    await list.clickAction(createdAnalyzerId, "edit");
    await form.expectOpen();

    // Verify name is populated
    const name = await form.getName();
    expect(name).toBe(analyzerName);

    // Update port
    await form.fillPort("1300");

    // Save
    await form.save();
    await form.expectSuccessNotification();
    await expect(form.modal).not.toBeVisible();
  });
});
