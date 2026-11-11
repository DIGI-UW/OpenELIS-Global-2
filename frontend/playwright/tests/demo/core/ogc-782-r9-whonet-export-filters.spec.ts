import { expect, test } from "../../../helpers/test-base";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import { seedMicrobiologyWhonetExportFilters } from "../../../helpers/seed-microbiology-data";
import {
  buildWhonetExportQuery,
  expectWhonetExportReady,
  readWhonetDownload,
  selectWhonetFilterOption,
  whonetFixtureLabels,
} from "../../../helpers/whonet-export";

const selectedFilterQuery = (
  seeded: Awaited<ReturnType<typeof seedMicrobiologyWhonetExportFilters>>,
) =>
  buildWhonetExportQuery(seeded.exportDate, {
    specimen: [seeded.sampleTypeId],
    organism: [seeded.organismId, seeded.unmappedOrganismId],
    origin: ["INPATIENT"],
    significance: ["CLINICALLY_SIGNIFICANT", "CONTAMINANT"],
  });

const metric = (page: import("@playwright/test").Page, label: string) =>
  page.locator(".whonet-export__metric").filter({ hasText: label });

test.describe("OGC-782 R9 WHONET export population filters", () => {
  test("filters, reloads, previews, and exports the selected population", async ({
    page,
  }, testInfo) => {
    test.setTimeout(180_000);
    const demo = createDemoPresentation(page, testInfo);
    const seeded = await seedMicrobiologyWhonetExportFilters(page);
    const query = buildWhonetExportQuery(seeded.exportDate);
    const filteredQuery = selectedFilterQuery(seeded);

    await demo.chapter({
      eyebrow: "OGC-782 R9",
      title: "Filter the WHONET export population",
      subtitle:
        "Choose the finalized bacteriology population, reload the bookmarkable state, preview the result, and download the same rows",
    });

    await test.step("Select every R9 population filter", async () => {
      await demo.chapter({
        eyebrow: "Story 1",
        title: "Choose the surveillance population",
        subtitle:
          "Specimen, organism, patient origin, significance, and first-isolate policy define one explicit export population",
        accent: "#24a148",
      });
      await page.goto(`/Microbiology/whonet?${query}`, {
        waitUntil: "commit",
      });
      await expectWhonetExportReady(page);
      await selectWhonetFilterOption(
        page,
        /^Specimen types/,
        whonetFixtureLabels.specimen(seeded.accessionNumber),
      );
      await selectWhonetFilterOption(
        page,
        /^Organisms/,
        whonetFixtureLabels.mappedOrganism,
      );
      await selectWhonetFilterOption(
        page,
        /^Organisms/,
        whonetFixtureLabels.unmappedOrganism(seeded.accessionNumber),
      );
      await selectWhonetFilterOption(
        page,
        /^Patient origins/,
        whonetFixtureLabels.inpatient,
      );
      await selectWhonetFilterOption(page, /^Inclusion/, "Contaminant");
      await expect(page).toHaveURL(`/Microbiology/whonet?${filteredQuery}`);
      await demo.evidence("ogc-782-r9-01-whonet-filters", {
        fullPage: true,
      });
      await demo.pause(2500);
    });

    await test.step("Reload the same population and preview it", async () => {
      await demo.chapter({
        eyebrow: "Story 2",
        title: "Reload and preview the same selection",
        subtitle:
          "The canonical URL restores every control before the server applies the same filters to the preview",
        accent: "#ff832b",
      });
      await page.reload({ waitUntil: "commit" });
      await expectWhonetExportReady(page);
      await expect(page).toHaveURL(`/Microbiology/whonet?${filteredQuery}`);
      await expect(
        page.getByRole("combobox", { name: /^Specimen types/ }),
      ).toHaveAccessibleName(/Total items selected: 1/);
      await expect(
        page.getByRole("combobox", { name: /^Organisms/ }),
      ).toHaveAccessibleName(/Total items selected: 2/);
      await expect(
        page.getByRole("combobox", { name: /^Patient origins/ }),
      ).toHaveAccessibleName(/Total items selected: 1/);
      await expect(
        page.getByRole("combobox", { name: /^Inclusion/ }),
      ).toHaveAccessibleName(/Total items selected: 2/);

      await page.getByRole("button", { name: "Preview export" }).click();
      await expect(page).toHaveURL(/step=preview/);
      await expect(
        page.getByRole("heading", { name: "Preview", exact: true }),
      ).toBeVisible();
      await expect(
        metric(page, "After specimen filter").locator("strong"),
      ).toHaveText("2");
      await expect(
        metric(page, "After organism filter").locator("strong"),
      ).toHaveText("2");
      await expect(
        metric(page, "After origin filter").locator("strong"),
      ).toHaveText("2");
      await expect(
        metric(page, "Isolates included").locator("strong"),
      ).toHaveText("2");
      await demo.evidence("ogc-782-r9-02-whonet-preview", {
        fullPage: true,
      });
      await demo.pause(3000);
    });

    await test.step("Generate the CSV from the previewed population", async () => {
      await demo.chapter({
        eyebrow: "Story 3",
        title: "Download exactly the previewed population",
        subtitle:
          "The generated CSV contains the eligible mapped AST rows while the pending organism remains visibly excluded",
        accent: "#8a3ffc",
      });
      const downloadPromise = page.waitForEvent("download");
      await page.getByRole("button", { name: "Generate CSV" }).click();
      const download = await downloadPromise;
      await expect(page.getByText("CSV generated")).toBeVisible();
      expect(download.suggestedFilename()).toMatch(/^WHONET_.*\.csv$/);
      expect(await readWhonetDownload(download)).toContain(
        seeded.accessionNumber,
      );
      await demo.evidence("ogc-782-r9-03-whonet-generated", {
        fullPage: true,
      });
      await demo.pause(2500);
    });

    await demo.chapter({
      eyebrow: "Evidence complete",
      title: "Automated R9 journey passed",
      subtitle:
        "Filter selection, canonical reload, server preview, and CSV generation passed; human Review-overlay rulings remain separate.",
      durationMs: 4000,
    });
  });

  test("captures the filtered workflow on mobile", async ({
    page,
  }, testInfo) => {
    test.skip(
      process.env.PLAYWRIGHT_MOBILE_EVIDENCE !== "on",
      "Mobile evidence is captured explicitly after the desktop walkthrough",
    );
    const demo = createDemoPresentation(page, testInfo);
    const seeded = await seedMicrobiologyWhonetExportFilters(page);
    const query = selectedFilterQuery(seeded);
    const expectNoHorizontalPageOverflow = async () => {
      const dimensions = await page.evaluate(() => ({
        pageWidth: document.documentElement.scrollWidth,
        viewportWidth: document.documentElement.clientWidth,
      }));
      expect(dimensions.pageWidth).toBeLessThanOrEqual(
        dimensions.viewportWidth + 1,
      );
    };

    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto(`/Microbiology/whonet?${query}`, {
      waitUntil: "commit",
    });
    await expectWhonetExportReady(page);
    await expectNoHorizontalPageOverflow();
    await expect(
      page.getByRole("combobox", { name: /^Organisms/ }),
    ).toHaveAccessibleName(/Total items selected: 2/);
    await demo.evidence("ogc-782-r9-04-whonet-filters-mobile", {
      fullPage: true,
    });

    await page.getByRole("button", { name: "Preview export" }).click();
    await expect(
      page.getByRole("heading", { name: "Preview", exact: true }),
    ).toBeVisible();
    await expectNoHorizontalPageOverflow();
    await page.evaluate(() => window.scrollTo(0, 0));
    await demo.evidence("ogc-782-r9-05-whonet-preview-mobile", {
      fullPage: true,
    });
  });
});
