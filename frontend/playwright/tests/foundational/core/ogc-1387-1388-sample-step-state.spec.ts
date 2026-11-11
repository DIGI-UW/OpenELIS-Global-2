import { test, expect, Page } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1387 / OGC-1388 — the Add Order sample step carries exactly what it
 * shows. Changing the sample type clears the test search and its results, an
 * unticked test or a test from a replaced sample type is not ordered, Back
 * does not bring an unticked test back, and "Select sample type" sticks.
 */

const API = "/api/OpenELIS-Global";

type CatalogTest = { id: string; name: string };
type SampleTypeWithTests = { id: string; name: string; tests: CatalogTest[] };

/** Two sample types with tests, and a search term that only matches the first. */
async function twoSampleTypes(page: Page) {
  const types = await (
    await page.request.get(`${API}/rest/user-sample-types`)
  ).json();
  const withTests: SampleTypeWithTests[] = [];
  for (const type of types) {
    const catalog = await (
      await page.request.get(
        `${API}/rest/sample-type-tests?sampleType=${type.id}`,
      )
    ).json();
    const tests = (catalog.tests || []).filter((t: CatalogTest) => t.name);
    if (tests.length > 0) {
      withTests.push({ id: String(type.id), name: type.value, tests });
    }
  }
  for (const first of withTests) {
    for (const second of withTests) {
      if (first.id === second.id) continue;
      const secondNames = second.tests.map((t) => t.name.toLowerCase());
      const test = first.tests.find(
        (t) =>
          t.name.length >= 4 &&
          !secondNames.some((n) =>
            n.includes(t.name.slice(0, 4).toLowerCase()),
          ),
      );
      if (test) {
        return { first, second, test, term: test.name.slice(0, 4) };
      }
    }
  }
  throw new Error("No two sample types with distinguishable tests");
}

async function openSampleStep(page: Page) {
  await page.goto("/SamplePatientEntry", { timeout: NAV_TIMEOUT });
  await page.getByRole("button", { name: /^Add Sample/ }).click();
  await expect(page.locator("#sampleId_0")).toBeVisible({
    timeout: UI_TIMEOUT,
  });
}

async function chooseSampleType(page: Page, id: string) {
  const loaded = page.waitForResponse((r) =>
    r.url().includes(`sample-type-tests?sampleType=${id}`),
  );
  await page.locator("#sampleId_0").selectOption(id);
  await loaded;
}

const testCheckbox = (page: Page, test: CatalogTest) =>
  page.locator(`#test_0_${test.id}`);

const testLabel = (page: Page, test: CatalogTest) =>
  page.locator(`label[for="test_0_${test.id}"]`);

const resultReporting = (page: Page) =>
  page.locator(".orderLegendBody", { hasText: /Result Reporting/i });

test.describe("Add Order sample step state (OGC-1387, OGC-1388)", () => {
  test("changing the sample type clears the test search and its results", async ({
    page,
  }) => {
    const { first, second, test, term } = await twoSampleTypes(page);
    await openSampleStep(page);
    await chooseSampleType(page, first.id);
    await page.locator("#tests_search_0").fill(term);
    await expect(
      page.locator(".searchTestsList li", { hasText: test.name }).first(),
    ).toBeVisible();

    await chooseSampleType(page, second.id);

    await expect(page.locator("#tests_search_0")).toHaveValue("");
    await expect(page.locator(".searchTestsList li")).toHaveCount(0);
    await expect(testLabel(page, second.tests[0])).toBeVisible();
  });

  test("an unticked test is not ordered and does not come back after Back", async ({
    page,
  }) => {
    const { first, test } = await twoSampleTypes(page);
    await openSampleStep(page);
    await chooseSampleType(page, first.id);
    await testLabel(page, test).click();
    await expect(testCheckbox(page, test)).toBeChecked();
    await testLabel(page, test).click();
    await expect(testCheckbox(page, test)).not.toBeChecked();

    await page.getByRole("button", { name: "Next", exact: true }).click();
    await expect(resultReporting(page)).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(resultReporting(page)).not.toContainText(test.name);

    await page.getByRole("button", { name: "Back", exact: true }).click();
    await expect(testCheckbox(page, test)).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(testCheckbox(page, test)).not.toBeChecked();
  });

  test("a test from a replaced sample type is not ordered", async ({
    page,
  }) => {
    const { first, second, test } = await twoSampleTypes(page);
    await openSampleStep(page);
    await chooseSampleType(page, first.id);
    await testLabel(page, test).click();
    await expect(testCheckbox(page, test)).toBeChecked();

    await chooseSampleType(page, second.id);
    await page.getByRole("button", { name: "Next", exact: true }).click();

    await expect(resultReporting(page)).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(resultReporting(page)).not.toContainText(test.name);
  });

  test("Select sample type sticks and empties the test list", async ({
    page,
  }) => {
    const { first, test } = await twoSampleTypes(page);
    await openSampleStep(page);
    await chooseSampleType(page, first.id);
    await testLabel(page, test).click();

    await page.locator("#sampleId_0").selectOption("");

    await expect(page.locator("#sampleId_0")).toHaveValue("");
    await expect(page.locator('input[id^="test_0_"]')).toHaveCount(0);
    await page.getByRole("button", { name: "Next", exact: true }).click();
    await expect(resultReporting(page)).not.toContainText(test.name);
  });
});
