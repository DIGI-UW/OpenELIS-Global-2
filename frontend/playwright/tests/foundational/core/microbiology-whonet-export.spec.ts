import { expect, test } from "../../../helpers/test-base";
import type { Download } from "@playwright/test";
import { Sidenav } from "../../../fixtures/sidenav";
import { seedMicrobiologyWhonetExport } from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

const currentPeriodQuery = (exportDate: string) => {
  return new URLSearchParams({
    from: exportDate,
    to: exportDate,
    significance: "CLINICALLY_SIGNIFICANT",
    dedup: "FIRST_ISOLATE_7_DAY",
    step: "configure",
    page: "1",
    pageSize: "100",
  }).toString();
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
  test("previews mapped AST, links mapping repair, and downloads CSV", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const seeded = await seedMicrobiologyWhonetExport(page);

    await test.step("Reach the export through configured navigation", async () => {
      await page.goto("/Dashboard", { waitUntil: "domcontentloaded" });
      const sidenav = new Sidenav(page);
      await sidenav.ensureExpanded();
      await sidenav.expandMenu("Microbiology");
      const exportLink = sidenav.nav.getByRole("link", {
        name: "WHONET export",
        exact: true,
      });
      await expect(exportLink).toHaveAttribute("href", "/Microbiology/whonet");
      await exportLink.click();
      await expect(
        page.getByRole("heading", { name: "WHONET export", exact: true }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
    });

    const query = currentPeriodQuery(seeded.exportDate);
    await test.step("Reload the complete canonical configuration", async () => {
      await page.goto(`/Microbiology/whonet?${query}`, {
        waitUntil: "domcontentloaded",
      });
      await expect(page).toHaveURL(`/Microbiology/whonet?${query}`);
      await expect(page.getByLabel("Inclusion")).toHaveValue(
        "CLINICALLY_SIGNIFICANT",
      );
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
      await page.reload({ waitUntil: "domcontentloaded" });
      await expect(page).toHaveURL(`/Microbiology/whonet?${query}`);
    });

    await test.step("Preview eligible rows and mapping repair", async () => {
      const previewResponse = page.waitForResponse(
        (response) =>
          response.url().includes("/rest/microbiology/whonet/preview?") &&
          response.request().method() === "GET" &&
          response.status() === 200,
      );
      await page.getByRole("button", { name: "Preview export" }).click();
      await previewResponse;
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
      const isolatesIncluded = await metricValue("Isolates included");
      const afterDeduplication = await metricValue("After de-duplication");
      const mappableIsolates = await metricValue("Mappable isolates");
      const eligibleRows = await metricValue("Eligible rows");
      const rowsExcluded = await metricValue("Rows excluded");

      expect(finalizedCases).toBeGreaterThanOrEqual(1);
      expect(isolatesFound).toBeGreaterThanOrEqual(2);
      expect(isolatesIncluded).toBeGreaterThanOrEqual(2);
      expect(isolatesIncluded).toBeLessThanOrEqual(isolatesFound);
      expect(afterDeduplication).toBeGreaterThanOrEqual(2);
      expect(afterDeduplication).toBeLessThanOrEqual(isolatesIncluded);
      expect(mappableIsolates).toBeGreaterThanOrEqual(1);
      expect(mappableIsolates).toBeLessThanOrEqual(afterDeduplication);
      expect(eligibleRows).toBeGreaterThanOrEqual(2);
      expect(rowsExcluded).toBeGreaterThanOrEqual(2);

      const mappedRows = page
        .getByRole("row")
        .filter({ hasText: seeded.accessionNumber });
      await expect(mappedRows.filter({ hasText: "CIPUAT" })).toContainText("S");
      await expect(mappedRows.filter({ hasText: "GENUAT" })).toContainText("R");
      await expect(mappedRows).toHaveCount(2);

      const repairHref =
        `/MasterListsPage/MicrobiologyReference/organisms?edit=` +
        seeded.unmappedOrganismId;
      const mappingReadiness = page.getByLabel("Mapping readiness");
      const repairLink = mappingReadiness.locator(`a[href="${repairHref}"]`);
      const warning = repairLink.locator("..");
      await expect(warning).toContainText("2 rows excluded");
      await expect(repairLink).toHaveAccessibleName("Fix organism mapping");
      await expect(repairLink).toHaveAttribute("href", repairHref);

      const previewUrl = page.url();
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
      await page.goBack({ waitUntil: "domcontentloaded" });
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
      const interpretationIndex = header.indexOf("RESULT");
      expect(accessionIndex).toBeGreaterThanOrEqual(0);
      expect(antibioticIndex).toBeGreaterThanOrEqual(0);
      expect(organismIndex).toBeGreaterThanOrEqual(0);
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
          }))
          .sort((left, right) =>
            left.antibiotic.localeCompare(right.antibiotic),
          ),
      ).toEqual([
        { antibiotic: "CIPUAT", interpretation: "S", organism: "refuat" },
        { antibiotic: "GENUAT", interpretation: "R", organism: "refuat" },
      ]);
    });
  });
});
