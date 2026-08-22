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

const metric = (page: import("@playwright/test").Page, label: string) =>
  page.locator(".whonet-export__metric").filter({ hasText: label });

test.describe("OGC-782 R11 culture purpose", () => {
  test("audits a pre-release correction and applies explicit WHONET inclusion", async ({
    page,
  }) => {
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
        .getByRole("radio", { name: "Active screening or carriage" })
        .click();
      await caseView.getByRole("button", { name: "Save order detail" }).click();
      await expect(
        caseView.getByRole("radio", {
          name: "Active screening or carriage",
        }),
      ).toBeChecked();

      await caseView
        .getByRole("button", { name: "Case information", exact: false })
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
    });

    await test.step("Include screening and unspecified populations independently", async () => {
      const screening = page.getByRole("checkbox", {
        name: "Include active screening or carriage cultures",
      });
      const unspecified = page.getByRole("checkbox", {
        name: "Include historical cultures with unspecified purpose",
      });
      await screening.click();
      await expect(page).toHaveURL(/includeScreening=true/);
      await expect(page).toHaveURL(/includeUnspecified=false/);
      await page.getByRole("button", { name: "Preview export" }).click();
      await expect(
        metric(page, "After culture-purpose selection").locator("strong"),
      ).toHaveText("4");

      await screening.click();
      await unspecified.click();
      await expect(page).toHaveURL(/includeScreening=false/);
      await expect(page).toHaveURL(/includeUnspecified=true/);
      await page.getByRole("button", { name: "Preview export" }).click();
      await expect(
        metric(page, "After culture-purpose selection").locator("strong"),
      ).toHaveText("4");
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
    });
  });
});
