import { expect, test } from "../../../helpers/test-base";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import { seedMicrobiologyWhonetExport } from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

test.describe("OGC-782 M4 WHONET export demo", () => {
  test("previews finalized bacteriology and generates CSV", async ({
    page,
  }, testInfo) => {
    test.setTimeout(180_000);
    const demo = createDemoPresentation(page, testInfo);
    const seeded = await seedMicrobiologyWhonetExport(page);
    const query = new URLSearchParams({
      from: seeded.exportDate,
      to: seeded.exportDate,
      significance: "CLINICALLY_SIGNIFICANT",
      dedup: "FIRST_ISOLATE_7_DAY",
      step: "configure",
      page: "1",
      pageSize: "20",
    }).toString();

    await demo.chapter({
      eyebrow: "OGC-782 M4",
      title: "WHONET manual export",
      subtitle:
        "Preview finalized routine bacteriology, repair mappings in context, and download an attributable CSV",
    });

    await test.step("Configure a bookmarkable export", async () => {
      await demo.chapter({
        eyebrow: "Story 1",
        title: "Configure a routine export",
        subtitle:
          "A compact, bookmarkable workflow keeps the reporting period and surveillance policy explicit",
        accent: "#24a148",
      });
      await page.goto(`/Microbiology/whonet?${query}`, {
        waitUntil: "domcontentloaded",
      });
      await expect(
        page.getByRole("heading", { name: "WHONET export", exact: true }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(page.getByLabel("Inclusion")).toHaveValue(
        "CLINICALLY_SIGNIFICANT",
      );
      await expect(page.getByLabel("De-duplication")).toHaveValue(
        "FIRST_ISOLATE_7_DAY",
      );
      await demo.evidence("ogc-782-m4-01-whonet-configure", {
        fullPage: true,
      });
      await demo.pause(2500);
    });

    await test.step("Preview eligible rows and scoped mapping gaps", async () => {
      await demo.chapter({
        eyebrow: "Story 2",
        title: "Preview before generating",
        subtitle:
          "Counts, all AST readings, and only the mapping gaps in this export are visible before download",
        accent: "#ff832b",
      });
      const previewResponse = page.waitForResponse(
        (response) =>
          response.url().includes("/rest/microbiology/whonet/preview?") &&
          response.status() === 200,
      );
      await page.getByRole("button", { name: "Preview export" }).click();
      await previewResponse;
      await expect(
        page.getByRole("heading", { name: "Preview", exact: true }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      const mappedRows = page
        .getByRole("row")
        .filter({ hasText: seeded.accessionNumber });
      await expect(mappedRows).toHaveCount(2);
      await expect(mappedRows.filter({ hasText: "CIPUAT" })).toContainText("S");
      await expect(mappedRows.filter({ hasText: "GENUAT" })).toContainText("R");
      await demo.evidence("ogc-782-m4-02-whonet-preview", {
        fullPage: true,
      });
      await demo.pause(3000);
    });

    await test.step("Follow the exact mapping repair path and return", async () => {
      await demo.scene("Repair only the affected organism mapping");
      const previewUrl = page.url();
      const repairHref =
        `/MasterListsPage/MicrobiologyReference/organisms?edit=` +
        seeded.unmappedOrganismId;
      const repairLink = page
        .getByLabel("Mapping readiness")
        .locator(`a[href="${repairHref}"]`);
      await expect(repairLink).toHaveAccessibleName("Fix organism mapping");
      await repairLink.click();
      const dialog = page.getByRole("dialog");
      await expect(
        dialog.getByRole("heading", { name: "Organism", exact: true }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await demo.evidence("ogc-782-m4-03-mapping-repair", { locator: dialog });
      await demo.pause(2500);
      await page.goBack({ waitUntil: "domcontentloaded" });
      await expect(page).toHaveURL(previewUrl);
      await expect(
        page.getByRole("heading", { name: "Preview", exact: true }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
    });

    await test.step("Generate the WHONET CSV", async () => {
      await demo.chapter({
        eyebrow: "Story 3",
        title: "Generate an attributable CSV",
        subtitle:
          "Only eligible mapped rows are downloaded, with WHONET S and R interpretation codes",
        accent: "#8a3ffc",
      });
      const downloadPromise = page.waitForEvent("download");
      await page.getByRole("button", { name: "Generate CSV" }).click();
      const download = await downloadPromise;
      await expect(page.getByText("CSV generated")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      expect(download.suggestedFilename()).toMatch(/^WHONET_.*\.csv$/);
      await demo.evidence("ogc-782-m4-04-whonet-generated", {
        fullPage: true,
      });
      await demo.pause(2500);
    });

    await demo.chapter({
      eyebrow: "Evidence complete",
      title: "Automated M4 journey passed",
      subtitle:
        "Configuration, preview, scoped repair, and CSV download passed; human Review-overlay rulings remain separate.",
      durationMs: 4000,
    });
  });

  test("captures mobile configure and preview evidence", async ({
    page,
  }, testInfo) => {
    test.skip(
      process.env.PLAYWRIGHT_MOBILE_EVIDENCE !== "on",
      "Mobile evidence is captured explicitly after the desktop walkthrough",
    );
    const demo = createDemoPresentation(page, testInfo);
    const seeded = await seedMicrobiologyWhonetExport(page);
    const query = new URLSearchParams({
      from: seeded.exportDate,
      to: seeded.exportDate,
      significance: "CLINICALLY_SIGNIFICANT",
      dedup: "FIRST_ISOLATE_7_DAY",
      step: "configure",
      page: "1",
      pageSize: "20",
    }).toString();
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
      waitUntil: "domcontentloaded",
    });
    await expect(
      page.getByRole("heading", { name: "WHONET export", exact: true }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expectNoHorizontalPageOverflow();
    await demo.evidence("ogc-782-m4-05-whonet-configure-mobile", {
      fullPage: true,
    });

    const previewResponse = page.waitForResponse(
      (response) =>
        response.url().includes("/rest/microbiology/whonet/preview?") &&
        response.status() === 200,
    );
    await page.getByRole("button", { name: "Preview export" }).click();
    await previewResponse;
    await expect(
      page.getByRole("heading", { name: "Preview", exact: true }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expectNoHorizontalPageOverflow();
    await page.evaluate(() => window.scrollTo(0, 0));
    await expect(
      page
        .getByTestId("whonet-export")
        .getByRole("link", { name: "Home", exact: true }),
    ).toBeVisible();
    await demo.evidence("ogc-782-m4-06-whonet-preview-mobile", {
      fullPage: true,
    });
  });
});
