import { expect, test } from "../../../helpers/test-base";
import type { Download } from "@playwright/test";
import {
  seedMicrobiologyWhonetExport,
  seedMicrobiologyWhonetExportFilters,
} from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";
import { Sidenav } from "../../../fixtures/sidenav";

const currentPeriodQuery = (
  exportDate: string,
  filters: {
    specimen?: string[];
    organism?: string[];
    origin?: string[];
    significance?: string[];
  } = {},
) => {
  const params = new URLSearchParams({
    from: exportDate,
    to: exportDate,
    dedup: "FIRST_ISOLATE_7_DAY",
    step: "configure",
    page: "1",
    pageSize: "100",
  });
  [...(filters.specimen || [])]
    .sort()
    .forEach((id) => params.append("specimen", id));
  [...(filters.organism || [])]
    .sort()
    .forEach((id) => params.append("organism", id));
  [...(filters.origin || [])]
    .sort()
    .forEach((id) => params.append("origin", id));
  [...(filters.significance || ["CLINICALLY_SIGNIFICANT"])]
    .sort()
    .forEach((id) => params.append("significance", id));
  const canonical = new URLSearchParams();
  ["from", "to"].forEach((key) => canonical.set(key, params.get(key) || ""));
  params
    .getAll("specimen")
    .forEach((value) => canonical.append("specimen", value));
  params
    .getAll("organism")
    .forEach((value) => canonical.append("organism", value));
  params.getAll("origin").forEach((value) => canonical.append("origin", value));
  params
    .getAll("significance")
    .forEach((value) => canonical.append("significance", value));
  ["dedup", "step", "page", "pageSize"].forEach((key) =>
    canonical.set(key, params.get(key) || ""),
  );
  return canonical.toString();
};

const selectFilterOption = async (
  page: import("@playwright/test").Page,
  filterName: RegExp,
  optionName: string,
) => {
  const filter = page.getByRole("combobox", { name: filterName });
  await filter.click();
  const listbox = page.getByRole("listbox", { name: filterName });
  await expect(listbox).toBeVisible();
  const supportsTextEntry = await filter.evaluate((element) =>
    element.matches("input, textarea, [contenteditable='true']"),
  );
  if (supportsTextEntry) {
    await filter.fill(optionName);
  }
  const option = listbox.getByRole("option", {
    name: optionName,
    exact: true,
  });
  await expect(option).toBeVisible();
  await option.click();
  await filter.press("Escape");
  await expect(listbox).toBeHidden();
};

const expectWhonetExportReady = async (
  page: import("@playwright/test").Page,
) => {
  await expect(page.getByTestId("whonet-export")).toBeVisible({
    timeout: 0,
  });
  await expect(
    page.getByRole("heading", { name: "WHONET export", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("combobox", { name: /^Specimen types/ }),
  ).toBeEnabled();
};

const fixtureLabels = {
  specimen: (accessionNumber: string) =>
    `UAT WHONET specimen ${accessionNumber.replace(/^UATMICRO/, "")}`,
  mappedOrganism: "Reference organism (UAT)",
  unmappedOrganism: (accessionNumber: string) =>
    `WHONET mapping pending (UAT ${accessionNumber.replace(/^UATMICRO/, "")})`,
  inpatient: "Inpatient",
};

const readDownload = async (download: Download) => {
  const stream = await download.createReadStream();
  let content = "";
  for await (const chunk of stream) content += chunk.toString();
  return content;
};

const parseCsvLine = (line: string) => {
  const fields: string[] = [];
  const pattern = /"((?:[^"]|"")*)"(?:,|$)/g;
  for (const match of line.matchAll(pattern)) {
    fields.push(match[1].replace(/""/g, '"'));
  }
  return fields;
};

