import { expect, Locator, Page } from "@playwright/test";
import { test } from "../../../helpers/test-base";
import { AnalyzerFormPage } from "../../../fixtures/analyzer-form";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import { LONG_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

const GENEXPERT_HL7_PROFILE_ID = "hl7/genexpert-hl7";
const GENEXPERT_HL7_PROFILE_DOM_ID = "hl7-genexpert-hl7";
const SEEDED_PENDING_ANALYZER = "Cepheid GeneXpert (ASTM Mode)";

async function selectCarbonItem(
  trigger: Locator,
  name: string | RegExp,
): Promise<void> {
  await expect(trigger).toBeEnabled({ timeout: UI_TIMEOUT });
  await trigger.click();
  const option = trigger
    .page()
    .getByRole("option", { name, exact: typeof name === "string" })
    .first();
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

function currentApplicationRoute(page: Page) {
  const url = new URL(page.url());
  return `${url.pathname}${url.search}`;
}

async function expectApplicationRoute(
  page: Page,
  expectedRoute: string,
): Promise<void> {
  await expect
    .poll(() => currentApplicationRoute(page), { timeout: LONG_TIMEOUT })
    .toBe(expectedRoute);
}

test.describe("OGC-1054 analyzer QC/config acceptance", () => {
  test("a lab user completes the canonical profile, mapping, connection, QC, and review story", async ({
    page,
  }, testInfo) => {
    test.setTimeout(300_000);
    const demo = createDemoPresentation(page, testInfo);
    const analyzerName = `UAT GeneXpert HL7 ${Date.now()}`;
    let analyzerId = "";
    let verifyUrl = "";

    await demo.title(
      "Analyzer QC and Configuration MVP",
      "A complete lab-facing setup and verification story",
    );

    await test.step("legacy route redirects to canonical Instrument setup", async () => {
      await page.goto("/analyzers/new", { waitUntil: "domcontentloaded" });
      await expect(page).toHaveURL(
        /\/analyzers\?add=1&step=instrument&returnTo=%2Fanalyzers$/,
        { timeout: LONG_TIMEOUT },
      );
      await expect(page.getByTestId("analyzer-inline-setup")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
    });

    await test.step("AN-QC-001 inspect a shipped profile", async () => {
      await demo.step(1, "Inspect a shipped analyzer profile");
      await page.goto("/analyzers/types?protocol=HL7", {
        waitUntil: "domcontentloaded",
      });
      const profileRow = page.getByTestId(
        `profile-row-${GENEXPERT_HL7_PROFILE_DOM_ID}`,
      );
      await expect(profileRow).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(profileRow).toContainText("Cepheid GeneXpert");
      await expect(profileRow).toContainText("HL7");
      await expect(
        page.getByTestId(
          `profile-test-mapping-count-${GENEXPERT_HL7_PROFILE_DOM_ID}`,
        ),
      ).toHaveText("4");
      await expect(
        page.getByTestId(
          `profile-qc-rule-count-${GENEXPERT_HL7_PROFILE_DOM_ID}`,
        ),
      ).toHaveText("0");
      await demo.evidence("an-qc-001-profile");

      await page
        .getByTestId(`profile-setup-${GENEXPERT_HL7_PROFILE_DOM_ID}`)
        .click();
      await expect(page).toHaveURL(
        new RegExp(
          `/analyzers\\?add=1&step=instrument&profile=${encodeURIComponent(
            GENEXPERT_HL7_PROFILE_ID,
          )}`,
        ),
        { timeout: LONG_TIMEOUT },
      );
    });

    await test.step("AN-QC-002 create one analyzer and enter Verify", async () => {
      await demo.step(2, "Create and assign the analyzer");
      const form = new AnalyzerFormPage(page);
      await form.expectOpen();
      await expect(form.defaultConfigDropdown).toContainText(
        "Cepheid GeneXpert",
      );
      await expect(form.profileSummary).toContainText("HL7");
      await expect(form.pluginTypeDropdown).not.toBeVisible();
      await expect(form.connectionFields).not.toBeVisible();

      await form.fillName(analyzerName);
      await selectCarbonItem(
        page.getByRole("combobox", { name: "Lab units" }),
        "Molecular Biology",
      );
      await demo.evidence("an-qc-002-instrument");
      await form.save();

      await expect(page).toHaveURL(
        /\/analyzers\/\d+\/mappings\?setup=1&step=verify/,
        { timeout: LONG_TIMEOUT },
      );
      analyzerId = new URL(page.url()).pathname.split("/")[2];
      verifyUrl = currentApplicationRoute(page);
      await expect(page.getByTestId("field-mapping")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
    });

    await test.step("AN-QC-003 review mappings and bookmarked step state", async () => {
      await demo.step(3, "Review mappings and readiness blockers");
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

      await page.reload({ waitUntil: "domcontentloaded" });
      await expectApplicationRoute(page, verifyUrl);
      await expect(profileMappings).toContainText("MTB", {
        timeout: LONG_TIMEOUT,
      });
      await page.goBack({ waitUntil: "domcontentloaded" });
      await expect(page.getByTestId("analyzer-inline-setup")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await page.goForward({ waitUntil: "domcontentloaded" });
      await expectApplicationRoute(page, verifyUrl);
      await expect(page.getByTestId("field-mapping")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
    });

    await test.step("AN-QC-004 visibly test the saved connection", async () => {
      await demo.step(4, "Configure and test the analyzer connection");
      await page.getByTestId("analyzer-setup-verify-continue").click();
      await expect(page).toHaveURL(
        new RegExp(`/analyzers/${analyzerId}/edit\\?setup=1&step=connect`),
        { timeout: LONG_TIMEOUT },
      );

      const form = new AnalyzerFormPage(page);
      await form.fillIpAddress("172.21.1.100");
      await form.fillPort("5380");
      await page.getByTestId("analyzer-form-test-connection-button").click();
      const modal = page.getByTestId("test-connection-modal");
      await expect(modal).toBeVisible();
      await page.getByTestId("test-connection-test-button").click();
      await expect(
        page
          .getByTestId("test-connection-success")
          .or(page.getByTestId("test-connection-error")),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(page.getByTestId("test-connection-logs")).toBeVisible();
      await demo.evidence("an-qc-004-connection-result");
      await page.getByTestId("test-connection-close-button").click();
      await form.save();

      await expect(page).toHaveURL(
        new RegExp(`/analyzers/${analyzerId}/review\\?setup=1&step=review`),
        { timeout: LONG_TIMEOUT },
      );
      const blockedReview = page.getByTestId("analyzer-setup-review");
      await expect(blockedReview).toContainText(
        "Setup is not ready for activation",
      );
      await demo.evidence("an-qc-004-blocked-review");
      await page.getByTestId("analyzer-review-back").click();
      await page.getByTestId("analyzer-form-cancel-button").click();
      await expectApplicationRoute(page, verifyUrl);
    });

    await test.step("AN-QC-005 resolve a pending qualitative result", async () => {
      await demo.step(
        5,
        "Resolve a seeded observed value with a catalog option",
      );
      const pendingAnalyzer = await openAnalyzer(page, SEEDED_PENDING_ANALYZER);
      await pendingAnalyzer.list.openOverflowMenu(pendingAnalyzer.id);
      await pendingAnalyzer.list.clickAction(pendingAnalyzer.id, "mappings");

      const pendingTable = page.getByTestId("pending-result-values-table");
      await expect(pendingTable).toBeVisible({ timeout: LONG_TIMEOUT });
      const pendingRow = pendingTable.locator("tbody tr", {
        hasText: "MTB TRACE DETECTED",
      });
      await expect(pendingRow).toContainText("PENDING");
      await selectCarbonItem(
        pendingRow.getByRole("combobox", {
          name: "OpenELIS Result Option",
        }),
        /^Detected$/i,
      );
      await pendingRow
        .getByTestId("result-value-resolve-uat-mtb-trace")
        .click();
      await expect(page.getByTestId("pending-result-values-empty")).toBeVisible(
        {
          timeout: LONG_TIMEOUT,
        },
      );
      const configuredRow = page
        .getByTestId("result-value-mappings-table")
        .locator("tbody tr", { hasText: "MTB TRACE DETECTED" });
      await expect(configuredRow).toContainText(/Detected/i);
      await expect(configuredRow).toContainText("BOUND");
      await page.reload({ waitUntil: "domcontentloaded" });
      await expect(configuredRow).toContainText("BOUND", {
        timeout: LONG_TIMEOUT,
      });
      await demo.evidence("an-qc-005-pending-result-resolved");
      await page.goto(verifyUrl, { waitUntil: "domcontentloaded" });
    });

    await test.step("AN-QC-006 add a QC rule and control lot", async () => {
      await demo.step(6, "Complete analyzer QC setup");
      const verification = page.getByTestId("setup-verification-panel");
      await expect(verification).toBeVisible({ timeout: LONG_TIMEOUT });
      await page.getByTestId("analyzer-setup-manage-qc-rules").click();

      await expect(page.getByTestId("qc-rule-page")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await page.getByTestId("qc-rule-add-btn").click();
      await selectCarbonItem(
        page.getByRole("combobox", { name: "Rule Type" }),
        "Specimen ID Prefix",
      );
      await page.getByTestId("qc-rule-operand-0").fill("QC");
      await demo.evidence("an-qc-006-qc-rule");
      await page.getByTestId("qc-rule-save-btn").click();
      await expectApplicationRoute(page, verifyUrl);

      await page.getByTestId("analyzer-setup-manage-control-lots").click();
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
        page.getByRole("combobox", { name: "Control Level" }),
        "Low",
      );
      const expiration = page.getByTestId("control-lot-expiration-input");
      await expiration.fill("12/31/2027");
      await expiration.press("Escape");
      await selectCarbonItem(
        page.getByRole("combobox", { name: "Test" }),
        "Xpert MTB/RIF(Sputum)",
      );
      await page.getByTestId("control-lot-statistics-config-button").click();
      await page.getByTestId("statistics-mean-input").fill("100");
      await page.getByTestId("statistics-sd-input").fill("5");
      await page.getByTestId("statistics-config-save-button").click();
      await demo.evidence("an-qc-006-control-lot");
      await page.getByTestId("control-lot-submit-button").click();
      await expectApplicationRoute(page, verifyUrl);
    });

    await test.step("AN-QC-007 verify the current setup", async () => {
      await demo.step(7, "Verify the completed mappings and QC");
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

    await test.step("AN-QC-008 review and finish the configuration", async () => {
      await demo.step(8, "Review the completed analyzer");
      await page.getByTestId("analyzer-setup-verify-continue").click();
      const form = new AnalyzerFormPage(page);
      await expect(form.ipAddressInput).toHaveValue("172.21.1.100");
      await expect(form.portInput).toHaveValue("5380");
      await form.save();

      const review = page.getByTestId("analyzer-setup-review");
      await expect(review).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(review).toContainText("Ready for activation");
      await expect(review).toContainText(analyzerName);
      await expect(review).toContainText("172.21.1.100:5380");
      await expect(review).toContainText("Verified by");
      await demo.evidence("an-qc-008-completed-review");
      await page.getByTestId("analyzer-review-finish").click();
      await expect(page).toHaveURL(/\/analyzers\/types\?protocol=HL7$/, {
        timeout: LONG_TIMEOUT,
      });
    });
  });
});
