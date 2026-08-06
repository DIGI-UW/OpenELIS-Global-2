import { test, expect } from "../../../helpers/test-base";
import type { Page, TestInfo } from "@playwright/test";
import { seedMicrobiologyMvpCase } from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

const accordionButton = (page: Page, name: string) =>
  page
    .getByTestId("microbiology-case-view")
    .getByRole("button", { name, exact: true });

const attachScreenshot = async (
  page: Page,
  testInfo: TestInfo,
  name: string,
) => {
  await page.evaluate(() => window.scrollTo(0, 0));
  await testInfo.attach(name, {
    body: await page.screenshot({ fullPage: true }),
    contentType: "image/png",
  });
};

test.describe("Microbiology reagent and card lot traceability", () => {
  test("blocks ineligible lots and records exact culture and AST provenance", async ({
    page,
  }, testInfo) => {
    test.setTimeout(120_000);
    const seeded = await seedMicrobiologyMvpCase(page);

    await test.step("Choose the recommended eligible culture lot", async () => {
      await page.goto(`/Microbiology/cases/${seeded.caseId}?section=setup`, {
        waitUntil: "domcontentloaded",
      });
      await expect(
        page.getByRole("heading", { name: "Microbiology case" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });

      const setup = page.getByRole("region", { name: "Inoculation" });
      await setup.getByRole("button", { name: "Start inoculation" }).click();
      await setup
        .getByRole("textbox", { name: "Bottle or plate ID" })
        .fill("UAT-MICRO-BOTTLE-R1");
      await setup
        .getByRole("textbox", { name: "Media or bottle" })
        .fill("Blood culture bottle");

      const expired = setup.getByRole("radio", {
        name: /UAT-MICRO-MEDIA-EXPIRED/,
      });
      const recommended = setup.getByRole("radio", {
        name: /UAT-MICRO-MEDIA-FEFO/,
      });
      await expect(expired).toBeDisabled();
      await expect(setup.getByText("Blocked: Expired")).toBeVisible();
      await expect(setup.getByText("FEFO - use first")).toHaveCount(2);
      await expect(setup.getByText("QC passed").first()).toBeVisible();
      await expect(setup.getByText("Primary", { exact: true })).toBeVisible();
      await expect(
        setup.getByRole("button", {
          name: /Lots are ordered by earliest expiry/,
        }),
      ).toBeVisible();
      const scanner = setup.getByRole("searchbox", {
        name: "Scan or enter lot number",
      });
      await scanner.fill("UAT-MICRO-MEDIA-FEFO");
      await scanner.press("Enter");
      await expect(
        setup.getByText("Selected lot UAT-MICRO-MEDIA-FEFO."),
      ).toBeVisible();
      await expect(recommended).toBeChecked();

      await setup.getByRole("button", { name: "Save media" }).click();
      const usageTable = page.getByRole("table", {
        name: "Recorded lot usage",
      });
      await expect(usageTable).toContainText("UAT-MICRO-MEDIA-FEFO", {
        timeout: LONG_TIMEOUT,
      });
      await expect(usageTable).toContainText("Culture setup");
      await attachScreenshot(page, testInfo, "culture-lot-provenance");
    });

    await test.step("Create the isolate needed for AST", async () => {
      await accordionButton(page, "Isolates").click();
      await page.getByLabel("Gram stain").fill("Gram negative rods");
      await page
        .getByLabel("Colony morphology")
        .fill("Lactose fermenting colonies");
      await page.getByRole("button", { name: "Create isolate" }).click();
      await expect(page.getByText("Identification pending")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await page.getByRole("button", { name: "Identify organism" }).click();
      await page.getByLabel("Organism").selectOption(seeded.organismId);
      await page.getByLabel("ID method").selectOption("MALDI_TOF");
      await page.getByLabel("ID confidence (%)").fill("99.5");
      await page.getByRole("button", { name: "Save identification" }).click();
      await expect(page.getByText("Identified", { exact: true })).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
    });

    await test.step("Choose and retain the exact AST card lot", async () => {
      await accordionButton(page, "Manual AST").click();
      await expect(page).toHaveURL(/section=ast$/);
      const ast = page.getByTestId("microbiology-ast-card");
      const card = ast.getByRole("radio", {
        name: /UAT-MICRO-CARD-FEFO/,
      });
      await expect(card).toBeEnabled({ timeout: LONG_TIMEOUT });
      await expect(ast.getByText("Secondary", { exact: true })).toBeVisible();
      const scanner = ast.getByRole("searchbox", {
        name: "Scan or enter lot number",
      });
      await scanner.fill("UAT-MICRO-CARD-FEFO");
      await scanner.press("Enter");
      await expect(
        ast.getByText("Selected lot UAT-MICRO-CARD-FEFO."),
      ).toBeVisible();
      await expect(card).toBeChecked();
      await ast.getByRole("button", { name: "Start AST run" }).click();

      await expect(
        page.getByTestId("microbiology-ast-run-status"),
      ).toContainText("In Progress", { timeout: LONG_TIMEOUT });
      const usageTable = page.getByRole("table", {
        name: "Recorded lot usage",
      });
      await expect(usageTable).toContainText("UAT-MICRO-CARD-FEFO", {
        timeout: LONG_TIMEOUT,
      });
      await expect(usageTable).toContainText("AST setup");
      await expect(usageTable).toContainText("UAT-MICRO-MEDIA-FEFO");
      await expect(usageTable).toContainText("Culture setup");
      await attachScreenshot(page, testInfo, "ast-card-lot-provenance");
    });
  });
});
