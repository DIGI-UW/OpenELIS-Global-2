import { test, expect } from "@playwright/test";
import { AnalyzerListPage } from "../fixtures/analyzer-list";
import { AnalyzerFormPage } from "../fixtures/analyzer-form";

/**
 * Demo: QuantStudio 7 — Generic File plugin configuration walkthrough.
 *
 * Records the full E2E workflow:
 *   1. Navigate to Analyzers list
 *   2. Open Add Analyzer form
 *   3. Select "Generic File" plugin type
 *   4. Select "QuantStudio QS5/QS7" default profile
 *   5. Verify prefilled fields (category = MOLECULAR)
 *   6. Verify FILE-specific form: no IP/port, no protocol version, FILE info tile shown
 *   7. Fill name, save
 *   8. Verify analyzer appears in list
 *   9. Verify FileImportConfiguration created with EXCEL format
 *  10. Cleanup
 *
 * Run with: PLAYWRIGHT_VIDEO=on TEST_USER=admin TEST_PASS="adminADMIN!" \
 *           npx playwright test demo-quantstudio --project=analyzer-harness
 */
// Video recording — only enable when explicitly requested via env var.
// Playwright requires test.use outside describe.
if (process.env.PLAYWRIGHT_VIDEO === "on") {
  test.use({ video: "on" });
}

test.describe("Demo: QuantStudio 7 Generic File Config", () => {
  test.describe.configure({ mode: "serial" });

  const uniqueSuffix = Date.now();
  const analyzerName = `QuantStudio-7-Demo-${uniqueSuffix}`;
  let createdAnalyzerId: string;

  test.afterAll(async ({ request }) => {
    if (createdAnalyzerId) {
      try {
        await request.post(
          `/api/OpenELIS-Global/rest/analyzer/analyzers/${createdAnalyzerId}/delete`,
        );
      } catch {
        // Best effort cleanup
      }
    }
  });

  test("create QuantStudio 7 analyzer via Generic File plugin + profile", async ({
    page,
    baseURL,
  }) => {
    const list = new AnalyzerListPage(page);
    const form = new AnalyzerFormPage(page);

    // Step 1: Navigate to Analyzers list
    await list.goto();
    await list.expectLoaded();
    await page.waitForTimeout(1_000); // Pause for video

    // Step 2: Open Add Analyzer form
    await list.clickAdd();
    await form.expectOpen();
    await page.waitForTimeout(500);

    // Step 3: Select "Generic File" plugin type
    await form.pluginTypeDropdown.click();
    const genericFileOption = page
      .getByRole("option", { name: /Generic File/i })
      .first();
    await expect(genericFileOption).toBeVisible({ timeout: 10_000 });
    await page.waitForTimeout(500); // Show the dropdown open
    await genericFileOption.click();
    await page.waitForTimeout(500);

    // Step 4: Verify FILE-specific form behavior
    // Protocol version dropdown should NOT be visible
    await expect(form.protocolVersionDropdown).not.toBeVisible();
    // Connection fields (IP/port/test-connection) should NOT be visible
    await expect(form.connectionFields).not.toBeVisible();
    // FILE protocol info tile SHOULD be visible
    await expect(form.fileProtocolInfo).toBeVisible();
    await page.waitForTimeout(500);

    // Step 5: Select "QuantStudio QS5/QS7" default profile
    await expect(form.defaultConfigDropdown).toBeVisible();
    await form.defaultConfigDropdown.click();

    // Profile dropdown should only show FILE profiles (not ASTM/HL7)
    const quantStudioOption = page
      .getByRole("option", { name: /QuantStudio/i })
      .first();
    await expect(quantStudioOption).toBeVisible({ timeout: 10_000 });
    await page.waitForTimeout(500); // Show profile options
    await quantStudioOption.click();
    await page.waitForTimeout(1_000); // Let profile load and fields populate

    // Step 6: Verify prefilled fields from QuantStudio profile
    // The profile should set the analyzer type/category to MOLECULAR
    await expect(form.typeDropdown).toContainText(/Molecular/i);

    // Step 7: Fill name
    await form.fillName(analyzerName);
    await page.waitForTimeout(500);

    // Step 8: Save — intercept API response for diagnostics
    const saveResponsePromise = page.waitForResponse(
      (resp) =>
        resp.url().includes("/rest/analyzer/analyzers") &&
        resp.request().method() === "POST",
      { timeout: 15_000 },
    );
    await form.save();
    const saveResponse = await saveResponsePromise;
    if (!saveResponse.ok()) {
      const body = await saveResponse.text().catch(() => "no body");
      console.error(`Save API returned ${saveResponse.status()}: ${body}`);
    }
    await form.expectSuccessNotification();
    await page.waitForTimeout(1_500); // Show success notification

    // Modal auto-closes after 1s
    await expect(form.modal).not.toBeVisible({ timeout: 5_000 });

    // Step 9: Verify analyzer appears in list
    await list.expectLoaded();
    await list.search(analyzerName);
    await page.waitForTimeout(500);

    const rows = page.locator("tbody tr");
    await expect(rows).toHaveCount(1, { timeout: 10_000 });
    const row = rows.first();
    await expect(row).toContainText(analyzerName);
    await expect(row).toContainText("MOLECULAR");
    await page.waitForTimeout(1_000);

    // Extract ID for cleanup
    const testid = await row.getAttribute("data-testid");
    if (testid && testid.startsWith("analyzer-row-")) {
      createdAnalyzerId = testid.replace("analyzer-row-", "");
    }

    // Step 10: Verify file-import configuration was created
    if (createdAnalyzerId) {
      const base = baseURL || process.env.BASE_URL || "https://localhost";
      const api = `${base}/api/OpenELIS-Global/rest/analyzer`;
      const cfgRes = await page.request.get(
        `${api}/file-import/configurations/analyzer/${createdAnalyzerId}`,
      );
      expect(cfgRes.ok()).toBeTruthy();
      const cfg = await cfgRes.json();
      expect(cfg.fileFormat).toBe("EXCEL");
      expect(cfg.active).toBe(true);
    }

    await page.waitForTimeout(2_000); // Final pause for video
  });
});
