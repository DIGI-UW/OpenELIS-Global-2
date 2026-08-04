import { test, expect } from "../../../helpers/test-base";
import type { Page, TestInfo } from "@playwright/test";
import { seedMicrobiologyMvpCase } from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

const accordionButton = (page: Page, name: string) =>
  page
    .getByTestId("microbiology-case-view")
    .getByRole("button", { name, exact: true });

const chooseCarbonRadio = async (radio: ReturnType<Page["getByRole"]>) => {
  await radio.locator("xpath=..").locator("label").click();
};

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

      const expired = page.getByRole("radio", {
        name: /UAT-MICRO-MEDIA-EXPIRED/,
      });
      const recommended = page.getByRole("radio", {
        name: /UAT-MICRO-MEDIA-FEFO/,
      });
      await expect(expired).toBeDisabled();
      await expect(page.getByText("Blocked: Expired")).toBeVisible();
      await expect(page.getByText("FEFO - use first")).toHaveCount(2);
      await expect(page.getByText("Primary", { exact: true })).toBeVisible();
      await chooseCarbonRadio(recommended);
      await expect(recommended).toBeChecked();

      await page.getByRole("button", { name: "Start inoculation" }).click();
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
      await page.getByLabel("Preliminary organism").fill("Escherichia coli");
      await page.getByRole("button", { name: "Create isolate" }).click();
      await expect(page.getByText(/ISO-1: Escherichia coli/)).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
    });

    await test.step("Choose and retain the exact AST card lot", async () => {
      await accordionButton(page, "Manual AST").click();
      await expect(page).toHaveURL(/section=ast$/);
      const card = page.getByRole("radio", {
        name: /UAT-MICRO-CARD-FEFO/,
      });
      await expect(card).toBeEnabled({ timeout: LONG_TIMEOUT });
      await expect(
        page
          .getByTestId("microbiology-ast-card")
          .getByText("Secondary", { exact: true }),
      ).toBeVisible();
      await chooseCarbonRadio(card);
      await page.getByRole("button", { name: "Start AST run" }).click();

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
      await attachScreenshot(page, testInfo, "ast-card-lot-provenance");
    });
  });
});
