import { expect, Locator, Page } from "@playwright/test";
import { test } from "../../../helpers/test-base";
import { AnalyzerFormPage } from "../../../fixtures/analyzer-form";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import { LONG_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

const GENEXPERT_PROFILE_ROW = "profile-row-astm-genexpert-astm";

async function selectCarbonDropdownItem(
  dropdown: Locator,
  text: string,
): Promise<void> {
  const trigger = dropdown.locator(
    'button[role="combobox"], .cds--list-box__field',
  );
  await expect(trigger).toBeEnabled({ timeout: UI_TIMEOUT });
  await trigger.click();
  const listbox = dropdown.getByRole("listbox");
  await expect(listbox).toBeVisible({ timeout: UI_TIMEOUT });
  const option = listbox.getByRole("option", { name: text }).first();
  await expect(option).toBeVisible({ timeout: UI_TIMEOUT });
  await option.click();
  await expect(listbox).not.toBeVisible({ timeout: UI_TIMEOUT });
}

async function selectFirstCarbonDropdownItem(dropdown: Locator): Promise<void> {
  const trigger = dropdown.locator(
    'button[role="combobox"], .cds--list-box__field',
  );
  await expect(trigger).toBeEnabled({ timeout: UI_TIMEOUT });
  await trigger.click();
  const listbox = dropdown.getByRole("listbox");
  await expect(listbox).toBeVisible({ timeout: UI_TIMEOUT });
  const option = listbox.getByRole("option").first();
  await expect(option).toBeVisible({ timeout: UI_TIMEOUT });
  await option.click();
  await expect(listbox).not.toBeVisible({ timeout: UI_TIMEOUT });
}

async function findAnalyzerIdFromVisibleRow(
  page: Page,
  analyzerName: string,
): Promise<string> {
  const row = page.locator("tbody tr", { hasText: analyzerName }).first();
  await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });
  const testId = await row.getAttribute("data-testid");
  expect(testId).toMatch(/^analyzer-row-/);
  return String(testId).replace("analyzer-row-", "");
}

async function openAnalyzerInList(
  page: Page,
  analyzerName: string,
): Promise<{ list: AnalyzerListPage; analyzerId: string }> {
  const list = new AnalyzerListPage(page);
  await list.goto();
  await list.expectLoaded();
  await list.search(analyzerName);
  const analyzerId = await findAnalyzerIdFromVisibleRow(page, analyzerName);
  return { list, analyzerId };
}

