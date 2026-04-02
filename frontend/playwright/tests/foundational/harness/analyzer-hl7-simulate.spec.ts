import { test, expect } from "@playwright/test";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";
import { UI_TIMEOUT, LONG_TIMEOUT } from "../../../helpers/timeouts";

const HL7_ANALYZER_NAME = "Mindray BC-5380";

test.describe("Analyzer HL7 Simulator", () => {
  test("simulates HL7 message and previews mapping for Mindray BC-5380", async ({
    page,
  }) => {
    const simulatorRes = await page.request.post(
      "http://localhost:8085/simulate/hl7/mindray_bc5380",
    );
    expect(simulatorRes.ok()).toBeTruthy();

    const baseUrl = process.env.BASE_URL || "https://localhost";
    const apiBase = `${baseUrl}/api/OpenELIS-Global/rest/analyzer`;
    const analyzersRes = await page.request.get(`${apiBase}/analyzers`);
    expect(analyzersRes.ok()).toBeTruthy();

    const analyzersBody = await analyzersRes.json();
    const analyzers = (analyzersBody?.analyzers || []) as {
      id: string;
      name: string;
    }[];
    const analyzer = analyzers.find((row) => row.name === HL7_ANALYZER_NAME);
    expect(analyzer).toBeDefined();

    const list = new AnalyzerListPage(page);
    await list.goto();
    await list.expectLoaded();
    await list.openOverflowMenu(analyzer!.id);
    await list.clickAction(analyzer!.id, "mappings");

    await expect(page).toHaveURL(
      new RegExp(`/analyzers/${analyzer!.id}/mappings`),
    );
    await page.locator('[data-testid="field-mapping-test-button"]').click({
      timeout: UI_TIMEOUT,
    });
    await expect(
      page.locator('[data-testid="test-mapping-modal"]'),
    ).toBeVisible();

    await page
      .locator('[data-testid="test-mapping-message-input"]')
      .fill(
        "MSH|^~\\&|MINDRAY BC-5380|LAB|OpenELIS|LAB|20260123120000||ORU^R01|MINDRAY001|P|2.5.1||||||||\n" +
          "PID|1||PAT001^^^HOSPITAL||DOE^JOHN||19800115|M||||||||||||||||||||\n" +
          "OBR|1|PLACER123|FILLER456|1|^^^CBC^COMPLETE BLOOD COUNT|||20260123110000|||||||||||||||||||||||F|||||||||||||||||||||||||\n" +
          "OBX|1|NM|^^^WBC^WHITE BLOOD CELL||7.5|10*3/uL|||||F||||||\n" +
          "OBX|2|NM|^^^HGB^HEMOGLOBIN||14.2|g/dL|||||F||||||",
      );

    await page.locator('[data-testid="test-mapping-preview-button"]').click();
    const results = page.locator('[data-testid="test-mapping-results"]');
    await expect(results).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(results.locator("tbody tr").first()).toBeVisible({
      timeout: UI_TIMEOUT,
    });
  });
});
