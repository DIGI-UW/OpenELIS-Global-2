import { test, expect, Page } from "../../../helpers/test-base";
import { createSampleOrder } from "../../../helpers/seed-tat-data";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";

/**
 * OGC-1221 — Edit Order "Remove Sample" cancelled the sample item but only the
 * analysis on the ticked row; every other test on that sample item stayed live
 * on the Results worklist with no screen left to cancel it from.
 *
 * The walk is the ticket's: order two tests on one Serum item, open Edit Order,
 * tick Remove Sample on the sample row, save, then read the worklist back.
 * "Edit Order lists no current tests" passes on the bug (a cancelled sample
 * item is dropped from that read either way), so the assertion is made against
 * the worklist and the analyses themselves.
 *
 * Run with:
 *   cd frontend && npm run pw:test:core-foundational -- ogc-1221
 */

const API = "/api/OpenELIS-Global";
const SERUM_SAMPLE_TYPE_ID = "2";

interface OrderableTest {
  id: string;
  name: string;
}

interface WorklistRow {
  accessionNumber?: string;
  testName?: string;
  analysisId?: string;
  analysisStatusId?: string;
}

async function twoOrderableSerumTests(page: Page): Promise<OrderableTest[]> {
  const res = await page.request.get(
    `${API}/rest/sample-type-tests?sampleType=${SERUM_SAMPLE_TYPE_ID}`,
  );
  expect(res.status(), "sample-type test list must load").toBe(200);
  const body = (await res.json()) as { tests?: OrderableTest[] };
  const tests = (body.tests ?? []).slice(0, 2);
  expect(tests, "Serum needs at least two orderable tests").toHaveLength(2);
  return tests;
}

async function worklistRows(
  page: Page,
  accessionNumber: string,
): Promise<WorklistRow[]> {
  const res = await page.request.get(
    `${API}/rest/LogbookResults?labNumber=${encodeURIComponent(accessionNumber)}&upperRangeAccessionNumber=&patientPK=&testSectionId=&collectionDate=&recievedDate=&selectedTest=&selectedSampleStatus=&selectedAnalysisStatus=&doRange=false&finished=true`,
  );
  expect(res.status(), "worklist must load").toBe(200);
  const body = (await res.json()) as { testResult?: WorklistRow[] };
  return body.testResult ?? [];
}

test.describe("OGC-1221 Edit Order remove sample", () => {
  test("removing a sample cancels every test on that sample item", async ({
    page,
  }) => {
    test.setTimeout(120_000);

    const [first, second] = await twoOrderableSerumTests(page);
    const now = new Date();
    const pad = (n: number) => String(n).padStart(2, "0");
    const accessionNumber = await createSampleOrder(page, {
      labNo: "",
      receivedDate: `${now.getUTCFullYear()}-${pad(now.getUTCMonth() + 1)}-${pad(now.getUTCDate())}`,
      receivedTime: `${pad(now.getUTCHours())}:${pad(now.getUTCMinutes())}`,
      sampleTypeId: SERUM_SAMPLE_TYPE_ID,
      testIds: `${first.id},${second.id}`,
    });
    expect(accessionNumber, "the two tests must be orderable").toBeTruthy();

    await test.step("Both tests are live on the worklist before the edit", async () => {
      const rows = await worklistRows(page, accessionNumber);
      expect(rows.map((r) => r.analysisId).filter(Boolean)).toHaveLength(2);
    });

    await test.step("Open the order in Edit Order and reach the Sample step", async () => {
      await page.goto("/SampleEdit", { waitUntil: "domcontentloaded" });
      const search = page.getByRole("textbox", {
        name: /enter accession number/i,
      });
      await expect(search).toBeVisible({ timeout: NAV_TIMEOUT });
      await search.fill(accessionNumber);
      await page.getByRole("button", { name: "Submit", exact: true }).click();
      await expect(page).toHaveURL(/\/ModifyOrder\?accessionNumber=/, {
        timeout: LONG_TIMEOUT,
      });
      await page.getByRole("button", { name: "Next", exact: true }).click();
      await expect(
        page.getByRole("table", { name: "Current Tests" }),
      ).toBeVisible({ timeout: UI_TIMEOUT });
    });

    await test.step("Tick Remove Sample on the sample row and save", async () => {
      const currentTests = page.getByRole("table", { name: "Current Tests" });
      await expect(currentTests.getByRole("row")).toHaveCount(3, {
        timeout: UI_TIMEOUT,
      });
      const removeSample = currentTests
        .locator('input[name="removeSample"]')
        .first();
      await expect(
        removeSample,
        "Remove Sample control renders on the sample row",
      ).toBeAttached();
      // Carbon hides the input; its label is the sibling inside the wrapper.
      await removeSample.locator("..").locator("label").click();
      await expect(removeSample).toBeChecked();

      await page.getByRole("button", { name: "Next", exact: true }).click();
      const submit = page.getByRole("button", { name: "Submit", exact: true });
      await expect(submit).toBeVisible({ timeout: UI_TIMEOUT });
      const saved = page.waitForResponse(
        (r) =>
          r.url().includes("/rest/SampleEdit") &&
          r.request().method() === "POST",
        { timeout: LONG_TIMEOUT },
      );
      await submit.click();
      await saved;
    });

    await test.step("No test on the removed sample item is left on the worklist", async () => {
      await expect
        .poll(async () => (await worklistRows(page, accessionNumber)).length, {
          timeout: UI_TIMEOUT,
        })
        .toBe(0);

      await page.goto(
        `/Results?accessionNumber=${encodeURIComponent(accessionNumber)}`,
        { waitUntil: "domcontentloaded" },
      );
      const main = page.getByRole("main");
      await expect(main).toBeVisible({ timeout: NAV_TIMEOUT });
      await expect(
        main.locator("tr", { hasText: accessionNumber }),
      ).toHaveCount(0, { timeout: UI_TIMEOUT });
    });
  });
});
