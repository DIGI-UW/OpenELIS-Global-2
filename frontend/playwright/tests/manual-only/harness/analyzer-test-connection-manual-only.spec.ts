import { test, expect } from "../../../helpers/test-base";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";
import { AnalyzerSetupPage } from "../../../fixtures/analyzer-setup";
import { cleanupAnalyzerByName } from "../../../helpers/cleanup-analyzer";

const GENEXPERT_HOST = process.env.GENEXPERT_HOST;
const GENEXPERT_PORT = process.env.GENEXPERT_PORT || "1200";

/**
 * Manual-only real-device connectivity coverage.
 *
 * This spec is intentionally excluded from ordinary PR CI. It is for explicit
 * operator-driven runs against real hardware.
 */
test.describe("Real GeneXpert Test Connection (Manual Only)", () => {
  test.skip(
    !GENEXPERT_HOST || process.env.CI === "true",
    "Set GENEXPERT_HOST (and optionally GENEXPERT_PORT) and run outside CI",
  );
  test.describe.configure({ mode: "serial" });

  const uniqueSuffix = Date.now();
  const analyzerName = `E2E-GeneXpert-Real-${uniqueSuffix}`;
  test("creates and tests a real GeneXpert connection through inline setup", async ({
    page,
  }) => {
    const list = new AnalyzerListPage(page);
    const setup = new AnalyzerSetupPage(page);

    await list.goto();
    await list.expectLoaded();
    await list.clickAdd();
    await setup.expectOpen();
    await setup.selectProfile("Cepheid GeneXpert (ASTM Mode)");
    await setup.fillName(analyzerName);
    await setup.selectFirstLabUnit();
    await setup.continueToVerify();
    await setup.continueToConnect();
    await setup.fillNetworkAddress(GENEXPERT_HOST!);
    await setup.fillPort(GENEXPERT_PORT);
    await setup.testConnection();

    const createdAnalyzerId = new URL(page.url()).searchParams.get(
      "analyzerId",
    );
    expect(createdAnalyzerId).toBeTruthy();
    await setup.close();

    await list.search(analyzerName);
    const rows = page.locator("tbody tr");
    await expect(rows).toHaveCount(1);
    await expect(
      page.locator(`[data-testid="plugin-warning-${createdAnalyzerId}"]`),
    ).toHaveCount(0);

    if (!process.env.SKIP_CLEANUP) {
      await cleanupAnalyzerByName(page, analyzerName);
    }
  });
});
