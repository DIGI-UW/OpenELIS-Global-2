import { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { LONG_TIMEOUT, NAV_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1194: Admin, Import Catalog (CSV).
 *
 * An implementer drops catalog files, sees what they would do without anything
 * being written, applies them, and settles a name the catalog does not know.
 * Every run uses its own names so repeated runs (and a shared CI database) stay
 * independent.
 */

const IMPORT_PAGE = "/MasterListsPage/CatalogImport";
const CATALOG_LIST = "/MasterListsPage/TestCatalogList";

const run = Date.now().toString().slice(-6);
const SECTION = `E2E Unit ${run}`;
const SPECIMEN = `E2E Serum ${run}`;
const TEST_NAME = `E2E Glucose ${run}`;

const file = (name: string, lines: string[]) => ({
  name,
  mimeType: "text/csv",
  buffer: Buffer.from(lines.join("\n") + "\n"),
});

const sectionsCsv = () =>
  file("test-sections-e2e.csv", [
    "testSectionName,isActive,sortOrder,isExternal,localization:en",
    `${SECTION},Y,980,N,${SECTION}`,
  ]);

const sampleTypesCsv = () =>
  file("sample-types-e2e.csv", [
    "description,localAbbreviation,domain,isActive,sortOrder",
    `${SPECIMEN},E${run},H,Y,980`,
  ]);

const testsCsv = () =>
  file("tests-e2e.csv", [
    "testName,testSection,sampleType,loinc,isActive,isOrderable,sortOrder,unitOfMeasure",
    `${TEST_NAME},${SECTION},${SPECIMEN},2345-7,Y,Y,980,`,
  ]);

const componentsCsv = () =>
  file("result-components-e2e.csv", [
    "testName,code,label,resultType,significantDigits,isPrimary,showOnReport",
    `${TEST_NAME},PRIMARY,${TEST_NAME},N,2,Y,Y`,
  ]);

const rangesCsv = (specimen: string) =>
  file("result-limits-e2e.csv", [
    "testName,sampleType,componentCode,gender,minAge,maxAge,lowNormal,highNormal",
    `${TEST_NAME},${specimen},PRIMARY,M,0,120,3.9,5.5`,
  ]);

const dropFiles = async (
  page: Page,
  files: { name: string; mimeType: string; buffer: Buffer }[],
) => {
  await page.locator('input[type="file"]').setInputFiles(files);
  await expect(page.getByTestId("catalog-import-files")).toBeVisible();
};

const preview = async (page: Page) => {
  await page.getByRole("button", { name: "Preview", exact: true }).click();
  await expect(page.getByTestId("catalog-import-plan")).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
};

const apply = async (page: Page) => {
  await page.getByRole("button", { name: "Apply", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "What was loaded" }),
  ).toBeVisible({ timeout: LONG_TIMEOUT });
};

/**
 * Opens the Test Catalog filtered to one name and waits for the list to
 * settle: either it has rows or it says nothing matches. Asserting straight
 * after the navigation reads an empty loading state as an empty result.
 */
const searchCatalog = async (page: Page, name: string) => {
  await page.goto(`${CATALOG_LIST}?search=${encodeURIComponent(name)}`, {
    waitUntil: "domcontentloaded",
  });
  const rows = page.locator('[data-cy^="test-row-"]').first();
  const empty = page.getByText("No tests match the current filters.");
  const failed = page.getByText("Couldn't load the test catalog");
  await expect(rows.or(empty).or(failed).first()).toBeVisible({
    timeout: NAV_TIMEOUT,
  });
  await expect(failed).toHaveCount(0);
};

/**
 * Puts the lab unit, specimen, test and result component in the catalog, so
 * each test stands on its own however the suite is sharded. Re-applying the
 * same files updates the records rather than adding more.
 */
const loadBaseCatalog = async (page: Page) => {
  await page.goto(IMPORT_PAGE, { waitUntil: "domcontentloaded" });
  await dropFiles(page, [
    sectionsCsv(),
    sampleTypesCsv(),
    testsCsv(),
    componentsCsv(),
  ]);
  await preview(page);
  await apply(page);
  const loaded = page
    .getByTestId("catalog-import-plan")
    .getByRole("row", { name: /tests-e2e\.csv/ });
  await expect(loaded.getByRole("cell").nth(3)).toHaveText("0");
};

test.describe("Catalog import (CSV)", () => {
  // Each case previews and applies several files and reads the catalog back;
  // the loads alone run past the 30s default.
  test.beforeEach(() => test.setTimeout(180_000));

  test("previews a set of catalog files, then applies them into the catalog", async ({
    page,
  }) => {
    await page.goto(IMPORT_PAGE, { waitUntil: "domcontentloaded" });
    await expect(
      page.getByRole("heading", { name: "Import Catalog (CSV)" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });

    await dropFiles(page, [
      sectionsCsv(),
      sampleTypesCsv(),
      testsCsv(),
      componentsCsv(),
    ]);
    // The catalog area of each file is read from its name.
    await expect(page.locator("#domain-tests-e2e\\.csv")).toHaveValue("tests");
    await expect(
      page.locator("#domain-result-components-e2e\\.csv"),
    ).toHaveValue("result-components");

    await preview(page);
    const plan = page.getByTestId("catalog-import-plan");
    await expect(plan).toContainText("tests-e2e.csv");
    await expect(
      plan.getByRole("row", { name: /tests-e2e\.csv/ }),
    ).toBeVisible();

    // A preview keeps nothing: the test is not in the catalog yet.
    await searchCatalog(page, TEST_NAME);
    await expect(page.getByRole("cell", { name: TEST_NAME })).toHaveCount(0);

    await loadBaseCatalog(page);

    await searchCatalog(page, TEST_NAME);
    await expect(
      page.getByRole("cell", { name: new RegExp(TEST_NAME) }).first(),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
  });

  test("a specimen the catalog does not know waits for a decision, and the remembered name resolves it", async ({
    page,
  }) => {
    const misspelled = `${SPECIMEN}x`;

    await loadBaseCatalog(page);

    await page.goto(IMPORT_PAGE, { waitUntil: "domcontentloaded" });
    await dropFiles(page, [rangesCsv(misspelled)]);
    await preview(page);

    const queue = page.getByTestId("catalog-import-unresolved");
    await expect(queue).toBeVisible({ timeout: LONG_TIMEOUT });
    const item = queue.getByRole("row", { name: new RegExp(misspelled) });
    await expect(item).toBeVisible();

    await item.getByRole("combobox").selectOption({ label: SPECIMEN });
    // Carbon hides the checkbox input itself; its label is what a user clicks.
    await item.getByText("Remember this name").click();
    await expect(
      item.getByRole("checkbox", { name: "Remember this name" }),
    ).toBeChecked();
    await item.getByRole("button", { name: "Use", exact: true }).click();

    await expect(
      queue.getByRole("row", { name: new RegExp(misspelled) }),
    ).toHaveCount(0, { timeout: LONG_TIMEOUT });

    // With the name remembered the same file loads on its own.
    await page.goto(IMPORT_PAGE, { waitUntil: "domcontentloaded" });
    await dropFiles(page, [rangesCsv(misspelled)]);
    await preview(page);
    await expect(
      page.getByTestId("catalog-import-plan").getByRole("row", {
        name: /result-limits-e2e\.csv/,
      }),
    ).toContainText("1");
  });
});