test.describe("OGC-1054 Analyzer QC/config MVP", () => {
  test("lab user verifies a profile, creates an analyzer, reviews mappings, and completes QC setup", async ({
    page,
  }, testInfo) => {
    test.setTimeout(180_000);
    const demo = createDemoPresentation(page, testInfo);
    const analyzerName = `MVP GeneXpert QC ${Date.now()}`;

    await test.step("Title", async () => {
      await demo.title(
        "Analyzer QC and Configuration MVP",
        "Profile-driven setup, deterministic mapping review, and analyzer QC readiness",
      );
    });

    await test.step("Verify shipped analyzer profile", async () => {
      await page.goto("/analyzers/types", { waitUntil: "domcontentloaded" });
      await demo.scene("Analyzer Type/Profile Verification");
      const profileRow = page.getByTestId(GENEXPERT_PROFILE_ROW);
      await expect(profileRow).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(
        page.getByTestId("profile-test-mapping-count-astm-genexpert-astm"),
      ).toBeVisible();
      await expect(
        page.getByTestId("profile-qc-rule-count-astm-genexpert-astm"),
      ).toBeVisible();
      await demo.evidence("ogc-1054-profile-verification");
      await demo.pause(1_500);

      await page.getByTestId("profile-setup-astm-genexpert-astm").click();
      await expect(page.getByTestId("analyzer-form")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
    });

    await test.step("Create analyzer from selected profile", async () => {
      await demo.scene("Guided Analyzer Setup");
      const form = new AnalyzerFormPage(page);
      await form.expectOpen();
      await expect(page.getByTestId("analyzer-inline-setup")).toBeVisible();
      await expect(page.getByTestId("analyzers-table")).toBeVisible();
      await expect(form.defaultConfigDropdown).toContainText(/GeneXpert/i);
      await expect(form.pluginTypeDropdown).not.toBeVisible();
      await expect(form.identifierPatternInput).not.toBeVisible();
      await expect(
        page.getByTestId("analyzer-form-profile-summary"),
      ).toBeVisible();

      await form.fillName(analyzerName);
      await demo.evidence("ogc-1054-inline-setup");
      await demo.pause(1_000);
      await form.save();
      await form.expectSuccessNotification();
      await expect(form.modal).not.toBeVisible({ timeout: LONG_TIMEOUT });
    });

    let analyzerId = "";

    await test.step("Confirm setup readiness on analyzer list", async () => {
      const opened = await openAnalyzerInList(page, analyzerName);
      analyzerId = opened.analyzerId;
      await demo.scene("Analyzer Setup Surface");
      await expect(opened.list.getRow(analyzerId)).toBeVisible();
      await expect(opened.list.getQcReadinessBadge(analyzerId)).toContainText(
        "QC setup required",
      );
      await demo.evidence("ogc-1054-qc-required");
      await demo.pause(1_000);
    });

    await test.step("Review profile-applied mapping workflow", async () => {
      const { list } = await openAnalyzerInList(page, analyzerName);
      await list.openOverflowMenu(analyzerId);
      await list.clickAction(analyzerId, "mappings");
      await demo.scene("Mapping Review");
      await expect(page).toHaveURL(
        new RegExp(`/analyzers/${analyzerId}/mappings`),
      );
      await expect(page.getByTestId("field-mapping")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await expect(page.getByTestId("field-mapping-stats")).toBeVisible();
      await expect(
        page.getByTestId("profile-applied-mappings-panel"),
      ).toContainText("MTB");
      await expect(
        page.getByTestId("plugin-config-snapshot"),
      ).not.toBeVisible();
      await expect(
        page.getByTestId("result-value-mappings-panel"),
      ).toContainText("DETECTED");
      await demo.evidence("ogc-1054-mapping-review");
      await demo.pause(1_500);
    });

    await test.step("Create analyzer QC rule through the UI", async () => {
      const { list } = await openAnalyzerInList(page, analyzerName);
      await list.openOverflowMenu(analyzerId);
      await list.clickAction(analyzerId, "qc-rules");
      await demo.scene("Analyzer QC Rule Setup");
      await expect(page.getByTestId("qc-rule-page")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await page.getByTestId("qc-rule-add-btn").click();
      await selectCarbonDropdownItem(
        page.locator("#qc-rule-type-0"),
        "Specimen ID Prefix",
      );
      await page.getByTestId("qc-rule-operand-0").fill("QC");
      await demo.evidence("ogc-1054-qc-rule");
      await demo.pause(1_000);
      await page.getByTestId("qc-rule-save-btn").click();
      await expect(page).toHaveURL(/\/analyzers/, { timeout: LONG_TIMEOUT });
    });

    await test.step("Create active control lot through the UI", async () => {
      const { list } = await openAnalyzerInList(page, analyzerName);
      await list.openOverflowMenu(analyzerId);
      await list.clickAction(analyzerId, "control-lots");
      await demo.scene("Analyzer Control Lot Setup");
      await expect(page.getByTestId("control-lot-setup")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await expect(
        page.getByTestId("control-lot-analyzer-dropdown"),
      ).toContainText(analyzerName, { timeout: LONG_TIMEOUT });

      const controlLotNumber = `MVP-QC-${Date.now()}`;
      await page.getByTestId("control-lot-number-input").fill(controlLotNumber);
      await page
        .getByTestId("control-lot-material-input")
        .fill("MVP QC material");
      await selectCarbonDropdownItem(
        page.getByTestId("control-lot-level-dropdown"),
        "Low",
      );
      const expirationInput = page.getByTestId("control-lot-expiration-input");
      await expirationInput.fill("06/28/2027");
      await expirationInput.press("Escape");
      await expect(page.locator(".flatpickr-calendar.open")).not.toBeVisible({
        timeout: UI_TIMEOUT,
      });
      await selectFirstCarbonDropdownItem(
        page.getByTestId("control-lot-test-dropdown"),
      );
      await page.getByTestId("control-lot-statistics-config-button").click();
      await expect(page.getByTestId("statistics-config-modal")).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      await page.getByTestId("statistics-mean-input").fill("100");
      await page.getByTestId("statistics-sd-input").fill("5");
      await page.getByTestId("statistics-config-save-button").click();
      await expect(page.getByTestId("statistics-config-modal")).not.toBeVisible(
        { timeout: UI_TIMEOUT },
      );
      await demo.evidence("ogc-1054-control-lot");
      await demo.pause(1_000);
      await page.getByTestId("control-lot-submit-button").click();
      await expect(page).toHaveURL(/\/analyzers\/qc\/control-lots$/, {
        timeout: LONG_TIMEOUT,
      });
      await expect(page.getByTestId("control-lot-list")).toContainText(
        controlLotNumber,
        { timeout: LONG_TIMEOUT },
      );
    });

    await test.step("Confirm analyzer QC readiness", async () => {
      const { list } = await openAnalyzerInList(page, analyzerName);
      await demo.scene("Analyzer QC Ready");
      await expect(list.getQcReadinessBadge(analyzerId)).toContainText(
        "QC ready",
        { timeout: LONG_TIMEOUT },
      );
      await demo.evidence("ogc-1054-qc-ready");
      await demo.pause(2_000);
    });
  });
});
