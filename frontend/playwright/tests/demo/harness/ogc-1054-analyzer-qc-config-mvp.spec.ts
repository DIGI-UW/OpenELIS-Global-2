import { expect, Locator, Page } from "@playwright/test";
import { test } from "../../../helpers/test-base";
import { AnalyzerFormPage } from "../../../fixtures/analyzer-form";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import { LONG_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

const GENEXPERT_HL7_PROFILE = "hl7-genexpert-hl7";
const SEEDED_PENDING_ANALYZER = "Cepheid GeneXpert (ASTM Mode)";

async function selectCarbonItem(scope: Locator, name: string): Promise<void> {
  const trigger = scope
    .locator(
      'input[role="combobox"], button[role="combobox"], .cds--list-box__field',
    )
    .first();
  await expect(trigger).toBeEnabled({ timeout: UI_TIMEOUT });
  await trigger.click();
  const option = scope.getByRole("option", { name, exact: true }).first();
  await expect(option).toBeVisible({ timeout: UI_TIMEOUT });
  await option.click();
}

async function selectFirstCarbonItem(scope: Locator): Promise<void> {
  const trigger = scope
    .locator(
      'input[role="combobox"], button[role="combobox"], .cds--list-box__field',
    )
    .first();
  await expect(trigger).toBeEnabled({ timeout: UI_TIMEOUT });
  await trigger.click();
  const option = scope.getByRole("option").first();
  await expect(option).toBeVisible({ timeout: UI_TIMEOUT });
  await option.click();
}

async function findAnalyzerId(page: Page, analyzerName: string) {
  const row = page.locator("tbody tr", { hasText: analyzerName }).first();
  await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });
  const testId = await row.getAttribute("data-testid");
  expect(testId).toMatch(/^analyzer-row-/);
  return String(testId).replace("analyzer-row-", "");
}

async function openAnalyzer(
  page: Page,
  analyzerName: string,
): Promise<{ list: AnalyzerListPage; id: string }> {
  const list = new AnalyzerListPage(page);
  await list.goto();
  await list.expectLoaded();
  await list.search(analyzerName);
  return { list, id: await findAnalyzerId(page, analyzerName) };
}

