import { test, expect } from "../../../helpers/test-base";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";
import { AnalyzerFormPage } from "../../../fixtures/analyzer-form";
import { LONG_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

interface AnalyzerSummary {
  id: string | number;
  name?: string;
}

async function findAnalyzerIdByName(
  request: import("@playwright/test").APIRequestContext,
  name: string,
): Promise<string> {
  const response = await request.get(
    "/api/OpenELIS-Global/rest/analyzer/analyzers",
  );
  expect(response.ok()).toBeTruthy();
  const body = await response.json();
  const analyzer = (body.analyzers || []).find(
    (row: AnalyzerSummary) => row.name === name,
  );
  expect(analyzer).toBeTruthy();
  return String(analyzer.id);
}

async function firstDisplayTestId(
  request: import("@playwright/test").APIRequestContext,
): Promise<string> {
  const response = await request.get(
    "/api/OpenELIS-Global/rest/displayList/ALL_TESTS",
  );
  expect(response.ok()).toBeTruthy();
  const tests = await response.json();
  expect(Array.isArray(tests)).toBeTruthy();
  expect(tests.length).toBeGreaterThan(0);
  return String(tests[0].id);
}

async function getCsrfToken(
  page: import("@playwright/test").Page,
): Promise<string> {
  const state = await page.context().storageState();
  for (const origin of state.origins) {
    for (const item of origin.localStorage) {
      if (item.name === "CSRF") {
        return item.value;
      }
    }
  }
  return "";
}

test.describe("Analyzer QC/config MVP", () => {
  const analyzerName = `PW-QC-MVP-${Date.now()}`;
  let analyzerId = "";
  let controlLotId = "";
  let csrfToken = "";

  test.afterAll(async ({ request }) => {
    const headers = csrfToken ? { "X-CSRF-Token": csrfToken } : undefined;
    if (controlLotId) {
      await request
        .put(
          `/api/OpenELIS-Global/rest/qc/controlLot/${controlLotId}/deactivate`,
          { headers },
        )
        .catch(() => undefined);
    }
    if (analyzerId) {
      await request
        .post(
          `/api/OpenELIS-Global/rest/analyzer/analyzers/${analyzerId}/delete`,
          { headers },
        )
        .catch(() => undefined);
    }
  });

  test("creates analyzer from profile and verifies mapping/QC setup readiness", async ({
    page,
    request,
  }) => {
    test.setTimeout(90_000);

    const list = new AnalyzerListPage(page);
    const form = new AnalyzerFormPage(page);
    csrfToken = await getCsrfToken(page);
    expect(csrfToken).toBeTruthy();
    const csrfHeaders = { "X-CSRF-Token": csrfToken };

    await list.goto();
    await list.expectLoaded();

    await list.clickAdd();
    await form.expectOpen();
    await form.selectPluginType("Generic ASTM");
    await form.selectDefaultConfig("GeneXpert");

    await form.fillName(analyzerName);
    await expect(form.identifierPatternInput).toHaveValue(/GENEXPERT/i, {
      timeout: LONG_TIMEOUT,
    });

    await form.save();
    await form.expectSuccessNotification();

    analyzerId = await findAnalyzerIdByName(request, analyzerName);

    await list.goto();
    await list.expectLoaded();
    await list.search(analyzerName);
    await expect(list.getRow(analyzerId)).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(list.getQcReadinessBadge(analyzerId)).toContainText(
      "QC setup required",
    );

    await list.openOverflowMenu(analyzerId);
    await list.clickAction(analyzerId, "mappings");
    await expect(page).toHaveURL(
      new RegExp(`/analyzers/${analyzerId}/mappings`),
    );
    await expect(page.locator('[data-testid="field-mapping"]')).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
    await expect(
      page.locator('[data-testid="result-value-mappings-panel"]'),
    ).toBeVisible();

    await list.goto();
    await list.expectLoaded();
    await list.search(analyzerName);
    await list.openOverflowMenu(analyzerId);
    await list.clickAction(analyzerId, "qc-rules");
    await expect(page.locator('[data-testid="qc-rule-page"]')).toBeVisible({
      timeout: UI_TIMEOUT,
    });

    const ruleResponse = await request.post(
      `/api/OpenELIS-Global/rest/analyzer/analyzers/${analyzerId}/qc-rules`,
      {
        data: {
          ruleType: "SPECIMEN_ID_PREFIX",
          operand: "QC",
          isActive: true,
          displayOrder: 1,
        },
        headers: csrfHeaders,
      },
    );
    expect(ruleResponse.ok(), await ruleResponse.text()).toBeTruthy();

    await list.goto();
    await list.expectLoaded();
    await list.search(analyzerName);
    await list.openOverflowMenu(analyzerId);
    await list.clickAction(analyzerId, "control-lots");
    await expect(page.locator('[data-testid="control-lot-setup"]')).toBeVisible(
      { timeout: UI_TIMEOUT },
    );
    await expect(
      page.locator('[data-testid="control-lot-analyzer-dropdown"]'),
    ).toContainText(analyzerName);

    const testId = await firstDisplayTestId(request);
    const timestamp = new Date().toISOString();
    const expiration = new Date(
      Date.now() + 365 * 24 * 60 * 60 * 1000,
    ).toISOString();
    const lotResponse = await request.post(
      "/api/OpenELIS-Global/rest/qc/controlLot",
      {
        data: {
          productName: "Playwright QC material",
          lotNumber: `PW-QC-${Date.now()}`,
          controlLevel: "LOW",
          testId,
          instrumentId: analyzerId,
          calculationMethod: "MANUFACTURER_FIXED",
          initialRunsCount: 20,
          manufacturerMean: 100,
          manufacturerStdDev: 5,
          activationDate: timestamp,
          expirationDate: expiration,
          status: "ACTIVE",
        },
        headers: csrfHeaders,
      },
    );
    expect(lotResponse.ok(), await lotResponse.text()).toBeTruthy();
    const lot = await lotResponse.json();
    controlLotId = String(lot.id || "");
    expect(controlLotId).toBeTruthy();

    await list.goto();
    await list.expectLoaded();
    await list.search(analyzerName);
    await expect(list.getQcReadinessBadge(analyzerId)).toContainText(
      "QC ready",
      { timeout: LONG_TIMEOUT },
    );
  });
});
