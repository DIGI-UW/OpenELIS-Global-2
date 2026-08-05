import { expect, test } from "../../../helpers/test-base";
import type { Download } from "@playwright/test";
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

test.describe("OGC-782 M4 WHONET manual export", () => {
  test("previews mapped AST, links mapping repair, and downloads CSV", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const seeded = await seedMicrobiologyWhonetExport(page);

    await test.step("Reach the export through configured navigation", async () => {
      await page.goto("/Dashboard", { waitUntil: "domcontentloaded" });
      await page.getByRole("button", { name: "Open menu" }).click();
      await page
        .getByRole("button", { name: "Microbiology", exact: true })
        .click();
      const exportLink = page.getByRole("link", {
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
      expect(csv).toContain("NATIONAL_ID");
      const seededRows = csv
        .split(/\r?\n/)
        .filter((line) => line.includes(seeded.accessionNumber));
      expect(seededRows).toHaveLength(2);
      expect(seededRows.some((line) => line.includes("CIPUAT"))).toBeTruthy();
      expect(seededRows.some((line) => line.includes("GENUAT"))).toBeTruthy();
      expect(seededRows.every((line) => line.includes("refuat"))).toBeTruthy();
    });
  });
});
