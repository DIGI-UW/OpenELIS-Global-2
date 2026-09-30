import { randomUUID } from "node:crypto";
import { expect, test } from "../../../helpers/test-base";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import { seedMicrobiologyReferenceAdmin } from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

const ADMIN_BASE = "/MasterListsPage/MicrobiologyReference";

const canonicalQuery = (values: Record<string, string> = {}) =>
  new URLSearchParams({
    status: "ALL",
    sort: "name",
    page: "1",
    pageSize: "20",
    ...values,
  }).toString();

test.describe("OGC-782 M3 reference administration demo", () => {
  test("maintains reference data, panel versions, and breakpoint imports", async ({
    page,
  }, testInfo) => {
    test.setTimeout(180_000);
    const demo = createDemoPresentation(page, testInfo);
    const seeded = await seedMicrobiologyReferenceAdmin(page);

    await demo.chapter({
      eyebrow: "OGC-782 M3",
      title: "Reference and breakpoint administration",
      subtitle:
        "Organism vocabularies, immutable AST panels, and guarded breakpoint imports",
    });

    await test.step("Reach organism administration through Admin navigation", async () => {
      await demo.chapter({
        eyebrow: "Story 1",
        title: "Maintain reference vocabularies",
        subtitle:
          "Reach the catalog through Admin navigation and preserve bookmarkable list state",
        accent: "#24a148",
      });
      await page.goto("/MasterListsPage", { waitUntil: "domcontentloaded" });
      await page
        .getByRole("button", {
          name: "Microbiology reference data",
          exact: true,
        })
        .click();
      await page.getByRole("link", { name: "Organisms", exact: true }).click();
      await expect(
        page.getByRole("heading", { name: "Microbiology reference data" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(
        page.getByRole("navigation", { name: "Breadcrumb" }),
      ).toContainText("Admin Management");

      const search = page.getByRole("searchbox", { name: "Filter table" });
      await expect(search).toBeVisible({ timeout: LONG_TIMEOUT });
      await search.fill("Reference organism (UAT)");
      await expect(page).toHaveURL(/q=Reference\+organism/);
      await expect(
        page.getByRole("row").filter({ hasText: "Reference organism (UAT)" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await demo.evidence("ogc-782-m3-01-organism-catalog");
      await demo.pause(2500);
    });

    await test.step("Edit service-created vocabulary", async () => {
      await demo.scene("Edit the synthetic organism");
      const row = page
        .getByRole("row")
        .filter({ hasText: "Reference organism (UAT)" });
      await row.getByRole("button", { name: "Options" }).click();
      await page.getByRole("menuitem", { name: "Edit" }).click();
      const dialog = page.getByRole("dialog");
      await expect(
        dialog.getByRole("heading", { name: "Organism", exact: true }),
      ).toBeVisible();
      await dialog
        .getByLabel("Notes")
        .fill("Synthetic organism reviewed during the M3 walkthrough");
      await demo.evidence("ogc-782-m3-02-organism-editor", {
        locator: dialog,
      });
      await demo.pause(2200);
      await dialog.getByRole("button", { name: "Save" }).click();
      await expect(page.getByText("Reference saved")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await expect(page).toHaveURL(/q=Reference\+organism/);
    });

    await test.step("Publish an immutable AST panel version", async () => {
      await demo.chapter({
        eyebrow: "Story 2",
        title: "Publish an immutable AST panel version",
        subtitle:
          "Create a new current version while historical AST runs keep their original panel",
        accent: "#ff832b",
      });
      await page.getByRole("link", { name: "AST panels", exact: true }).click();
      const panelSearch = page.getByRole("searchbox", {
        name: "Filter table",
      });
      await expect(panelSearch).toBeVisible({ timeout: LONG_TIMEOUT });
      await panelSearch.fill("Gram negative AST panel (UAT)");
      const currentRow = page
        .getByRole("row")
        .filter({ hasText: "Gram negative AST panel (UAT)" })
        .filter({ hasText: "Current" });
      await expect(currentRow).toBeVisible({ timeout: LONG_TIMEOUT });
      const originalVersion = Number(
        (await currentRow.getByRole("cell").nth(2).innerText()).replace(
          /^v/,
          "",
        ),
      );
      await currentRow.getByRole("button", { name: "Options" }).click();
      await page.getByRole("menuitem", { name: "Publish new version" }).click();
      const panelDialog = page.getByRole("dialog");
      await expect(panelDialog).toContainText(
        "Saving creates a new panel version",
      );
      await panelDialog.getByLabel("Tier").first().selectOption("2");
      await demo.evidence("ogc-782-m3-03-panel-version-editor", {
        locator: panelDialog,
      });
      await demo.pause(2200);
      await panelDialog
        .getByRole("button", { name: "Publish new version" })
        .click();
      const confirmation = page.getByRole("dialog");
      await confirmation
        .getByRole("button", { name: "Publish new version" })
        .click();
      const publishedRow = page
        .getByRole("row")
        .filter({ hasText: "Gram negative AST panel (UAT)" })
        .filter({ hasText: `v${originalVersion + 1}` })
        .filter({ hasText: "Current" });
      await expect(publishedRow).toBeVisible({ timeout: LONG_TIMEOUT });
      await publishedRow.scrollIntoViewIfNeeded();
      await demo.scene(`Published v${originalVersion + 1} is current`);
      await demo.evidence("ogc-782-m3-04-panel-version-published", {
        locator: publishedRow,
      });
      await demo.pause(2500);
    });

    await test.step("Inspect synthetic breakpoint lifecycle", async () => {
      await demo.chapter({
        eyebrow: "Story 3",
        title: "Inspect breakpoint catalog lifecycle",
        subtitle:
          "Review a loaded standard and the exact rules available for new AST work",
        accent: "#8a3ffc",
      });
      await page.goto(
        `${ADMIN_BASE}/breakpoints/${seeded.loadedBreakpointStandardId}?${canonicalQuery({ method: "MIC" })}`,
        { waitUntil: "domcontentloaded" },
      );
      await expect(
        page.getByRole("heading", { name: "CLSI SYNTH-UAT-LOADED" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(page.getByRole("table")).toContainText("UAT_SYNTHETIC");
      await demo.evidence("ogc-782-m3-05-breakpoint-standard-detail");
      await demo.pause(2500);
    });

    await test.step("Import synthetic breakpoint data safely", async () => {
      await demo.chapter({
        eyebrow: "Story 4",
        title: "Import breakpoint updates safely",
        subtitle:
          "Preview mixed-validity rows, preserve actionable errors, and apply only valid data",
        accent: "#ee5396",
      });
      await demo.scene("Preview a guarded CSV import");
      await page.getByRole("button", { name: "Back to standards" }).click();
      const importVersion = `SYNTH-DEMO-${randomUUID()
        .slice(0, 8)
        .toUpperCase()}`;
      const csv = [
        "publisher,version,organism_or_group,antibiotic_whonet_code,method,specimen_type_id,breakpoint_type,susceptible_value,intermediate_lower_value,intermediate_upper_value,resistant_value,units",
        `CLSI,${importVersion},group:UAT_SYNTHETIC,REFUAT,MIC,,MIC,1,2,2,4,synthetic-mg/L`,
        `CLSI,${importVersion},Unknown organism,REFUAT,MIC,,MIC,1,2,2,4,synthetic-mg/L`,
        `CLSI,${importVersion},group:UAT_SYNTHETIC,REFUAT,MIC,,MIC,not-a-number,2,2,4,synthetic-mg/L`,
      ].join("\n");
      await page.getByRole("button", { name: "Import CSV" }).click();
      await page.locator('input[type="file"]').setInputFiles({
        name: "synthetic-breakpoints.csv",
        mimeType: "text/csv",
        buffer: Buffer.from(csv),
      });
      const importDialog = page.getByRole("dialog");
      await expect(importDialog.getByText("1 valid")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await expect(importDialog.getByText("2 skipped")).toBeVisible();
      await expect(importDialog.getByText(/Unknown organism/)).toBeVisible();
      await expect(importDialog.getByText(/Invalid decimal/)).toBeVisible();
      await demo.evidence("ogc-782-m3-06-breakpoint-import-preview", {
        locator: importDialog,
      });
      await demo.pause(3000);
      await importDialog
        .getByRole("button", { name: "Apply valid rows" })
        .click();
      await expect(importDialog.getByText("1 imported")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await demo.evidence("ogc-782-m3-07-breakpoint-import-applied", {
        locator: importDialog,
      });
      await demo.pause(2500);
    });

    await demo.chapter({
      eyebrow: "Evidence complete",
      title: "Automated M3 journeys passed",
      subtitle:
        "Reference edits, panel versioning, and guarded imports passed; human Review-overlay rulings remain separate.",
      durationMs: 4000,
    });
  });
});
