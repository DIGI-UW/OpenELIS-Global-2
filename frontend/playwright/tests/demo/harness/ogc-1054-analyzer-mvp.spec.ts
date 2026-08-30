import type { Locator, Page, TestInfo } from "@playwright/test";
import { expect, test } from "../../../helpers/test-base";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  TIMEOUT_SCALE,
} from "../../../helpers/timeouts";

const GENEXPERT = "Cepheid GeneXpert (ASTM Mode)";
const FLUOROCYCLER = "FluoroCycler XT";
const KNOWN_ACCESSION = "DEV01261000000000001";
const UNKNOWN_TEST_ACCESSION = "DEV01261000000000002";
const UNKNOWN_VALUE_ACCESSION = "DEV01261000000000003";
const FILE_ACCESSION_ONE = "DEV01263000000000001";
const FILE_ACCESSION_TWO = "DEV01263000000000002";

async function capture(page: Page, testInfo: TestInfo, name: string) {
  const path = testInfo.outputPath(`${name}.png`);
  await page.screenshot({ path, fullPage: false });
  await testInfo.attach(name, { path, contentType: "image/png" });
}

async function openDashboard(page: Page) {
  await page.goto("/analyzers", {
    waitUntil: "domcontentloaded",
    timeout: NAV_TIMEOUT,
  });
  await expect(
    page.getByRole("heading", { level: 1, name: "Analyzers" }),
  ).toBeVisible({ timeout: LONG_TIMEOUT });
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

async function analyzerRow(page: Page, name: string): Promise<Locator> {
  const search = page.getByTestId("analyzer-search-input");
  await search.fill(name);
  const row = page.getByRole("row", {
    name: new RegExp(escapeRegExp(name), "i"),
  });
  await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });
  return row;
}

async function openAnalyzerAction(
  page: Page,
  analyzerName: string,
  actionName: string,
) {
  const row = await analyzerRow(page, analyzerName);
  await row.getByRole("button", { name: "Actions" }).click();
  await page.getByRole("menuitem", { name: actionName }).click();
}

async function selectAllVisibleResults(page: Page) {
  await page.getByText("Save All Results", { exact: true }).click();
  await expect(
    page.getByRole("checkbox", { name: "Save All Results" }),
  ).toBeChecked();
}