test.describe("OGC-1054 analyzer QC/config acceptance", () => {
  test("a lab user completes profile setup, mapping review, QC readiness, and verification", async ({
    page,
  }, testInfo) => {
    test.setTimeout(300_000);
    const demo = createDemoPresentation(page, testInfo);
    const analyzerName = `UAT GeneXpert HL7 ${Date.now()}`;
    let analyzerId = "";

    await demo.title(
      "Analyzer QC and Configuration MVP",
      "A complete lab-facing setup and verification story",
    );

    await test.step("AN-QC-001 inspect a shipped profile", async () => {
      await demo.step(1, "Inspect a shipped analyzer profile");
      await page.goto("/analyzers/types", { waitUntil: "domcontentloaded" });
      const profileRow = page.getByTestId(
        `profile-row-${GENEXPERT_HL7_PROFILE}`,
      );
      await expect(profileRow).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(profileRow).toContainText("Cepheid GeneXpert");
      await expect(profileRow).toContainText("HL7");
      await expect(
        page.getByTestId(`profile-test-mapping-count-${GENEXPERT_HL7_PROFILE}`),
      ).toHaveText("4");
      await expect(
        page.getByTestId(`profile-qc-rule-count-${GENEXPERT_HL7_PROFILE}`),
      ).toHaveText("0");
      await demo.evidence("an-qc-001-profile");

      await page.getByTestId(`profile-setup-${GENEXPERT_HL7_PROFILE}`).click();
      await expect(page.getByTestId("analyzer-inline-setup")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
    });

    await test.step("AN-QC-002 create an analyzer through inline setup", async () => {
      await demo.step(2, "Create and assign the analyzer");
      const form = new AnalyzerFormPage(page);
      await form.expectOpen();
      await expect(form.defaultConfigDropdown).toContainText(
        "Cepheid GeneXpert",
      );
      await expect(form.profileSummary).toContainText("HL7");
      await expect(form.pluginTypeDropdown).not.toBeVisible();

      await form.fillName(analyzerName);
      await selectCarbonItem(
        page.getByTestId("analyzer-form-lab-units"),
        "Molecular Biology",
      );
      await form.fillIpAddress("172.21.1.100");
      await form.fillPort("5380");
      await demo.evidence("an-qc-002-inline-setup");
      await form.save();
      await form.expectSuccessNotification();
      await expect(form.modal).not.toBeVisible({ timeout: LONG_TIMEOUT });

      const opened = await openAnalyzer(page, analyzerName);
      analyzerId = opened.id;
      await expect(opened.list.getStatusBadge(analyzerId)).toContainText(
        "Setup",
      );
      await expect(opened.list.getQcReadinessBadge(analyzerId)).toContainText(
        "Setup required",
      );
    });

    await test.step("AN-QC-003 review deterministic mappings and blockers", async () => {
      await demo.step(3, "Review mappings and readiness blockers");
      const { list } = await openAnalyzer(page, analyzerName);
      await list.openOverflowMenu(analyzerId);
      await list.clickAction(analyzerId, "mappings");

      await expect(page.getByTestId("field-mapping")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      const profileMappings = page.getByTestId(
        "profile-applied-mappings-panel",
      );
      await expect(profileMappings).toContainText("MTB");
      await expect(profileMappings).toContainText("RIF");
      await expect(profileMappings).toContainText("COVID19");
      await expect(page.getByTestId("pending-codes-empty")).toBeVisible();
      await expect(
        page.getByTestId("pending-result-values-empty"),
      ).toBeVisible();

      const verification = page.getByTestId("setup-verification-panel");
      await expect(verification).toContainText("Setup incomplete");
      await expect(verification).toContainText(
        "An active QC rule is required.",
      );
      await expect(verification).toContainText(
        "An active control lot is required.",
      );
      await demo.evidence("an-qc-003-mapping-review");
    });

    await test.step("AN-QC-004 receive a visible connection test result", async () => {
      await demo.step(4, "Test the saved analyzer connection");
      const { list } = await openAnalyzer(page, analyzerName);
      await list.openOverflowMenu(analyzerId);
      await list.clickAction(analyzerId, "test-connection");

      const modal = page.getByTestId("test-connection-modal");
      await expect(modal).toBeVisible();
      await expect(modal).toContainText("172.21.1.100");
      await expect(modal).toContainText("5380");
      await page.getByTestId("test-connection-test-button").click();
      await expect(
        page
          .getByTestId("test-connection-success")
          .or(page.getByTestId("test-connection-error")),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(page.getByTestId("test-connection-logs")).toBeVisible();
      await demo.evidence("an-qc-004-connection-result");
      await page.getByTestId("test-connection-close-button").click();
    });

    await test.step("AN-QC-005 resolve a pending qualitative result from the catalog", async () => {
      await demo.step(5, "Resolve an instrument value with a catalog option");
      const pendingAnalyzer = await openAnalyzer(page, SEEDED_PENDING_ANALYZER);
      await pendingAnalyzer.list.openOverflowMenu(pendingAnalyzer.id);
      await pendingAnalyzer.list.clickAction(pendingAnalyzer.id, "mappings");

      const pendingTable = page.getByTestId("pending-result-values-table");
      await expect(pendingTable).toBeVisible({ timeout: LONG_TIMEOUT });
      const pendingRow = pendingTable.locator("tbody tr", {
        hasText: "MTB TRACE DETECTED",
      });
      await expect(pendingRow).toContainText("PENDING");
      await selectCarbonItem(pendingRow, "Detected");
      await pendingRow
        .getByTestId("result-value-resolve-uat-mtb-trace")
        .click();
      await expect(pendingRow).toContainText("MAPPED", {
        timeout: LONG_TIMEOUT,
      });
      await expect(pendingRow).toContainText("Detected");

      await page.reload({ waitUntil: "domcontentloaded" });
      const persistedRow = page
        .getByTestId("pending-result-values-table")
        .locator("tbody tr", { hasText: "MTB TRACE DETECTED" });
      await expect(persistedRow).toContainText("MAPPED", {
        timeout: LONG_TIMEOUT,
      });
      await expect(persistedRow).toContainText("Detected");
      await demo.evidence("an-qc-005-pending-result-resolved");
    });

    await test.step("AN-QC-006 add an active QC rule", async () => {
      await demo.step(6, "Add the analyzer QC rule");
      const { list } = await openAnalyzer(page, analyzerName);
      await list.openOverflowMenu(analyzerId);
      await list.clickAction(analyzerId, "qc-rules");

      await expect(page.getByTestId("qc-rule-page")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await page.getByTestId("qc-rule-add-btn").click();
      await selectCarbonItem(
        page.locator("#qc-rule-type-0"),
        "Specimen ID Prefix",
      );
      await page.getByTestId("qc-rule-operand-0").fill("QC");
      await demo.evidence("an-qc-006-qc-rule");
      await page.getByTestId("qc-rule-save-btn").click();
      await expect(page).toHaveURL(/\/analyzers$/, {
        timeout: LONG_TIMEOUT,
      });
    });

    await test.step("AN-QC-006 add an active control lot", async () => {
      await demo.step(7, "Add an active control lot");
      const { list } = await openAnalyzer(page, analyzerName);
      await list.openOverflowMenu(analyzerId);
      await list.clickAction(analyzerId, "control-lots");

      await expect(page.getByTestId("control-lot-setup")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await expect(
        page.getByTestId("control-lot-analyzer-dropdown"),
      ).toContainText(analyzerName);
      await page
        .getByTestId("control-lot-number-input")
        .fill(`UAT-QC-${Date.now()}`);
      await page
        .getByTestId("control-lot-material-input")
        .fill("GeneXpert positive control");
      await selectCarbonItem(
        page.getByTestId("control-lot-level-dropdown"),
        "Low",
      );
      const expiration = page.getByTestId("control-lot-expiration-input");
      await expiration.fill("12/31/2027");
      await expiration.press("Escape");
      await selectFirstCarbonItem(
        page.getByTestId("control-lot-test-dropdown"),
      );

      await page.getByTestId("control-lot-statistics-config-button").click();
      await expect(page.getByTestId("statistics-config-modal")).toBeVisible();
      await page.getByTestId("statistics-mean-input").fill("100");
      await page.getByTestId("statistics-sd-input").fill("5");
      await page.getByTestId("statistics-config-save-button").click();
      await expect(page.getByTestId("statistics-config-modal")).not.toBeVisible(
        {
          timeout: UI_TIMEOUT,
        },
      );
      await demo.evidence("an-qc-006-control-lot");
      await page.getByTestId("control-lot-submit-button").click();
      await expect(page).toHaveURL(/\/analyzers\/qc\/control-lots$/, {
        timeout: LONG_TIMEOUT,
      });
    });

    await test.step("AN-QC-007 verify the current setup and reach ACTIVE", async () => {
      await demo.step(8, "Verify the completed setup");
      const { list } = await openAnalyzer(page, analyzerName);
      await list.openOverflowMenu(analyzerId);
      await list.clickAction(analyzerId, "mappings");

      const verification = page.getByTestId("setup-verification-panel");
      await expect(verification).toContainText("Ready for verification", {
        timeout: LONG_TIMEOUT,
      });
      const verifyButton = verification.getByRole("button", {
        name: "Verify current setup",
      });
      await expect(verifyButton).toBeEnabled();
      await verifyButton.click();
      await expect(verification).toContainText("Currently verified", {
        timeout: LONG_TIMEOUT,
      });
      await expect(page.getByTestId("setup-verification-audit")).toContainText(
        "Verified by",
      );
      await demo.evidence("an-qc-007-current-verification");
    });

    await test.step("AN-QC-008 review the completed lab configuration", async () => {
      const completed = await openAnalyzer(page, analyzerName);
      await expect(completed.list.getStatusBadge(analyzerId)).toContainText(
        "Active",
        { timeout: LONG_TIMEOUT },
      );
      await expect(
        completed.list.getQcReadinessBadge(analyzerId),
      ).toContainText("Setup verified");
      await demo.evidence("an-qc-008-completed-configuration");
    });
  });
});