test.describe("OGC-782 M4 WHONET manual export", () => {
  test.describe.configure({ timeout: 120_000 });

  test("preserves every R9 export population filter", async ({ page }) => {
    const seeded = await seedMicrobiologyWhonetExportFilters(page);
    const query = currentPeriodQuery(seeded.exportDate);
    const organismIds = [seeded.organismId, seeded.unmappedOrganismId];

    await page.goto(`/Microbiology/whonet?${query}`, {
      waitUntil: "commit",
    });
    await expectWhonetExportReady(page);

    await selectFilterOption(
      page,
      /^Specimen types/,
      fixtureLabels.specimen(seeded.accessionNumber),
    );
    await selectFilterOption(page, /^Organisms/, fixtureLabels.mappedOrganism);
    await selectFilterOption(
      page,
      /^Organisms/,
      fixtureLabels.unmappedOrganism(seeded.accessionNumber),
    );
    await selectFilterOption(page, /^Patient origins/, fixtureLabels.inpatient);
    await selectFilterOption(page, /^Inclusion/, "Contaminant");

    const filteredQuery = currentPeriodQuery(seeded.exportDate, {
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
    expect(await readDownload(download)).toContain(seeded.accessionNumber);
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

    const query = currentPeriodQuery(seeded.exportDate);
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
      await selectFilterOption(
        page,
        /^Specimen types/,
        fixtureLabels.specimen(seeded.accessionNumber),
      );
      await selectFilterOption(
        page,
        /^Organisms/,
        fixtureLabels.mappedOrganism,
      );
      await selectFilterOption(
        page,
        /^Organisms/,
        fixtureLabels.unmappedOrganism(seeded.accessionNumber),
      );

      const filteredQuery = currentPeriodQuery(seeded.exportDate, {
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
      expect(
        Number(await metric("Finalized cases").locator("strong").innerText()),
      ).toBeGreaterThanOrEqual(1);
      expect(
        Number(await metric("Isolates found").locator("strong").innerText()),
      ).toBeGreaterThanOrEqual(2);
      await expect(
        metric("After specimen filter").locator("strong"),
      ).toHaveText("2");
      await expect(
        metric("After organism filter").locator("strong"),
      ).toHaveText("2");
      await expect(metric("After origin filter").locator("strong")).toHaveText(
        "2",
      );
      await expect(metric("Isolates included").locator("strong")).toHaveText(
        "2",
      );
      await expect(metric("After de-duplication").locator("strong")).toHaveText(
        "2",
      );
      await expect(metric("Mappable isolates").locator("strong")).toHaveText(
        "0",
      );
      await expect(metric("Eligible rows").locator("strong")).toHaveText("0");
      await expect(metric("Rows excluded").locator("strong")).toHaveText("4");
      await expect(
        page.getByRole("button", { name: "Generate CSV" }),
      ).toBeDisabled();

      const previewUrl = page.url();
      const previewLocation = new URL(previewUrl);
      const previewReturnTo = `${previewLocation.pathname}${previewLocation.search}`;
      const mappingReadiness = page.getByLabel("Mapping readiness");
      const specimenRepairHref =
        `/MasterListsPage/SampleTypeManagement/${seeded.sampleTypeId}/basic-info?` +
        new URLSearchParams({
          focus: "whonet",
          returnTo: previewReturnTo,
        }).toString();
      const specimenRepairLink = mappingReadiness.locator(
        `a[href="${specimenRepairHref}"]`,
      );
      await expect(specimenRepairLink.locator("..")).toContainText(
        "4 rows excluded",
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
          `/MasterListsPage/SampleTypeManagement/${seeded.sampleTypeId}/basic-info`,
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
      await expect(metric("Mappable isolates").locator("strong")).toHaveText(
        "1",
      );
      await expect(metric("Eligible rows").locator("strong")).toHaveText("2");
      await expect(metric("Rows excluded").locator("strong")).toHaveText("2");

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

      const csv = await readDownload(download);
      const lines = csv.split(/\r?\n/).filter(Boolean);
      const header = parseCsvLine(lines[0]);
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
        .map(parseCsvLine)
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
