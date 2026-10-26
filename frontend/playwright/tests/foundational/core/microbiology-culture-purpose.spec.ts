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
import { createDemoPresentation } from "../../../helpers/demo-presentation";

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
  test(
    "audits a pre-release correction and applies explicit WHONET inclusion",
    { tag: ["@evidence", "@microbiology", "@case"] },
    async ({ page }, testInfo) => {
      test.setTimeout(240_000);
      const demo = createDemoPresentation(page, testInfo);
      await demo.intro(
        "Culture purpose: corrected, audited and honoured by WHONET",
        "A purpose corrected before release is audited, WHONET exports include screening cultures only when asked, and a released purpose is locked.",
      );
      await demo.caption(
        "Off screen: a diagnostic case is ordered, and finalized clinical, screening and survey cultures are prepared for export.",
      );
      const editableCase = await seedMicrobiologyCulturePurposeCase(page);
      const population =
        await seedMicrobiologyCulturePurposeWhonetPopulation(page);

      await test.step("Correct culture purpose before final release", async () => {
        await demo.caption(
          "Before release, the culture purpose is corrected from diagnostic to screening.",
        );
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
        await caseView
          .getByRole("button", { name: "Save", exact: true })
          .click();
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
        await demo.caption("The case timeline records the correction.");
        await expect(timeline).toContainText("Diagnostic → Screening");
        await demo.highlight(
          timeline.getByText("Diagnostic → Screening").first(),
        );
        await attachScreenshot(page, testInfo, "r11-culture-purpose-audit");
      });

      const specimen = [
        population.clinical.sampleTypeId,
        population.screening.sampleTypeId,
        population.survey.sampleTypeId,
      ];
      const query = buildWhonetExportQuery(population.exportDate, { specimen });

      await test.step("Exclude screening, environmental and unselected survey cultures by default", async () => {
        await demo.caption(
          "By default, WHONET export leaves out screening, environmental and unselected survey cultures.",
        );
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
        await demo.highlight(metric(page, "After culture-purpose selection"));
        await attachScreenshot(
          page,
          testInfo,
          "r11-whonet-default-population",
          true,
        );
      });

      await test.step("Include screening explicitly and restore the diagnostic population", async () => {
        await demo.caption(
          "Including screening cultures explicitly adds them to the population.",
        );
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
        await demo.highlight(metric(page, "After culture-purpose selection"));
        await demo.caption(
          "Clearing the option restores the diagnostic population.",
        );
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
        await demo.caption(
          "On a released case, the culture purpose is shown but can no longer be changed.",
        );
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
        await demo.highlight(
          caseView.getByLabel("Culture purpose", { exact: true }),
        );
        await attachScreenshot(page, testInfo, "r11-final-purpose-locked");
      });

      await test.step("Keep culture-purpose filters usable at a narrow viewport", async () => {
        const frame = page.viewportSize();
        await demo.caption(
          "On a phone-width screen the purpose filters still fit.",
        );
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
        if (frame) await page.setViewportSize(frame);
      });
      await demo.verified(
        "The purpose correction is audited and WHONET follows it",
        "Screening cultures join the export only when included, and a released purpose is locked.",
      );
    },
  );
});
