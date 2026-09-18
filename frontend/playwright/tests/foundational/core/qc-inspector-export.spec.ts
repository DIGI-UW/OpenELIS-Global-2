import { test, expect } from "../../../helpers/test-base";
import { Page } from "@playwright/test";
import {
  seedExportData,
  ExportSeed,
} from "../../../helpers/seed-qc-export-data";

/**
 * QC inspector export (OGC-706) — E2E.
 *
 * Covers what the backend slice test can't: the whole path exercised through the
 * authenticated browser session against real seeded rows (the seed-qc-sigma-data
 * pattern), plus the dashboard modal UI. Content correctness (BOM, escaping,
 * PDF text) is asserted in QCExportRestControllerSecurityTest; here we assert the
 * endpoints and UI wire up end-to-end.
 */

const API = "/api/OpenELIS-Global";

async function fetchExport(
  page: Page,
  url: string,
): Promise<{ status: number; contentType: string; body: string }> {
  return page.evaluate(async (u) => {
    const res = await fetch(u, { credentials: "include" });
    return {
      status: res.status,
      contentType: res.headers.get("content-type") || "",
      body: await res.text(),
    };
  }, url);
}

test.describe("QC inspector export (OGC-706)", () => {
  let seed: ExportSeed;

  test.beforeAll(() => {
    seed = seedExportData();
  });

  test.afterAll(() => {
    seed?.restore();
  });

  test("CSV export returns the seeded runs and violation", async ({ page }) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });

    const url =
      `${API}/rest/qc/export/csv?instrumentId=${seed.analyzerId}` +
      `&startDate=${seed.startDate}&endDate=${seed.endDate}`;
    const { status, contentType, body } = await fetchExport(page, url);

    expect(status).toBe(200);
    expect(contentType).toContain("text/csv");
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

  test("PDF export returns a PDF document", async ({ page }) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });

    const url =
      `${API}/rest/qc/export/pdf?instrumentId=${seed.analyzerId}` +
      `&startDate=${seed.startDate}&endDate=${seed.endDate}`;
    const { status, contentType, body } = await fetchExport(page, url);

    expect(status).toBe(200);
    expect(contentType).toContain("application/pdf");
    expect(body.slice(0, 5)).toBe("%PDF-");
  });

  test("reversed date range is rejected with 400", async ({ page }) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });

    const url =
      `${API}/rest/qc/export/csv?instrumentId=${seed.analyzerId}` +
      `&startDate=${seed.endDate}&endDate=${seed.startDate}`;
    const { status } = await fetchExport(page, url);

    expect(status).toBe(400);
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
