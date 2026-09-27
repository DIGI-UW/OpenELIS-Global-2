import { test, expect } from "../../../helpers/test-base";
import type { Page, TestInfo } from "@playwright/test";
import { seedMicrobiologyMvpCase } from "../../../helpers/seed-microbiology-data";
import {
  createAndIdentifyMicrobiologyIsolate,
  openMicrobiologyCaseSection,
} from "../../../helpers/microbiology-ui";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

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
      await createAndIdentifyMicrobiologyIsolate(page, seeded.organismId!);
    });

    await test.step("Choose and retain the exact AST card lot", async () => {
      await openMicrobiologyCaseSection(page, "Manual AST");
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
