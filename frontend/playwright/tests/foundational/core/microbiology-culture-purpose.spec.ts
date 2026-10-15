import { expect, test } from "../../../helpers/test-base";
import {
  seedMicrobiologyCulturePurposeCase,
  seedMicrobiologyCulturePurposeWhonetPopulation,
} from "../../../helpers/seed-microbiology-data";
import {
  buildWhonetExportQuery,
  expectWhonetExportReady,
} from "../../../helpers/whonet-export";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";
import type { Page, TestInfo } from "@playwright/test";

const metric = (page: Page, label: string) =>
  page.locator(".whonet-export__metric").filter({ hasText: label });

const attachScreenshot = async (
  page: Page,
  testInfo: TestInfo,
  name: string,
  fullPage = false,
) => {
  await testInfo.attach(name, {
    body: await page.screenshot({ animations: "disabled", fullPage }),
    contentType: "image/png",
  });
};

test.describe("OGC-782 R11 culture purpose", () => {
  test("audits a pre-release correction and applies explicit WHONET inclusion", async ({
    page,
  }, testInfo) => {
    test.setTimeout(240_000);
    const editableCase = await seedMicrobiologyCulturePurposeCase(page);
    const population =
      await seedMicrobiologyCulturePurposeWhonetPopulation(page);

    await test.step("Correct culture purpose before final release", async () => {
      await page.goto(
        `/Microbiology/cases/${editableCase.caseId}?section=order-detail`,
        { waitUntil: "commit" },
      );
      const caseView = page.getByTestId("microbiology-case-view");
      await expect(caseView).toBeVisible({ timeout: LONG_TIMEOUT });
      const purpose = caseView.getByLabel("Culture purpose", { exact: true });
      await expect(purpose).toHaveValue("DIAGNOSTIC");
      await purpose.selectOption("SCREENING");
      const saved = page.waitForResponse(
        (response) =>
          response.url().endsWith(`/cases/${editableCase.caseId}/context`) &&
          response.request().method() === "POST",
      );
      await caseView.getByRole("button", { name: "Save", exact: true }).click();
      expect((await saved).ok()).toBeTruthy();
      await expect(purpose).toBeEnabled();
      await expect(purpose).toHaveValue("SCREENING");
      await page.reload();
      await expect(purpose).toHaveValue("SCREENING");
      await attachScreenshot(page, testInfo, "r11-culture-purpose-corrected");
      await page
        .getByLabel("Testing section", { exact: true })
        .selectOption("HISTORY");
      const timeline = page.getByRole("table", {
        name: "Case timeline",
        exact: true,
      });
      await expect(timeline).toContainText("Diagnostic → Screening");
      await attachScreenshot(page, testInfo, "r11-culture-purpose-audit");
    });

    const specimen = [
      population.clinical.sampleTypeId,
      population.screening.sampleTypeId,
      population.survey.sampleTypeId,
    ];
    const query = buildWhonetExportQuery(population.exportDate, { specimen });

    await test.step("Exclude screening, environmental and unselected survey cultures by default", async () => {
      await page.goto(`/Microbiology/whonet?${query}`, {
        waitUntil: "commit",
      });
      await expectWhonetExportReady(page);
      await expect(
        page.getByRole("checkbox", {
          name: "Include active screening or carriage cultures",
        }),
      ).not.toBeChecked();
      await expect(
        page.getByRole("checkbox", {
          name: "Include environmental isolates",
        }),
      ).not.toBeChecked();
      await page.getByRole("button", { name: "Preview export" }).click();
      await expect(
        page.getByRole("heading", { name: "Preview", exact: true }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(
        metric(page, "After culture-purpose selection").locator("strong"),
      ).toHaveText("2");
      await attachScreenshot(
        page,
        testInfo,
        "r11-whonet-default-population",
        true,
      );
    });

    await test.step("Include screening explicitly and restore the diagnostic population", async () => {
      const screening = page.getByRole("checkbox", {
        name: "Include active screening or carriage cultures",
      });
      const screeningLabel = page.locator(
        'label[for="whonet-include-screening"]',
      );
      await screeningLabel.click();
      await expect(screening).toBeChecked();
      await expect(page).toHaveURL(/includeScreening=true/);
      await expect(page).toHaveURL(/includeUnspecified=false/);
      await page.getByRole("button", { name: "Preview export" }).click();
      await expect(
        metric(page, "After culture-purpose selection").locator("strong"),
      ).toHaveText("4");
      await attachScreenshot(
        page,
        testInfo,
        "r11-whonet-screening-population",
        true,
      );

      await screeningLabel.click();
      await expect(screening).not.toBeChecked();
      // Unselected survey cases stay excluded under the V2 population policy.
      await page.getByRole("button", { name: "Preview export" }).click();
      await expect(
        metric(page, "After culture-purpose selection").locator("strong"),
      ).toHaveText("2");
    });

    await test.step("Keep final culture purpose visible and locked", async () => {
      await page.goto(
        `/Microbiology/cases/${population.screening.caseId}?section=order-detail`,
        { waitUntil: "commit" },
      );
      const caseView = page.getByTestId("microbiology-case-view");
      await expect(caseView).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(
        caseView.getByLabel("Culture purpose", { exact: true }),
      ).toHaveValue("SCREENING");
      await expect(
        caseView.getByLabel("Culture purpose", { exact: true }),
      ).toBeDisabled();
      await expect(
        caseView.getByRole("button", { name: "Save order detail" }),
      ).toHaveCount(0);
      await attachScreenshot(page, testInfo, "r11-final-purpose-locked");
    });

    await test.step("Keep culture-purpose filters usable at a narrow viewport", async () => {
      await page.setViewportSize({ width: 390, height: 844 });
      await page.goto(`/Microbiology/whonet?${query}`, {
        waitUntil: "commit",
      });
      await expectWhonetExportReady(page);
      const purposeFilters = page.locator(".whonet-export__purpose-filters");
      await purposeFilters.scrollIntoViewIfNeeded();
      await expect(purposeFilters).toBeVisible();
      const width = await page.evaluate(() => ({
        client: document.documentElement.clientWidth,
        scroll: document.documentElement.scrollWidth,
      }));
      expect(width.scroll).toBeLessThanOrEqual(width.client);
      await attachScreenshot(page, testInfo, "r11-whonet-purpose-mobile");
    });
  });
});