test.describe("OGC-1054 assembled analyzer MVP", () => {
  test("reviews safe ASTM and FILE traffic entirely through the visible UI", async ({
    page,
  }, testInfo) => {
    test.setTimeout(300_000 * TIMEOUT_SCALE);

    await openDashboard(page);
    await expect(page.getByTestId("held-results-attention")).toContainText(
      `${GENEXPERT} has held results`,
    );
    const geneRow = await analyzerRow(page, GENEXPERT);
    await expect(geneRow).toContainText("Needs attention");
    await expect(geneRow).not.toContainText("Loading analyzer type", {
      timeout: LONG_TIMEOUT,
    });
    await capture(page, testInfo, "m4-dashboard-attention");

    await page.getByRole("button", { name: "Review held results" }).click();
    await expect(page).toHaveURL(/\/AnalyzerResults\?id=\d+$/);
    await expect(
      page.getByRole("heading", {
        level: 1,
        name: `Analyzer: ${GENEXPERT}`,
      }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    const resultsBreadcrumb = page.getByRole("navigation", {
      name: "Breadcrumb",
    });
    await expect(
      resultsBreadcrumb.getByRole("link", { name: "Analyzers" }),
    ).toHaveAttribute("href", "/analyzers");

    const unknownTestRow = page.getByRole("row", {
      name: new RegExp(UNKNOWN_TEST_ACCESSION),
    });
    await expect(unknownTestRow).toContainText("Held");
    await expect(unknownTestRow).toContainText("UNMAPPED-MTB");
    await expect(
      unknownTestRow.getByRole("link", {
        name: "Review Analyzer Type mapping",
      }),
    ).toHaveCount(0);

    const unknownValueRow = page.getByRole("row", {
      name: new RegExp(UNKNOWN_VALUE_ACCESSION),
    });
    await expect(unknownValueRow).toContainText("Held");
    await expect(unknownValueRow).toContainText("REVIEW REQUIRED");
    await capture(page, testInfo, "m4-held-results");

    await unknownValueRow
      .getByRole("link", { name: "Review Analyzer Type mapping" })
      .click();
    await expect(page).toHaveURL((url) => {
      return (
        url.pathname === "/analyzers/types/genexpert-astm/mapping" &&
        url.searchParams.get("focusTest") === "MTB-RIF" &&
        url.searchParams.get("focusValue") === "REVIEW REQUIRED" &&
        /^\/AnalyzerResults\?id=\d+$/.test(
          url.searchParams.get("returnTo") || "",
        )
      );
    });

    const resultPicker = page.getByRole("combobox", {
      name: "OpenELIS result for REVIEW REQUIRED",
    });
    await expect(resultPicker).toBeFocused({ timeout: LONG_TIMEOUT });
    await expect(page.getByText("Observed in held results")).toBeVisible();
    await resultPicker.click();
    await page
      .getByRole("option", { name: "INDETERMINATE", exact: true })
      .click();
    await page.getByRole("button", { name: "Update shared mappings" }).click();
    await expect(page.getByText("Mappings saved")).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
    await page
      .getByRole("button", {
        name: "Confirm mappings and control recognition",
      })
      .click();
    await expect(
      page.getByText("Mappings and control recognition confirmed"),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await capture(page, testInfo, "m4-catalog-bound-resolution");

    const mappingUrl = page.url();
    await page.reload({ waitUntil: "domcontentloaded", timeout: NAV_TIMEOUT });
    await expect(page).toHaveURL(mappingUrl);
    await expect(
      page.getByRole("combobox", {
        name: "OpenELIS result for REVIEW REQUIRED",
      }),
    ).toHaveAttribute("title", "INDETERMINATE");
    await page
      .getByRole("navigation", { name: "Breadcrumb" })
      .getByRole("link", { name: "Analyzer Types" })
      .click();

    const knownResultRow = page.getByRole("row", {
      name: new RegExp(KNOWN_ACCESSION),
    });
    await expect(knownResultRow).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(knownResultRow).toContainText("NOT DETECTED");
    await selectAllVisibleResults(page);
    await page.getByRole("button", { name: "Save", exact: true }).click();
    await expect(knownResultRow).not.toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(unknownTestRow).toBeVisible();
    await expect(unknownValueRow).toBeVisible();

    await resultsBreadcrumb.getByRole("link", { name: "Analyzers" }).click();
    await openAnalyzerAction(page, GENEXPERT, "Quality Control");
    await expect(page).toHaveURL(/\/analyzers\/qc\/instruments\/\d+/);
    await expect(
      page.getByRole("heading", { level: 1, name: GENEXPERT }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(page.locator("#mainHeader")).toBeInViewport();
    await expect(page.getByText(/HIV Viral Load/).first()).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
    await capture(page, testInfo, "m4-operational-qc");

    await page
      .getByRole("navigation", { name: "Breadcrumb" })
      .getByRole("link", { name: "Analyzers" })
      .click();
    await openAnalyzerAction(page, FLUOROCYCLER, "View results");
    await expect(
      page.getByRole("heading", {
        level: 1,
        name: `Analyzer: ${FLUOROCYCLER}`,
      }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    const firstFileResultRow = page.getByRole("row", {
      name: new RegExp(FILE_ACCESSION_ONE),
    });
    const secondFileResultRow = page.getByRole("row", {
      name: new RegExp(FILE_ACCESSION_TWO),
    });
    await expect(firstFileResultRow).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(secondFileResultRow).toBeVisible();
    await expect(firstFileResultRow.getByRole("textbox").first()).toHaveValue(
      "1250.00",
    );
    await expect(secondFileResultRow.getByRole("textbox").first()).toHaveValue(
      "450.00",
    );
    await capture(page, testInfo, "m4-file-results");

    await selectAllVisibleResults(page);
    await page.getByRole("button", { name: "Save", exact: true }).click();
    await expect(page.getByText(FILE_ACCESSION_ONE)).toHaveCount(0, {
      timeout: LONG_TIMEOUT,
    });
    await expect(page.getByText(FILE_ACCESSION_TWO)).toHaveCount(0);

    await page
      .getByRole("navigation", { name: "Breadcrumb" })
      .getByRole("link", { name: "Analyzers" })
      .click();
    await page.setViewportSize({ width: 390, height: 844 });
    await expect(
      page.getByRole("heading", { level: 1, name: "Analyzers" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    expect(
      await page.evaluate(
        () =>
          document.documentElement.scrollWidth <=
          document.documentElement.clientWidth,
      ),
      "Analyzer dashboard should not overflow the mobile page horizontally",
    ).toBe(true);
    await capture(page, testInfo, "m4-mobile-dashboard");
  });
});
