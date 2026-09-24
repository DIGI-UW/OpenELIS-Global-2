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
      await expect(
        caseView.getByRole("radio", {
          name: "Clinical diagnosis or treatment",
        }),
      ).toBeChecked();
      await caseView
        .getByRole("group", { name: "Culture purpose" })
        .getByText("Active screening or carriage", { exact: true })
        .click();
      await caseView.getByRole("button", { name: "Save order detail" }).click();
      await expect(
        caseView.getByRole("radio", {
          name: "Active screening or carriage",
        }),
      ).toBeChecked();
      await caseView
        .getByRole("group", { name: "Culture purpose" })
        .scrollIntoViewIfNeeded();
      await attachScreenshot(page, testInfo, "r11-culture-purpose-corrected");

      await caseView
        .getByRole("button", { name: /^Case info Accession number:/ })
        .click();
      await expect(
        caseView.getByText("Active screening or carriage", { exact: true }),
      ).toBeVisible();
      await caseView
        .getByRole("button", { name: "Timeline", exact: true })
        .click();
      const timeline = page.getByTestId("microbiology-timeline-card");
      await expect(
        timeline.getByText("Culture purpose changed", { exact: true }),
      ).toBeVisible();
      await expect(
        timeline.getByText(
          "Clinical diagnosis or treatment to Active screening or carriage",
          { exact: true },
        ),
      ).toBeVisible();
      await attachScreenshot(page, testInfo, "r11-culture-purpose-audit");
    });

    const specimen = [
      population.clinical.sampleTypeId,
      population.screening.sampleTypeId,
      population.unspecified.sampleTypeId,
    ];
    const query = buildWhonetExportQuery(population.exportDate, { specimen });

    await test.step("Exclude screening and unspecified cultures by default", async () => {
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
          name: "Include historical cultures with unspecified purpose",
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

    await test.step("Include screening and unspecified populations independently", async () => {
      const screening = page.getByRole("checkbox", {
        name: "Include active screening or carriage cultures",
      });
      const unspecified = page.getByRole("checkbox", {
        name: "Include historical cultures with unspecified purpose",
      });
      const screeningLabel = page.locator(
        'label[for="whonet-include-screening"]',
      );
      const unspecifiedLabel = page.locator(
        'label[for="whonet-include-unspecified"]',
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
      await unspecifiedLabel.click();
      await expect(unspecified).toBeChecked();
      await expect(page).toHaveURL(/includeScreening=false/);
      await expect(page).toHaveURL(/includeUnspecified=true/);
      await page.getByRole("button", { name: "Preview export" }).click();
      await expect(
        metric(page, "After culture-purpose selection").locator("strong"),
      ).toHaveText("4");
      await attachScreenshot(
        page,
        testInfo,
        "r11-whonet-unspecified-population",
        true,
      );
    });

    await test.step("Keep final culture purpose visible and locked", async () => {
      await page.goto(
        `/Microbiology/cases/${population.screening.caseId}?section=order-detail`,
        { waitUntil: "commit" },
      );
      const caseView = page.getByTestId("microbiology-case-view");
      await expect(caseView).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(
        caseView.getByRole("radio", {
          name: "Active screening or carriage",
        }),
      ).toBeChecked();
      await expect(
        caseView.getByRole("radio", {
          name: "Clinical diagnosis or treatment",
        }),
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
