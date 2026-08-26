import { expect, test } from "../../../helpers/test-base";
import {
  seedMicrobiologyWhonetExport,
  seedMicrobiologyWhonetExportFilters,
} from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";
import {
  buildWhonetExportQuery,
  expectWhonetExportReady,
  parseWhonetCsvLine,
  readWhonetDownload,
  selectWhonetFilterOption,
  whonetFixtureLabels,
} from "../../../helpers/whonet-export";
import { Sidenav } from "../../../fixtures/sidenav";

test.describe("OGC-782 WHONET manual export", () => {
  test.describe.configure({ timeout: 120_000 });

  test("preserves every R9 export population filter", async ({ page }) => {
    const seeded = await seedMicrobiologyWhonetExportFilters(page);
    const query = buildWhonetExportQuery(seeded.exportDate);
    const organismIds = [seeded.organismId, seeded.unmappedOrganismId];

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

    const filteredQuery = buildWhonetExportQuery(seeded.exportDate, {
      specimen: [seeded.sampleTypeId],
      organism: organismIds,
      origin: ["INPATIENT"],
      significance: ["CLINICALLY_SIGNIFICANT", "CONTAMINANT"],
    });
    await expect(page).toHaveURL(`/Microbiology/whonet?${filteredQuery}`);
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
    const metric = (label: string) =>
      page.locator(".whonet-export__metric").filter({ hasText: label });
    await expect(metric("After specimen filter").locator("strong")).toHaveText(
      "2",
    );
    await expect(metric("After organism filter").locator("strong")).toHaveText(
      "2",
    );
    await expect(metric("After origin filter").locator("strong")).toHaveText(
      "2",
    );
    await expect(metric("Isolates included").locator("strong")).toHaveText("2");

    const downloadPromise = page.waitForEvent("download");
    await page.getByRole("button", { name: "Generate CSV" }).click();
    const download = await downloadPromise;
    expect(await readWhonetDownload(download)).toContain(
      seeded.accessionNumber,
    );
  });

  test("previews mapped AST, links mapping repair, and downloads CSV", async ({
    page,
  }) => {
    const seeded = await seedMicrobiologyWhonetExport(page);
    const organismIds = [seeded.organismId, seeded.unmappedOrganismId];

    await test.step("Reach the export through configured navigation", async () => {
      await page.goto("/Dashboard", { waitUntil: "commit" });
      const sidenav = new Sidenav(page);
      await sidenav.ensureExpanded();
      await sidenav.expandMenu("Reports");
      const exportLink = sidenav.nav.getByRole("link", {
        name: "WHONET export",
        exact: true,
      });
      await expect(exportLink).toHaveAttribute("href", "/Microbiology/whonet");
      await exportLink.click();
      await expectWhonetExportReady(page);
    });

    const query = buildWhonetExportQuery(seeded.exportDate);
    await test.step("Reload the complete canonical configuration", async () => {
      await page.goto(`/Microbiology/whonet?${query}`, {
        waitUntil: "commit",
      });
      await expectWhonetExportReady(page);
      await expect(page).toHaveURL(`/Microbiology/whonet?${query}`);
      await expect(
        page.getByRole("combobox", { name: /^Inclusion/ }),
      ).toHaveAccessibleName(/Inclusion Total items selected: 1/);
      await expect(page.getByLabel("De-duplication")).toHaveValue(
        "FIRST_ISOLATE_7_DAY",
      );
      const breadcrumb = page.getByRole("navigation", { name: "Breadcrumb" });
      await expect(
        breadcrumb.getByRole("link", { name: "Home" }),
      ).toHaveAttribute("href", "/Dashboard");
      await expect(
        breadcrumb.getByRole("link", { name: "Reports" }),
      ).toHaveAttribute("href", "/Report");
      await page.reload({ waitUntil: "commit" });
      await expectWhonetExportReady(page);
      await expect(page).toHaveURL(`/Microbiology/whonet?${query}`);
    });

    await test.step("Select and preserve the export population", async () => {
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

      const filteredQuery = buildWhonetExportQuery(seeded.exportDate, {
        specimen: [seeded.sampleTypeId],
        organism: organismIds,
      });
      await expect(page).toHaveURL(`/Microbiology/whonet?${filteredQuery}`);
      await page.reload({ waitUntil: "commit" });
      await expectWhonetExportReady(page);
      await expect(page).toHaveURL(`/Microbiology/whonet?${filteredQuery}`);
      await expect(
        page.getByRole("combobox", { name: /^Specimen types/ }),
      ).toHaveAccessibleName(/Total items selected: 1/);
      await expect(
        page.getByRole("combobox", { name: /^Organisms/ }),
      ).toHaveAccessibleName(/Total items selected: 2/);
    });

    await test.step("Preview eligible rows and mapping repair", async () => {
      await page.getByRole("button", { name: "Preview export" }).click();
      await expect(page).toHaveURL(/step=preview/);
      await expect(
        page.getByRole("heading", { name: "Preview", exact: true }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });

      const metric = (label: string) =>
        page.locator(".whonet-export__metric").filter({ hasText: label });
      const metricValue = async (label: string) => {
        const value = metric(label).locator("strong");
        await expect(value).toHaveText(/^\d+$/);
        return Number(await value.textContent());
      };
      const finalizedCases = await metricValue("Finalized cases");
      const isolatesFound = await metricValue("Isolates found");
      const afterSpecimenFilter = await metricValue("After specimen filter");
      const afterOrganismFilter = await metricValue("After organism filter");
      const afterOriginFilter = await metricValue("After origin filter");
      const isolatesIncluded = await metricValue("Isolates included");
      const afterDeduplication = await metricValue("After de-duplication");
      const initialMappableIsolates = await metricValue("Mappable isolates");
      const initialEligibleRows = await metricValue("Eligible rows");
      const initialRowsExcluded = await metricValue("Rows excluded");

      expect(finalizedCases).toBeGreaterThanOrEqual(1);
      expect(isolatesFound).toBeGreaterThanOrEqual(2);
      expect(afterSpecimenFilter).toBeGreaterThanOrEqual(2);
      expect(afterSpecimenFilter).toBeLessThanOrEqual(isolatesFound);
      expect(afterOrganismFilter).toBeGreaterThanOrEqual(2);
      expect(afterOrganismFilter).toBeLessThanOrEqual(afterSpecimenFilter);
      expect(afterOriginFilter).toBeGreaterThanOrEqual(2);
      expect(afterOriginFilter).toBeLessThanOrEqual(afterOrganismFilter);
      expect(isolatesIncluded).toBeGreaterThanOrEqual(2);
      expect(isolatesIncluded).toBeLessThanOrEqual(afterOriginFilter);
      expect(afterDeduplication).toBeGreaterThanOrEqual(2);
      expect(afterDeduplication).toBeLessThanOrEqual(isolatesIncluded);
      expect(initialMappableIsolates).toBeLessThanOrEqual(afterDeduplication);
      expect(initialRowsExcluded).toBeGreaterThanOrEqual(4);

      const generateCsv = page.getByRole("button", { name: "Generate CSV" });
      if (initialEligibleRows === 0) {
        await expect(generateCsv).toBeDisabled();
      } else {
        await expect(generateCsv).toBeEnabled();
      }

      const previewUrl = page.url();
      const previewLocation = new URL(previewUrl);
      const previewReturnTo = `${previewLocation.pathname}${previewLocation.search}`;
      const mappingReadiness = page.getByLabel("Mapping readiness");
      const specimenRepairHref =
        `/MasterListsPage/SampleTypeEditor/${seeded.sampleTypeId}/basic-info?` +
        new URLSearchParams({
          focus: "whonet",
          returnTo: previewReturnTo,
        }).toString();
      const specimenRepairLink = mappingReadiness.locator(
        `a[href="${specimenRepairHref}"]`,
      );
      await expect(specimenRepairLink.locator("..")).toContainText(
        "rows excluded",
      );
      await expect(specimenRepairLink).toHaveAccessibleName(
        "Fix specimen mapping",
      );
      await expect(specimenRepairLink).toHaveAttribute(
        "href",
        specimenRepairHref,
      );

      await specimenRepairLink.click();
      await expect(page).toHaveURL(
        new RegExp(
          `/MasterListsPage/SampleTypeEditor/${seeded.sampleTypeId}/basic-info`,
        ),
      );
      const specimenCode = page.getByLabel("WHONET specimen code");
      await expect(specimenCode).toBeFocused();
      await specimenCode.fill("BLD");
      await page.getByRole("button", { name: "Save" }).click();
      const returnToPreview = page.getByRole("link", {
        name: "Return to WHONET preview",
      });
      await expect(returnToPreview).toBeVisible();
      await returnToPreview.click();
      await expect(page).toHaveURL(previewUrl);
      await expect(
        page.getByRole("heading", { name: "Preview", exact: true }),
      ).toBeVisible();
      await expect(
        mappingReadiness.getByRole("link", { name: "Fix specimen mapping" }),
      ).toHaveCount(0);
      const repairedMappableIsolates = await metricValue("Mappable isolates");
      const repairedEligibleRows = await metricValue("Eligible rows");
      const repairedRowsExcluded = await metricValue("Rows excluded");
      expect(repairedMappableIsolates).toBeGreaterThanOrEqual(
        initialMappableIsolates + 1,
      );
      expect(repairedEligibleRows).toBeGreaterThanOrEqual(
        initialEligibleRows + 2,
      );
      expect(repairedRowsExcluded).toBeLessThanOrEqual(initialRowsExcluded - 2);
      await expect(generateCsv).toBeEnabled();

      const mappedRows = page
        .getByRole("row")
        .filter({ hasText: seeded.accessionNumber });
      await expect(mappedRows.filter({ hasText: "CIPUAT" })).toContainText("S");
      await expect(mappedRows.filter({ hasText: "GENUAT" })).toContainText("R");
      await expect(mappedRows).toHaveCount(2);

      const repairHref =
        `/MasterListsPage/MicrobiologyReference/organisms?edit=` +
        seeded.unmappedOrganismId;
      const repairLink = mappingReadiness.locator(`a[href="${repairHref}"]`);
      const warning = repairLink.locator("..");
      await expect(warning).toContainText("2 rows excluded");
      await expect(repairLink).toHaveAccessibleName("Fix organism mapping");
      await expect(repairLink).toHaveAttribute("href", repairHref);

      await repairLink.click();
      await expect(page).toHaveURL(
        new RegExp(`edit=${seeded.unmappedOrganismId}`),
      );
      await expect(
        page.getByRole("dialog").getByRole("heading", {
          name: "Organism",
          exact: true,
        }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await page.goBack({ waitUntil: "commit" });
      await expect(page).toHaveURL(previewUrl);
      await expect(
        page.getByRole("heading", { name: "Preview", exact: true }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
    });

    await test.step("Generate and inspect the downloaded CSV", async () => {
      const downloadPromise = page.waitForEvent("download");
      await page.getByRole("button", { name: "Generate CSV" }).click();
      const download = await downloadPromise;
      await expect(page.getByText("CSV generated")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      expect(download.suggestedFilename()).toMatch(/^WHONET_.*\.csv$/);

      const csv = await readWhonetDownload(download);
      const lines = csv.split(/\r?\n/).filter(Boolean);
      const header = parseWhonetCsvLine(lines[0]);
      const accessionIndex = header.indexOf("LAB_NUMBER");
      const antibioticIndex = header.indexOf("ANTIBIOTIC");
      const organismIndex = header.indexOf("ORGANISM");
      const specimenIndex = header.indexOf("SPECIMEN_TYPE");
      const interpretationIndex = header.indexOf("RESULT");
      expect(accessionIndex).toBeGreaterThanOrEqual(0);
      expect(antibioticIndex).toBeGreaterThanOrEqual(0);
      expect(organismIndex).toBeGreaterThanOrEqual(0);
      expect(specimenIndex).toBeGreaterThanOrEqual(0);
      expect(interpretationIndex).toBeGreaterThanOrEqual(0);

      const seededRows = lines
        .slice(1)
        .map(parseWhonetCsvLine)
        .filter((row) => row[accessionIndex] === seeded.accessionNumber);
      expect(seededRows).toHaveLength(2);
      expect(
        seededRows
          .map((row) => ({
            antibiotic: row[antibioticIndex],
            interpretation: row[interpretationIndex],
            organism: row[organismIndex],
            specimen: row[specimenIndex],
          }))
          .sort((left, right) =>
            left.antibiotic.localeCompare(right.antibiotic),
          ),
      ).toEqual([
        {
          antibiotic: "CIPUAT",
          interpretation: "S",
          organism: "refuat",
          specimen: "BLD",
        },
        {
          antibiotic: "GENUAT",
          interpretation: "R",
          organism: "refuat",
          specimen: "BLD",
        },
      ]);
    });
  });
});
