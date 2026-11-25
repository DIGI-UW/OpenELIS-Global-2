import { test, expect } from "../../../helpers/test-base";
import {
  seedExportData,
  ExportSeed,
} from "../../../helpers/seed-qc-export-data";

/**
 * QC inspector export (OGC-706) — E2E.
 *
 * Content correctness (BOM, escaping, PDF text, rejected date ranges) is
 * asserted in QCExportRestControllerSecurityTest. What is left for a browser:
 * that an authenticated session reaches the export over real seeded rows, and
 * that the dashboard modal offers it.
 */

const API = "/api/OpenELIS-Global";

test.describe("QC inspector export (OGC-706)", () => {
  let seed: ExportSeed;

  test.beforeAll(() => {
    seed = seedExportData();
  });

  test.afterAll(() => {
    seed?.restore();
  });

  test("CSV export returns the seeded runs and violation", async ({ page }) => {
    const res = await page.request.get(
      `${API}/rest/qc/export/csv?instrumentId=${seed.analyzerId}` +
        `&startDate=${seed.startDate}&endDate=${seed.endDate}`,
    );

    expect(res.status()).toBe(200);
    expect(res.headers()["content-type"]).toContain("text/csv");
    const body = await res.text();
    expect(body).toContain("Instrument"); // header row
    expect(body).toContain("PW Export Analyzer"); // resolved instrument name
    expect(body).toContain("1_3S"); // seeded violation rule code
    expect(body).toContain("REJECTION");
    // three seeded runs → three data rows (each carries the instrument name)
    const dataRows = body
      .split(/\r?\n/)
      .filter((line) => line.includes("PW Export Analyzer"));
    expect(dataRows.length).toBe(3);
  });

  test("export modal opens from the dashboard", async ({ page }) => {
    await page.goto("/qa/qc/dashboard", { waitUntil: "domcontentloaded" });

    const exportButton = page.getByTestId("qc-dashboard-export-button");
    await expect(exportButton).toBeVisible();
    await exportButton.click();

    await expect(
      page.getByTestId("qc-export-instrument-dropdown"),
    ).toBeVisible();
    await expect(page.getByTestId("qc-export-csv")).toBeVisible();
    await expect(page.getByTestId("qc-export-pdf")).toBeVisible();
  });
});
